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

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.client.Delete;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.KuduSession;
import org.apache.kudu.client.KuduTable;
import org.apache.kudu.client.PartialRow;
import org.apache.kudu.client.Upsert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KuduTableWrapper
 * Data model for Kudu table 
 * @author <a href="mailto:averemee@a2.solutions">Aleksei Veremeev</a>
 */
public class KuduTableWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(KuduTableWrapper.class);

	final int schemaType;
	final List<KuduTable> kuduTables;
	final List<Map<String, MappingHolder>> kuduToKafka;

	private KuduTableWrapper() {
		//TODO
		schemaType = ParamConstants.SCHEMA_TYPE_INT_KAFKA_STD;
		kuduTables = new ArrayList<>();
		kuduToKafka = new ArrayList<>();
	}

	private boolean exist(final KuduClient client, final String tableName) throws KuduConnectException {
		try {
			return client.tableExists(tableName);
		} catch (KuduException ke) {
			throw new KuduConnectException(ke);
		}
	}

	/**
	 * 
	 * Creates wrapper from existing Kudu table
	 * 
	 * @param client KuduClient
	 * @param tableName existing table name in format <DATABASE>.<TABLE>
	 * @param sinkRecord Kafka Connect Sink record to match data
	 * @param caseSensitive use case sensitive field names?
	 * @return KuduTableWrapper constructed from existing Kudu table 
	 * @throws
	 */
	public static KuduTableWrapper createFromExistingTable(
			final KuduClient client,
            final String tableName,
            final SinkRecord record,
            final boolean caseSensitive) throws KuduConnectException {
		final KuduTableWrapper wrapper = new KuduTableWrapper();
		if (wrapper.exist(client, tableName)) {
			try {
				wrapper.kuduTables.add(client.openTable(tableName));
				wrapper.kuduToKafka.add(new HashMap<>());
				//Map Kudu table columns to fields in topic
				final Map<String, String> topicKeys = new HashMap<>();
				if (record.keySchema() != null &&
						record.keySchema().fields() != null &&
						record.keySchema().fields().size() > 0) {
					for (Field keyField : record.keySchema().fields()) {
						final String fieldName = keyField.name(); 
						if (caseSensitive) {
							topicKeys.put(fieldName, fieldName);
						} else {
							topicKeys.put(fieldName.toLowerCase(Locale.ROOT), fieldName);
						}
					}
				}
				final Map<String, String> topicValues = new HashMap<>();
				if (record.valueSchema() != null &&
						record.valueSchema().fields() != null &&
						record.valueSchema().fields().size() > 0) {
					for (Field valueField : record.valueSchema().fields()) {
						final String fieldName = valueField.name(); 
						if (caseSensitive && !topicKeys.containsKey(fieldName)) {
							topicValues.put(fieldName, fieldName);
						} else {
							if (!topicKeys.containsKey(fieldName.toLowerCase(Locale.ROOT))) {
								topicValues.put(fieldName.toLowerCase(Locale.ROOT), fieldName);
							}
						}
					}
				}
				
				for (ColumnSchema column : wrapper.kuduTables.get(0).getSchema().getColumns()) {
					boolean missed = false;
					final MappingHolder holder = new MappingHolder();
					if (topicKeys.containsKey(column.getName())) {
						holder.keyOrValue = true;
						holder.kafkaName = topicKeys.get(column.getName());
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Kudu table '{}' column '{}' is mapped to Kafka keySchema field '{}'",
								wrapper.kuduTables.get(0).getName(), column.getName(), holder.kafkaName);
						}
					} else if (topicValues.containsKey(column.getName())) {
						holder.keyOrValue = false;
						holder.kafkaName = topicValues.get(column.getName());
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Kudu table '{}' column '{}' is mapped to Kafka valueSchema field '{}'",
								wrapper.kuduTables.get(0).getName(), column.getName(), holder.kafkaName);
						}
					} else {
						missed = true;
					}
					if (missed) {
						if (column.isKey()) {
							LOGGER.error("Kudu table '{}': key column '{}' is missed in Kafka Topic '{}' at offset {}!",
									tableName, column.getName(), record.topic(), record.kafkaOffset());
							throw new KuduConnectException(
									"Column " + column.getName() + " is missed in Kafka topic at offset " + record.kafkaOffset() + "!");
						} else if (!column.isNullable() && column.getDefaultValue() == null) {
							LOGGER.error("Kudu table '{}': non-null column without default value '{}' is missed in Kafka Topic '{}' at offset {}!",
									tableName, column.getName(), record.topic(), record.kafkaOffset());
							final Struct keyStruct = (Struct) record.key();
							LOGGER.error("Values of Key fields for this message are:");
							for (Field keyField : keyStruct.schema().fields()) {
								final String fieldName = keyField.name();
								LOGGER.error("\t\"{}\"\t=\t'{}'", fieldName, keyStruct.get(fieldName));
							}
							if (record.valueSchema() == null ||
									record.value() == null ||
									record.valueSchema().fields() == null) {
								//Delete event
								//Just try this:
								holder.keyOrValue = false;
								//TODO - better detection is required...
								holder.kafkaName = column.getName();
							} else {
								throw new KuduConnectException(
										"Column " + column.getName() + " is missed in Kafka topic at offset " + record.kafkaOffset() + "!");
							}
						} else if (column.getDefaultValue() != null) {
							LOGGER.debug("Default value will be used for column '{}' in table '{}'.",
									column.getName(), tableName);
						} else {
							LOGGER.debug("Kudu table '{}': NULLable column '{}' definition is missed in Kafka Topic '{}'!",
									tableName, column.getName(), record.topic());
						}
					} else {
						wrapper.kuduToKafka.get(0).put(column.getName(), holder);
						LOGGER.debug("Column '{}' added to processing list for table '{}'.",
								column.getName(), tableName);
					}
				}
				return wrapper;
			} catch (KuduException ke) {
				LOGGER.error("Error processing table {} in cluster {}!",
						tableName, client.getMasterAddressesAsString());
				throw new KuduConnectException("Error opening table " + tableName + "!");				
			}
		} else {
			LOGGER.error("Table {} not exist in cluster {}!",
					tableName, client.getMasterAddressesAsString());
			throw new KuduConnectException("Table not exist " + tableName + "!");
		}
	}

	/**
	 * 
	 * Upsert data from KafkaConnect SinkRecord to Kudu table(s) 
	 * 
	 * @param session
	 * @param record
	 * @throws KuduConnectException
	 */
	public void upsert(final KuduSession session, final SinkRecord record) throws KuduConnectException {
		final Struct keyStruct;
		final Struct valueStruct;
		if (schemaType == ParamConstants.SCHEMA_TYPE_INT_KAFKA_STD) {
			keyStruct = (Struct) record.key();
			valueStruct = (Struct) record.value();
		} else { // if (schemaType == ParamConstants.SCHEMA_TYPE_INT_DEBEZIUM)
			keyStruct = ((Struct) record.value()).getStruct("before");
			valueStruct = ((Struct) record.value()).getStruct("after");
		}
		for (int i = 0; i < kuduTables.size(); i++) {
			final KuduTable kuduTable = kuduTables.get(i);
			final Map<String, MappingHolder> columnMapping = kuduToKafka.get(i);
			final Upsert upsert = kuduTable.newUpsert();
			PartialRow row = upsert.getRow();
			for (ColumnSchema cs : kuduTable.getSchema().getColumns()) {
				final String columnName = cs.getName();
				if (columnMapping.containsKey(columnName)) {
					Object columnValue;
					if (columnMapping.get(columnName).keyOrValue) {
						columnValue = keyStruct.get(columnMapping.get(columnName).kafkaName);
					} else {
						try {
							columnValue = valueStruct.get(columnMapping.get(columnName).kafkaName);
						} catch (NullPointerException npe) {
							LOGGER.error("NullPointerException while getting mapping for Kudu column '{}'!", columnName);
							LOGGER.error("Values of Key fields for this message are:");
							for (Field keyField : keyStruct.schema().fields()) {
								final String fieldName = keyField.name();
								LOGGER.error("\t\"{}\"\t=\t'{}'", fieldName, keyStruct.get(fieldName));
							}
							LOGGER.error("Kudu column '{}' is mapped to Kafka value STRUCT field '{}'",
									columnName, columnMapping.get(columnName).kafkaName);
							throw new ConnectException(npe);
						}
					}
					if (columnValue == null) {
						row.setNull(columnName);
						LOGGER.trace("Table '{}', column '{}': value set to null.", kuduTable.getName(), columnName);
					} else {
						addRowValues(kuduTable.getName(), row, columnName, cs.getType().getName(), columnValue);
					}
				} else {
					LOGGER.trace("Table '{}', column '{}': is skipped.", kuduTable.getName(), columnName);
				}
			}
			try {
				session.apply(upsert);
			} catch (KuduException ke) {
				LOGGER.error("Unable to perform UPSERT for {}", kuduTable.getName());
				throw new KuduConnectException(ke);
			}
		}
	}

	/**
	 * 
	 * Delete row from Kudu Table 
	 * 
	 * @param session
	 * @param record
	 * @throws KuduConnectException
	 */
	public void delete(final KuduSession session, final SinkRecord record) throws KuduConnectException {
		final Struct keyStruct;
		final Struct valueStruct;
		if (schemaType == ParamConstants.SCHEMA_TYPE_INT_KAFKA_STD) {
			keyStruct = (Struct) record.key();
			valueStruct = (Struct) record.value();
		} else { // if (schemaType == ParamConstants.SCHEMA_TYPE_INT_DEBEZIUM)
			keyStruct = ((Struct) record.value()).getStruct("before");
			valueStruct = ((Struct) record.value()).getStruct("after");
		}
		for (int i = 0; i < kuduTables.size(); i++) {
			final KuduTable kuduTable = kuduTables.get(i);
			final Map<String, MappingHolder> columnMapping = kuduToKafka.get(i);
			final Delete delete = kuduTable.newDelete();
			PartialRow row = delete.getRow();
			for (ColumnSchema cs : kuduTable.getSchema().getPrimaryKeyColumns()) {
				final String columnName = cs.getName();
				final Object columnValue;
				if (columnMapping.get(columnName).keyOrValue) {
					columnValue = keyStruct.get(columnMapping.get(columnName).kafkaName);
				} else {
					columnValue = valueStruct.get(columnMapping.get(columnName).kafkaName);
				}
				if (columnValue == null) {
					LOGGER.error("Table '{}', primary key column '{}' is NULL.",
							kuduTable.getName(), columnName);
					throw new KuduConnectException("Primary key column is NULL");
				} else {
					addRowValues(kuduTable.getName(), row, columnName, cs.getType().getName(), columnValue);
				}
			}
			try {
				session.apply(delete);
			} catch (KuduException ke) {
				LOGGER.error("Unable to perform DELETE for {}", kuduTable.getName());
				throw new KuduConnectException(ke);
			}
		}
	}

	private void addRowValues(final String tableName, final PartialRow row,
			final String columnName, final String columnTypeName, final Object columnValue) {
		
		switch (columnTypeName.toLowerCase()) {
		case "bool":
			row.addBoolean(columnName, (boolean) columnValue);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						(boolean) columnValue);
			}
			break;
		case "int8":
			row.addByte(columnName, (byte) columnValue);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						(byte) columnValue);
			}
			break;
		case "int16":
			row.addShort(columnName, (short) columnValue);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						(short) columnValue);
			}
			break;
		case "int32":
			row.addInt(columnName, (int) columnValue);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						(int) columnValue);
			}
			break;
		case "int64":
			try {
				row.addLong(columnName, (long) columnValue);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
							tableName, columnName, columnTypeName,
							(long) columnValue);
				}
			} catch (ClassCastException cce) {
				row.addInt(columnName, (int) columnValue);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
							tableName, columnName, columnTypeName,
							(int) columnValue);
				}
			}
			break;
		case "float":
			row.addFloat(columnName, (float) columnValue);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						(float) columnValue);
			}
			break;
		case "double":
			row.addDouble(columnName, (double) columnValue);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						(double) columnValue);
			}
			break;
		case "decimal":
			row.addDecimal(columnName, (BigDecimal) columnValue);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						row.getDecimal(columnName));
			}
			break;
		case "date":
			row.addDate(columnName, new java.sql.Date(((java.util.Date) columnValue).getTime()));
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						row.getDate(columnName));
			}
			break;
		case "unixtime_micros":
			row.addTimestamp(columnName, new java.sql.Timestamp(((java.util.Date) columnValue).getTime()));
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						row.getTimestamp(columnName));
			}
			break;
		case "binary":
			row.addBinary(columnName, (ByteBuffer) columnValue);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						row.getBinary(columnName));
			}
			break;
		case "varchar":
			row.addVarchar(columnName, (String) columnValue);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						(String) columnValue);
			}
			break;
		case "string":
			row.addString(columnName, (String) columnValue);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Table '{}', column '{}': value set to '{}' value of '{}'.",
						tableName, columnName, columnTypeName,
						(String) columnValue);
			}
			break;
		}
	}

	private static class MappingHolder {
		boolean keyOrValue;
		String kafkaName;
	}

}
