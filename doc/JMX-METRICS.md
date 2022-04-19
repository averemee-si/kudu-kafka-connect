#### Metrics for solutions.a2.kafka.kudu.KuduSinkConnector

**MBean:solutions.a2.kafka:type=Kudu-Sink-Connector-metrics,name=<Connector-Name>**

|Attribute Name                        |Type     |Description                                                                                       |
|:-------------------------------------|:--------|:-------------------------------------------------------------------------------------------------|
|StartTime                             |String   |Connector start date and time (ISO format)                                                        |
|ElapsedTimeMillis                     |long     |Elapsed time, milliseconds                                                                        |
|ElapsedTime                           |String   |Elapsed time, Days/Hours/Minutes/Seconds                                                          |
|KuduSessionsCount                     |int      |Number of KuduSession's created                                                                   |
|KuduSessionsCreateMillis              |long     |Total time spent for KuduSession's creation, milliseconds                                         |
|KuduSessionsCreateTime                |String   |Total time spent for KuduSession's creation, Days/Hours/Minutes/Seconds                           |
|KuduSessionsCloseMillis               |long     |Total time spent for KuduSession's close, milliseconds                                            |
|KuduSessionsCloseTime                 |String   |Total time spent for KuduSession's close, Days/Hours/Minutes/Seconds                              |
|UpsertRecordsCount                    |long     |Total number of UPSERT operations                                                                 |
|UpsertMillis                          |long     |Total time spent for performing UPSERT operations, milliseconds                                   |
|UpsertTime                            |String   |Total time spent for performing UPSERT operations, Days/Hours/Minutes/Seconds                     |
|UpsertPerSecond                       |int      |Average number of UPSERT operations per second                                                    |
|DeleteRecordsCount                    |long     |Total number of DELETE operations                                                                 |
|DeleteMillis                          |long     |Total time spent for performing DELETE operations, milliseconds                                   |
|DeleteTime                            |String   |Total time spent for performing DELETE operations, Days/Hours/Minutes/Seconds                     |
|DeletePerSecond                       |int      |Average number of DELETE operations per second                                                    |
|KuduFlushesCount                      |int      |Total number of FLUSH operations                                                                  |
|KuduFlushesMillis                     |long     |Total time spent for performing FLUSH operations, milliseconds                                    |
|KuduFlushesTime                       |String   |Total time spent for performing FLUSH operations, Days/Hours/Minutes/Seconds                      |
|KuduFlushesPerSecond                  |int      |Average number of FLUSH operations per second                                                     |
|TotalRecordsCount                     |long     |Total number of records processed by connector                                                    |
|KuduProcessingMillis                  |long     |Total processing time for all Kudu operations, milliseconds                                       |
|KuduProcessingTime                    |String   |Total processing time for all Kudu operations, Days/Hours/Minutes/Seconds                         |
