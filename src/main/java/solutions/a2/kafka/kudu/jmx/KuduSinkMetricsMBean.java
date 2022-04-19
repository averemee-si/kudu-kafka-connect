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

package solutions.a2.kafka.kudu.jmx;

/**
 * Kadu Sink Connector Metrics MBean
 *  
 * @author <a href="mailto:averemee@a2.solutions">Aleksei Veremeev</a>
 */
public interface KuduSinkMetricsMBean {
	public String getStartTime();
	public long getElapsedTimeMillis();
	public String getElapsedTime();
	public int getKuduSessionsCount();
	public long getKuduSessionsCreateMillis();
	public String getKuduSessionsCreateTime();
	public long getKuduSessionsCloseMillis();
	public String getKuduSessionsCloseTime();
	public long getTotalRecordsCount();
	public long getUpsertRecordsCount();
	public long getUpsertMillis();
	public String getUpsertTime();
	public int getUpsertPerSecond();
	public long getDeleteRecordsCount();
	public long getDeleteMillis();
	public String getDeleteTime();
	public int getDeletePerSecond();
	public int getKuduFlushesCount();
	public long getKuduFlushesMillis();
	public String getKuduFlushesTime();
	public int getKuduFlushesPerSecond();
	public long getKuduProcessingMillis();
	public String getKuduProcessingTime();

}
