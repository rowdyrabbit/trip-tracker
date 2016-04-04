Trip Data Processor and API
===============

Assumptions
-----------
The messages are being sent by vehicles.

The average trip duration is around 20 minutes on average.

The API side of the system (that is servicing the query requests) is an internal tool used by staff, and will have very low load. 

The system will store a year's worth of data, after which archiving will take place.

We don't need to support strong eventual consistency at this stage, but the design of the system should allow us to easily introduce the mechanisms required to implement it.

We need to consider how this system will scale over time.

Analysis
--------

The system must be able to handle 500 concurrent connections, each of which sends a location update every second, therefore a load of 500 requests/second needs to be handled by the channel subscriber.

The total number of messages received per day will be: 500 * 60 * 60 * 24 = 43,200,000 and per year this works out to be: 15,768,000,000.

Working with an average trip duration of 20 minutes: 
- the number of messages sent per trip is: 20 * 60 = 1200 (one message every second)
- the number of trips per day for all vehicles is: 3 * 24 * 500 = 36,000, which is 13,140,000 per year.

Now we have a good understanding of how much data our system needs to store.

I will be encoding the latitude/longitude into a [geohash](https://en.wikipedia.org/wiki/Geohash) value of length 9. Given that the average car length is just under 5 metres, it doesn't make sense to use a more precise geohash than that.


Design
------

### Initial Concept, Design and Scaling Considerations
The system is well suited to a [CQRS](http://martinfowler.com/bliki/CQRS.html) design approach, the message event data being streamed into the pub/sub channel can be written to one large append-only flat table, and we can then read from this store to build up our read tables, of which there may be multiple and which may be stored in different databases, depending on what makes sense for the type of read queries we want to optimize for. This design isolates the read side from the write side, allowing them to be scaled independently of each other.

A CQRS approach is particularly well suited to this application because of the idempotent nature of the messages, and we can build it to have strong eventual consistency by ensuring at-least-once-delivery, i.e. if a message is delivered more than once, it doesn't change the state of the stored data and the result of processing the same message again is effectively a no-op, this means we don't have to worry about things like distributed transactions which don't scale. At-least-once-delivery can be achieved in several ways, using Kafka as a persistent message queue is one way, another is writing all messages to a datastore and using a sequence number to track which messages have been successfully processed by the read side.

This project does not support strong eventual consistency, but when required, I would store the events in an intermediate table using Akka persistence and have Akka persistence queries update the read side tables with at-least-once-delivery of messages. This application does not implement the write side of CQRS, and avoids this intermediate event storage, instead, it updates all query tables immediately upon receipt of a message because I have assumed that the read query API will have very low load (see assumptions section above), and we don't need to be concerned about the impact of the read side on the performance of the write side at this stage. If we need to scale the system because the read load increases (maybe the API gets exposed to the general public) then we can introduce the write side intermediate table, so that the read queries don't impact the performance of the writes. This will allow us to scale the read and write sides independently and they won't impact each other's performance.


### Data Store Decisions

#### Pub/Sub Channel
I have chosen Redis Pub/Sub for my pub/sub channel, it's great for speed of setup and executing quickly, but has no guarantees around message delivery, messages aren't persisted, and if there are no channel subscribers, then messages will be dropped. If we need these guarantees then we can replace it with Kafka or another persistent message queue implementation.

I have decided to use two different databases to persist the data for querying, Cassandra and PostgreSQL, the rationale is below, but first we need to talk about geohashes.

#### Geohashes

Two out of the three queries we need to implement involve querying by a georect. This is defined as a rectangular area bordered by two arbitrary lat/long points, which defines a rectangular area on a map - e.g NW and SE. All location data is stored in our database as a geohash of length 9, so in order to query by a georect, we need to map a georect to the geohashes within it. I have implemented a mapping function which does the following:

1. Calculate the centre point of the supplied georect.
2. Given the width and height of the georect, determine the largest geohash precision which fully encompasses the georect and get its geohash
3. From this large fully encompassing geohash, find all the contained 'sub-geohashes' (geohashes which are higher in precision but smaller in area)
4. Iterate through each of these sub-geohashes and if there is no overlap with the georect supplied by the query, then discard it, otherwise retain it
5. Return the list of all geohashes which overlap with the original georect query area


#### Datastores

In order to scale the system, I am taking advantage of the idempotent nature of the messages being sent to the system. Idempotent updates means we can support at-least-once delivery semantics, without worrying about distributed transactions which don't scale across multiple nodes. I have also designed the tables so that the queries we need to perform don't require joins, because in order to support eventual consistency in the future, if joins were required then we would need to have transactional updates of these join tables which again will be problematic when scaling.

##### Query 1
The first query *'How many trips passed through a georect defined by two lat/lng points?'* is the most difficult and requires storing the most data, i.e. in the order of 43 million records a day. A distributed database makes sense here because it will allow us to easily scale and shard the data across multiple nodes, I have chosen to use Cassandra because of the ease of setup, and its ability to scale out. One disadvantage is the restriction on the types of queries we can execute, for example there are no like clauses in CQL, it cannot be treated the same way as a relational data store.

The query requires that given an arbitrary rectangle defined by two lat/long points (a georect), we return the number of (unique) trips which passed through it. If the georect is very large and contains a highly-trafficked area then there could have been thousands of trips that passed through it, and each trip might have thousands of 9 length geohashes. In order to support idempotency, I have chosen to design the Cassandra table with a primary key index on `(geohash, trip_id)`. This means that if, for example, a vehicle has remained stationary for a period of time but has still been sending messages to our system, there won't be duplicate entries for those times, thus resulting in fewer persisted location updates. 

When the system writes to this table, it calculates the prefix array of the geohash and persists an entry for every geohash prefix with the `trip_id`. This allows us to query the table by geohashes of any length and is necessary because CQL doesn't support like clauses. In order to execute the query, I do the following:
1. Calculate all geohashes for the supplied georect
2. Query the Cassandra table for all geohashes (executed in parallel)
3. Combine the results and return the number of distinct `trip_id` values

If we add multiple Cassandra nodes then the data will be partitioned by `geohash`, which may become problematic if there is significantly more traffic in some geohashes than others. I.e. we will end up with an uneven distribution of data, this will be something that needs to be monitored as the system grows. 

Another way of scaling this table is to introduce a table per geohash precision, so if we query a length 4 geohash, we ask the length 4 precision table. This means each event is going to correspond to up to 9 index writes - each lat/long coordinate is mapped to a length 9 geohash, which is stored in the length 9 table, then truncate the length 9 geohash to length 8, and store it in the length 8 table, then to length 7, and so on.  As the geohash length decreases, the chances of actually doing a write decrease, since the chance of the geohash being the same as the last event for the trip - i.e., this geohash already exists for this trip in the database, increases, beacuse the geohash area increases.  And so the amount of data in each table decreases and the precision decreases - the 8 lower precisions will probably have about the same amount of data combined as what the length 9 precision table contains.


##### Query 2
For the second query *'How many trips started or stopped within a georect, and the sum total of their fares?'* there is a lot less data that we need to store, around 13 million records, which can be stored in a single PostgreSQL table with the following structure: 
`(trip_id, start_geohash, end_geohash, fare)`, then when we receive a `begin` message, we insert the `trip_id` and `start_geohash` into the table, and when we receive an `end` message we update the `end_geohash` and the `fare` columns. Note that these insert and update queries are also idempotent, and I am taking advantage of the new Postgres 9.5 upsert feature to ignore conflicts if multiple inserts are attempted with the same `trip_id` and `start_geohash` primary key values. Adding a composite index on the `start_geohash` and `end_geohash` columns improves query performance and we can implement the query with a simple select statement.

##### Query 3
The third query is even simpler, and stores the the same number of records as the above query, again using a single table in PostgreSQL. The table has the structure `(trip_id, start_time, end_time)`, again allowing us to have idempotency and support at-least-once message delivery. When a `begin` message is received, we insert the `trip_id` and `start_time` into the table and when we receive an `end` message, the `end_time` is updated. Again, I am using Postgres 2.5 upserts to ignore conflicts in the case of multiple duplicate messages. The query itself is a simple select, and I am using a composite index of `(start_time, end_time)` to improve query performance.

##### Scaling Considerations
Every read/write is to essentially key/value stores that can be sharded, which means individual reads/writes can all scale horizontally. Scaling Cassandra would involve increasing the number of nodes, scaling the PostgreSQL instances would require a sharding strategy, for the first query, sharding based on geohash makes sense as our query is geohashed-based. The third query table could be sharded based on timestamps as this is what the query searches on, and storing trips with the same `start_timestamp` together will result in better performance. We may need to introduce new generated IDs here to improve distribution of data across the shards.  


##### Fault Tolerance
The replication factor of the Cassandra database can be increased, ensuring data is replicated and available on multiple nodes in the case of a node failure.
For the PostgreSQL database, there are various options, for example a primary/secondary setup, supporting failover to the secondary in the case of a primary node failure.  


### Application Components
I have designed the Subscriber and API applications so that they can be deployed independently, i.e. they are standalone applications, which means they can be scaled independently of each other, they are also stateless, which means multiple instances of the same application can be deployed to different nodes.

#### Subscriber Application
The subscriber is a standalone application which connects to a Redis Pub/Sub channel and receives all trip messages sent there. Upon receiving a message, it spawns a thread from a threadpool, which is responsible for validating and parsing the message and then persisting it to the 3 query tables, one of which is a Cassandra table. The application has been load tested with 500 concurrent connections, each sending one message per second, and easily handles this load. To scale the system, we could have multiple deployments of this application, listening to different pub/sub channels and processing different messages, i.e. we might have one pub/sub channel per region or city, and we could have one instance of this application per channel, processing messages from that geographical area. To support failover, we could have a load balancer in front of multiple deployed instances of this application, distributing messages on a round-robin basis to each instance. In the case of failure, the load balancer would simply stop sending messages to the failed instance.

#### API Application
I have exposed the queries via a simple REST API. It parses the request parameters, performs input validation and executes the database queries against the different data stores and returns the results to the user. In order to measure query performance I am using the DropWizard Metrics library which times each database query and logs it via JMX which you can view the output of by running `jconsole` at the command line (requires JDK to be installed). It collects information around performance percentiles and is really useful for monitoring performance critical pieces of code. This API application can be easily scaled and handle individual instance failures by deploying it to multiple nodes with a load balancer in front, distributing requests to each instance.

#### Test Harness
I wrote a simple test harness which connects to the Redis Pub/Sub channel and publishes 500 messages per second. This was useful when testing my applications. It can be found in the `test-harness module`.


Building and Running from Source
--------------------------------

### Requirements
* Java JDK Version 8 [download here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* Apache Maven Version 3+ [download here] (https://maven.apache.org/download.cgi)
* Apache Cassandra 3.4
* PostgreSQL 9.5 and above - I am using the new upsert SQL syntax.
* Redis 2.6.9

### Database setup
You will need to create the PostgreSQL and Cassandra tables, I have created a simple script which you can find in the scripts directory called `initialise_database.sh`

### Building the Executables
To build the executable jars:

1. Change to the top level directory 
2. Run `mvn package` - this will run all the tests and produce two executable jars: 
   `trip_subscriber/target/trip-subscriber.jar` and `trip_api/target/trip-api.jar`
3. To run them: `java -jar <path_to_jarname>`

The API application starts on port `4567` by default, and the following are the API request formats, which you can also see by going to `http://localhost:4567/`

    GET http://localhost:[port]/api/trips/geocount?nw=[lat,long]&se=[lat,long]

    GET http://localhost:[port]/api/trips/geovalue?nw=[lat,long]&se=[lat,long]
    
    GET http://localhost:[port]/api/trips/timecount?from=[from_epoch]&to=[to_epoch]



