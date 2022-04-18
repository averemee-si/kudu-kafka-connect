package solutions.a2.kafka.kudu;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.KuduSession;
import org.apache.kudu.client.RowError;
import org.apache.kudu.client.SessionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solutions.a2.utils.ExceptionUtils;
import solutions.a2.utils.Version;


/**
 * Kudu Sink Connector Task
 *  
 * @author <a href="mailto:averemee@a2.solutions">Aleksei Veremeev</a>
 */
public class KuduSinkTask extends SinkTask {

	private static final Logger LOGGER = LoggerFactory.getLogger(KuduSinkTask.class);
	
	private KuduClient kuduClient;
	private int batchSize;
	private int schemaType;
	private String kuduTableName;
	private boolean caseSensitiveNames;
	KuduTableWrapper ktw;

	@Override
	public String version() {
		return Version.getVersion();
	}

	@Override
	public void start(Map<String, String> props) {
		LOGGER.info("Starting Kudu Sink Task for connector '{}'", props.get("name"));
		KuduSinkConnectorConfig config = new KuduSinkConnectorConfig(props);
		batchSize = config.getInt(ParamConstants.BATCH_SIZE_PARAM);
		kuduTableName = config.getString(ParamConstants.KUDU_TABLE_PARAM);
		caseSensitiveNames = config.getBoolean(ParamConstants.CASE_SENSITIVE_NAMES_PARAM);
		final String schemaTypeString = props.get(ParamConstants.SCHEMA_TYPE_PARAM);
		if (ParamConstants.SCHEMA_TYPE_DEBEZIUM.equals(schemaTypeString)) {
			schemaType = ParamConstants.SCHEMA_TYPE_INT_DEBEZIUM;
		} else {
			schemaType = ParamConstants.SCHEMA_TYPE_INT_KAFKA_STD;
		}
		try {
			kuduClient = new KuduClient
				.KuduClientBuilder(config.getString(ParamConstants.KUDU_MASTERS_PARAM))
				.build();
			LOGGER.info("Connected to Kudu masters at {}", config.getString(ParamConstants.KUDU_MASTERS_PARAM));
		} catch (Exception ke) {
			LOGGER.error(ExceptionUtils.getExceptionStackTrace(ke));
			throw new ConnectException(ke);
		}
	}

	@Override
	public void put(Collection<SinkRecord> records) {
		final KuduSession kuduSession = kuduClient.newSession();
		kuduSession.setFlushMode(SessionConfiguration.FlushMode.MANUAL_FLUSH);
		long flushTime = 0;
		long deleteTime = 0;
		long upsertTime = 0;
		int deleteCount = 0;
		int upsertCount = 0;
		int batchCount = 0;
		int processedRecords = 0;
		final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
		for (SinkRecord record : records) {
			LOGGER.trace("Processing record from topic '{}', partition '{}' with offset '{}'",
					record.topic(), record.kafkaPartition(), record.kafkaOffset());
			//TODO
			//TODO - better handling here is required!
			//TODO - different tables in topic!!!
			//TODO
			if (ktw == null) {
				try {
				ktw = KuduTableWrapper.createFromExistingTable(
						kuduClient, kuduTableName, record, caseSensitiveNames);
				} catch (KuduConnectException kce) {
					LOGGER.error(ExceptionUtils.getExceptionStackTrace(kce));
					throw new ConnectException(kce);
				}
			}

			//TODO
			//TODO - better operation detection required here
			//TODO
			String opType = "u";
			if (schemaType == ParamConstants.SCHEMA_TYPE_INT_KAFKA_STD) {
				Iterator<Header> iterator = record.headers().iterator();
				while (iterator.hasNext()) {
					Header header = iterator.next();
					if ("op".equals(header.key())) {
						opType = (String) header.value();
						break;
					}
				}
				LOGGER.debug("Operation type from headers is {}.", opType);
			} else { // if (schemaType == ParamConstants.SCHEMA_TYPE_INT_DEBEZIUM)
				opType = ((Struct) record.value()).getString("op");
				LOGGER.debug("Operation type from payload is {}.", opType);
			}
			try {
				final long nanosStart = System.nanoTime();
				if ("d".equals(opType)) {
					ktw.delete(kuduSession, record);
					deleteTime += System.nanoTime() - nanosStart;
					deleteCount++;
				} else {
					ktw.upsert(kuduSession, record);
					upsertTime += System.nanoTime() - nanosStart;
					upsertCount++;
				}
			} catch (KuduConnectException kce) {
				LOGGER.error(ExceptionUtils.getExceptionStackTrace(kce));
				throw new ConnectException(kce);
			}
			currentOffsets.put(
					new TopicPartition(record.topic(), record.kafkaPartition()),
					new OffsetAndMetadata(record.kafkaOffset()));
			processedRecords++;
			if (processedRecords > batchSize) {
				flushTime += flushChanges(kuduSession, currentOffsets);
				batchCount++;
				processedRecords = 0;
			}
		}
		if (processedRecords > 0) {
			flushTime += flushChanges(kuduSession, currentOffsets);
			batchCount++;
		}
		try {
			kuduSession.close();
		} catch (KuduException ke) {
			LOGGER.error(ExceptionUtils.getExceptionStackTrace(ke));
			throw new ConnectException(ke);
		}
		//TODO
		//TODO sent metrics to JMX
		//TODO
	}

	@Override
	public void stop() {
		if (kuduClient != null) {
			try {
				kuduClient.shutdown();
			} catch (KuduException ke) {
				LOGGER.error(ExceptionUtils.getExceptionStackTrace(ke));
				throw new ConnectException(ke);
			}
		}
	}

	private long flushChanges(
			final KuduSession kuduSession,
			final Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
		try {
			final long nanosStart = System.nanoTime();
			kuduSession.flush();
			flush(currentOffsets);
			currentOffsets.clear();
			if (kuduSession.countPendingErrors() > 0) {
				RowError[] rowErrors = kuduSession.getPendingErrors().getRowErrors();
				for (RowError rowError : rowErrors) {
					LOGGER.error(rowError.getErrorStatus().toString());
				}
				throw new ConnectException("Unable to flush data to Kudu Masters!");
			}
			return (System.nanoTime() - nanosStart);
		} catch (KuduException ke) {
			LOGGER.error(ExceptionUtils.getExceptionStackTrace(ke));
			throw new ConnectException(ke);
		}
	}
}
