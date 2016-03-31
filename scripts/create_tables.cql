DROP KEYSPACE UBER;

CREATE KEYSPACE UBER WITH REPLICATION = {
    'class': 'SimpleStrategy',
    'replication_factor': '1'
};

USE UBER;

CREATE TABLE geo_trips (
geohash text,
trip_id text,
PRIMARY KEY (geohash, trip_id));
