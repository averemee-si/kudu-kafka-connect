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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solutions.a2.utils.Version;

/**
 * Kudu Sink Connector
 *  
 * @author <a href="mailto:averemee@a2.solutions">Aleksei Veremeev</a>
 */
public class KuduSinkConnector extends SinkConnector {

	private static final Logger LOGGER = LoggerFactory.getLogger(KuduSinkConnector.class);
	// Generated using 	https://patorjk.com/software/taag/#p=display&f=ANSI%20Shadow&t=A2%20KuduSinkhttps://patorjk.com/software/taag/#p=display&f=ANSI%20Shadow&t=A2%20Solutions%0A%20%20%20%20%20%20%20KuduSink
	private static final String LOGO = 
			"\n" +
			" █████╗ ██████╗     ███████╗ ██████╗ ██╗     ██╗   ██╗████████╗██╗ ██████╗ ███╗   ██╗███████╗\n" +
			"██╔══██╗╚════██╗    ██╔════╝██╔═══██╗██║     ██║   ██║╚══██╔══╝██║██╔═══██╗████╗  ██║██╔════╝\n" +
			"███████║ █████╔╝    ███████╗██║   ██║██║     ██║   ██║   ██║   ██║██║   ██║██╔██╗ ██║███████╗\n" +
			"██╔══██║██╔═══╝     ╚════██║██║   ██║██║     ██║   ██║   ██║   ██║██║   ██║██║╚██╗██║╚════██║\n" +
			"██║  ██║███████╗    ███████║╚██████╔╝███████╗╚██████╔╝   ██║   ██║╚██████╔╝██║ ╚████║███████║\n" +
			"╚═╝  ╚═╝╚══════╝    ╚══════╝ ╚═════╝ ╚══════╝ ╚═════╝    ╚═╝   ╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚══════╝\n" +
			"\n" +
			"                            ██╗  ██╗██╗   ██╗██████╗ ██╗   ██╗███████╗██╗███╗   ██╗██╗  ██╗  \n" +
			"                            ██║ ██╔╝██║   ██║██╔══██╗██║   ██║██╔════╝██║████╗  ██║██║ ██╔╝  \n" +
			"                            █████╔╝ ██║   ██║██║  ██║██║   ██║███████╗██║██╔██╗ ██║█████╔╝   \n" +
			"                            ██╔═██╗ ██║   ██║██║  ██║██║   ██║╚════██║██║██║╚██╗██║██╔═██╗   \n" +
			"                            ██║  ██╗╚██████╔╝██████╔╝╚██████╔╝███████║██║██║ ╚████║██║  ██╗  \n" +
			"                            ╚═╝  ╚═╝ ╚═════╝ ╚═════╝  ╚═════╝ ╚══════╝╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝  \n" +
			"\t by A2 Rešitve d.o.o.";

	private KuduSinkConnectorConfig config;

	@Override
	public String version() {
		return Version.getVersion();
	}

	@Override
	public void start(Map<String, String> props) {
		LOGGER.info(LOGO);
		config = new KuduSinkConnectorConfig(props);
	}

	@Override
	public void stop() {
	}

	@Override
	public Class<? extends Task> taskClass() {
		return KuduSinkTask.class;
	}

	@Override
	public List<Map<String, String>> taskConfigs(int maxTasks) {
		// Only one task currently!
		final List<Map<String, String>> configs = new ArrayList<>();
		final Map<String, String> props = new HashMap<>();
		config.values().forEach((k, v) -> {
			if (v instanceof Boolean) {
				props.put(k, ((Boolean) v).toString());
			} else if (v instanceof Short) {
				props.put(k, ((Short) v).toString());
			} else if (v instanceof Integer) {
				props.put(k, ((Integer) v).toString());
			} else if (v instanceof Long) {
				props.put(k, ((Long) v).toString());
			} else {
				//TODO - need to handle more types
				props.put(k, (String) v);
			}
		});
		configs.add(props);
		return configs;
	}

	@Override
	public ConfigDef config() {
		return KuduSinkConnectorConfig.config();
	}

}
