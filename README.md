# kudu-kafka-connect
The Kudu Sink Connector allows you to ingest data to [Apache Kudu](https://kudu.apache.org/) tables from [Apache Kafka](http://kafka.apache.org/) topic. **kudu-kafka-connect** does not use JDBC driver.
Cuurently only one configuration is supported: **kudu-kafka-connect** reads definition of Kudu table and map columns of this table to fields in Apache Kafka topic.

## Distribution
1. [GitHub](https://github.com/averemee-si/kudu-kafka-connect)

## Getting Started
These instructions will get you a copy of the project up and running on any platform with JDK8+ support.

### Prerequisites

Before using **kudu-kafka-connect** please check that required Java8+ is installed with

```
echo "Checking Java version"
java -version
```

### Installing

Build with

```
mvn install
```

## Running 

### kudu-kafka-connect Connector's parameters
`a2.kudu.masters` - Kudu master server or a comma separated list of Kudu masters
`a2.case.sensitive.names` - Use case sensitive column names (true, default) or do not (false)
`a2.kudu.table` - Name of Kudu table
`a2.batch.size` - Maximum number of statements to include in a single batch when inserting/updating/deleting data
`a2.schema.type` - Type of schema used by **kudu-kafka-connect**: plain ([JDBC Connector](https://docs.confluent.io/kafka-connect-jdbc/current/index.html) compatible) (default) or Debezium

### Monitoring
_solutions.a2.kafka.kudu.KuduSinkConnector_ publishes a number of metrics about the connector’s activities that can be monitored through JMX. For complete list of metrics please refer to [JMX-METRICS.md](doc/JMX-METRICS.md)

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## TODO

* Table auto-creation from topic information

## Version history

####0.1.0 (APR-2022)

Initial release


## Authors

* **Aleksej Veremeev** - *Initial work* - [A2 Rešitve d.o.o.](https://a2-solutions.eu/)

## License

This project is licensed under the Apache License - see the [LICENSE](LICENSE) file for details

