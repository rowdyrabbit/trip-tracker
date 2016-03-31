#!/bin/bash

# Set up PostgreSQL database and table
createdb uber
psql uber -f create_tables.sql



# Set up Cassandra keyspace and table
cqlsh -k uber -f create_tables.cql

