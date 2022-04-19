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

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solutions.a2.utils.ExceptionUtils;

/**
 * Kadu Sink Connector Metrics MBean Implementation
 *  
 * @author <a href="mailto:averemee@a2.solutions">Aleksei Veremeev</a>
 */
public class KuduSinkMetrics implements KuduSinkMetricsMBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(KuduSinkMetrics.class);
	private static final String DURATION_FMT = "%sdays %shrs %smin %ssec.\n";

	private long startTimeMillis;
	private LocalDateTime startTime;
	private int sessionsCreated;
	private long sessionCreateNanos;
	private long sessionCloseNanos;
	private long totalRecords;
	private long upsertCount;
	private long upsertNanos;
	private long deleteCount;
	private long deleteNanos;
	private int flushCount;
	private long flushNanos;

	public KuduSinkMetrics(final String connectorName) {
		
		startTimeMillis = System.currentTimeMillis();
		startTime = LocalDateTime.now();
		sessionsCreated = 0;
		sessionCreateNanos = 0;
		sessionCloseNanos = 0;
		totalRecords = 0;
		upsertCount = 0;
		upsertNanos = 0;
		deleteCount = 0;
		deleteNanos = 0;
		flushCount = 0;
		flushNanos = 0;

		final StringBuilder sb = new StringBuilder(96);
		sb.append("solutions.a2.kafka:type=Kudu-Sink-Connector-metrics,name=");
		sb.append(connectorName);
		try {
			final ObjectName name = new ObjectName(sb.toString());
			final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			if (mbs.isRegistered(name)) {
				LOGGER.warn("JMX MBean {} already registered, trying to remove it.", name.getCanonicalName());
				try {
					mbs.unregisterMBean(name);
				} catch (InstanceNotFoundException nfe) {
					LOGGER.error("Unable to unregister MBean {}", name.getCanonicalName());
					LOGGER.error(ExceptionUtils.getExceptionStackTrace(nfe));
					throw new ConnectException(nfe);
				}
			}
			mbs.registerMBean(this, name);
			LOGGER.debug("MBean {} registered.", sb.toString());
		} catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
			LOGGER.error("Unable to register MBean {} !!! ", sb.toString());
			LOGGER.error(ExceptionUtils.getExceptionStackTrace(e));
			throw new ConnectException(e);
		}
	}

	private static String formatDuration(Duration duration) {
		return String.format(DURATION_FMT,
				duration.toDays(),
				duration.toHours() % 24,
				duration.toMinutes() % 60,
				duration.getSeconds() % 60);
	}

	@Override
	public String getStartTime() {
		return startTime.format(DateTimeFormatter.ISO_DATE_TIME);
	}
	@Override
	public long getElapsedTimeMillis() {
		return System.currentTimeMillis() - startTimeMillis;
	}
	@Override
	public String getElapsedTime() {
		final Duration duration = Duration.ofMillis(System.currentTimeMillis() - startTimeMillis);
		return formatDuration(duration);
	}

	public void addMetrics(
			long sessionCreateNanos, long sessionCloseNanos,
			long upsertCount, long upsertNanos,
			long deleteCount, long deleteNanos,
			int flushCount, long flushNanos) {

		sessionsCreated++;
		this.sessionCreateNanos += sessionCreateNanos;
		this.sessionCloseNanos += sessionCloseNanos;

		totalRecords += upsertCount;
		this.upsertCount += upsertCount;
		this.upsertNanos += upsertNanos;
		
		totalRecords += deleteCount;
		this.deleteCount += deleteCount;
		this.deleteNanos += deleteNanos;

		this.flushCount += flushCount;
		this.flushNanos += flushNanos;
	}
	@Override
	public int getKuduSessionsCount() {
		return sessionsCreated;
	}
	@Override
	public long getKuduSessionsCreateMillis() {
		return sessionCreateNanos / 1_000_000;
	}
	@Override
	public String getKuduSessionsCreateTime() {
		final Duration duration = Duration.ofNanos(sessionCreateNanos);
		return formatDuration(duration);
	}
	@Override
	public long getKuduSessionsCloseMillis() {
		return sessionCloseNanos / 1_000_000;
	}
	@Override
	public String getKuduSessionsCloseTime() {
		final Duration duration = Duration.ofNanos(sessionCloseNanos);
		return formatDuration(duration);
	}
	@Override
	public long getTotalRecordsCount() {
		return totalRecords;
	}
	@Override
	public long getUpsertRecordsCount() {
		return upsertCount;
	}
	@Override
	public long getUpsertMillis() {
		return upsertNanos / 1_000_000;
	}
	@Override
	public String getUpsertTime() {
		final Duration duration = Duration.ofNanos(upsertNanos);
		return formatDuration(duration);
	}
	@Override
	public int getUpsertPerSecond() {
		if (upsertCount == 0) {
			return 0;
		} else if (upsertNanos == 0) {
			return Integer.MAX_VALUE;
		} else {
			return (int) (upsertCount / (upsertNanos / 1_000_000_000));
		}
	}
	@Override
	public long getDeleteRecordsCount() {
		return deleteCount;
	}
	@Override
	public long getDeleteMillis() {
		return deleteNanos / 1_000_000;
	}
	@Override
	public String getDeleteTime() {
		final Duration duration = Duration.ofNanos(deleteNanos);
		return formatDuration(duration);
	}
	@Override
	public int getDeletePerSecond() {
		if (deleteCount == 0) {
			return 0;
		} else if (deleteNanos == 0) {
			return Integer.MAX_VALUE;
		} else {
			return (int) (deleteCount / (deleteNanos / 1_000_000_000));
		}
	}
	@Override
	public int getKuduFlushesCount() {
		return flushCount;
	}
	@Override
	public long getKuduFlushesMillis() {
		return flushNanos / 1_000_000;
	}
	@Override
	public String getKuduFlushesTime() {
		final Duration duration = Duration.ofNanos(deleteNanos);
		return formatDuration(duration);
	}
	@Override
	public int getKuduFlushesPerSecond() {
		if (flushCount == 0) {
			return 0;
		} else if (flushNanos == 0) {
			return Integer.MAX_VALUE;
		} else {
			return (int) (flushCount / (flushNanos / 1_000_000_000));
		}
	}
	@Override
	public long getKuduProcessingMillis() {
		return (sessionCreateNanos + sessionCloseNanos + flushNanos + upsertNanos + deleteNanos) / 1_000_000;
	}
	@Override
	public String getKuduProcessingTime() {
		final Duration duration = Duration.ofNanos(sessionCreateNanos + sessionCloseNanos + flushNanos + upsertNanos + deleteNanos);
		return formatDuration(duration);
	}

}
