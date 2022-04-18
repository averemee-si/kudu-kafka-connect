/**
 * Copyright (c) 2018-present, A2 Rešitve d.o.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package solutions.a2.kafka.kudu;

/**
 * Constants Definition for Kudu Sink Connector
 *  
 * @author <a href="mailto:averemee@a2.solutions">Aleksei Veremeev</a>
 */
public class ParamConstants {

	public static final int SCHEMA_TYPE_INT_DEBEZIUM = 1;
	public static final int SCHEMA_TYPE_INT_KAFKA_STD = 2;

	public static final String KUDU_MASTERS_PARAM = "a2.kudu.masters";
	public static final String KUDU_MASTERS_DOC = "Kudu master server or a comma separated list of Kudu masters";

	public static final String CASE_SENSITIVE_NAMES_PARAM = "a2.case.sensitive.names";
	public static final String CASE_SENSITIVE_NAMES_DOC = "Use case sensitive column names (true, default) or do not (false)";

	public static final String KUDU_TABLE_PARAM = "a2.kudu.table";
	public static final String KUDU_TABLE_DOC = "Name of Kudu table";

	public static final String BATCH_SIZE_PARAM = "a2.batch.size";
	public static final String BATCH_SIZE_DOC = "Maximum number of statements to include in a single batch when inserting/updating/deleting data";
	public static final int BATCH_SIZE_DEFAULT = 1000;

	public static final String SCHEMA_TYPE_PARAM = "a2.schema.type";
	public static final String SCHEMA_TYPE_DOC = "Type of schema used by oracdc: Kafka Connect JDBC compatible (default) or Debezium";
	public static final String SCHEMA_TYPE_KAFKA = "kafka";
	public static final String SCHEMA_TYPE_DEBEZIUM = "debezium";


}
