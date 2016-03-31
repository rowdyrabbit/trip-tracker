drop schema public cascade;
create schema public;



CREATE TABLE time_trips (trip_id varchar not null,
                         start_time bigint,
                         end_time bigint,
                         primary key(trip_id));

CREATE TABLE orgn_dst_geo_trips (trip_id text not null,
                                 geohash_start text not null,
                                 geohash_end text,
                                 fare numeric,
                                 primary key (trip_id));


CREATE INDEX orgn_dst_geo_trips_idx on orgn_dst_geo_trips (geohash_start, geohash_end);
CREATE INDEX time_trips_idx on time_trips (start_time, end_time);

