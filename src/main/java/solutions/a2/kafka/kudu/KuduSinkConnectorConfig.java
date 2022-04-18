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

import java.util.Map;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

/**
 * Kudu Sink Connector Configuration Definition
 * 
 * @author <a href="mailto:averemee@a2.solutions">Aleksei Veremeev</a>
 */
public class KuduSinkConnectorConfig extends AbstractConfig {

	public static ConfigDef config() {
		return new ConfigDef()
				.define(ParamConstants.KUDU_MASTERS_PARAM, Type.STRING,
						Importance.HIGH, ParamConstants.KUDU_MASTERS_DOC)
				.define(ParamConstants.CASE_SENSITIVE_NAMES_PARAM, Type.BOOLEAN, true,
						Importance.HIGH, ParamConstants.CASE_SENSITIVE_NAMES_DOC)
				.define(ParamConstants.KUDU_TABLE_PARAM, Type.STRING,
						Importance.HIGH, ParamConstants.KUDU_TABLE_DOC)
				.define(ParamConstants.BATCH_SIZE_PARAM, Type.INT,
						ParamConstants.BATCH_SIZE_DEFAULT,
						Importance.HIGH, ParamConstants.BATCH_SIZE_DOC)
				.define(ParamConstants.SCHEMA_TYPE_PARAM, Type.STRING,
						ParamConstants.SCHEMA_TYPE_KAFKA,
						ConfigDef.ValidString.in(ParamConstants.SCHEMA_TYPE_KAFKA, ParamConstants.SCHEMA_TYPE_DEBEZIUM),
						Importance.HIGH, ParamConstants.SCHEMA_TYPE_DOC);
	}

	public KuduSinkConnectorConfig(Map<?, ?> originals) {
		super(config(), originals);
	}

}
