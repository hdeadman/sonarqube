/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.monitoring;

import io.prometheus.client.CollectorRegistry;
import org.assertj.core.api.Assertions;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.es.EsClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ElasticSearchMetricStatusTaskTest {

  @Rule
  public LogTester logTester = new LogTester();

  private final ServerMonitoringMetrics serverMonitoringMetrics = mock(ServerMonitoringMetrics.class);
  private final EsClient esClient = mock(EsClient.class);
  private final Configuration configuration = new MapSettings().asConfig();

  private final ElasticSearchMetricStatusTask underTest = new ElasticSearchMetricStatusTask(serverMonitoringMetrics, esClient, configuration);

  @Before
  public void before() {
    CollectorRegistry.defaultRegistry.clear();
  }

  @Test
  public void when_elasticsearch_up_status_is_updated_to_green() {
    ClusterHealthResponse clusterHealthResponse = new ClusterHealthResponse();
    clusterHealthResponse.setStatus(ClusterHealthStatus.GREEN);
    when(esClient.clusterHealth(any())).thenReturn(clusterHealthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToGreen();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  public void when_elasticsearch_yellow_status_is_updated_to_green() {
    ClusterHealthResponse clusterHealthResponse = new ClusterHealthResponse();
    clusterHealthResponse.setStatus(ClusterHealthStatus.YELLOW);
    when(esClient.clusterHealth(any())).thenReturn(clusterHealthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToGreen();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  public void when_elasticsearch_down_status_is_updated_to_red() {
    ClusterHealthResponse clusterHealthResponse = new ClusterHealthResponse();
    clusterHealthResponse.setStatus(ClusterHealthStatus.RED);
    when(esClient.clusterHealth(any())).thenReturn(clusterHealthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  public void when_no_es_status_null_status_is_updated_to_red() {
    ClusterHealthResponse clusterHealthResponse = Mockito.mock(ClusterHealthResponse.class);
    when(clusterHealthResponse.getStatus()).thenReturn(null);
    when(esClient.clusterHealth(any())).thenReturn(clusterHealthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  public void when_es_status_throw_exception_status_is_updated_to_red() {
    when(esClient.clusterHealth(any())).thenThrow(new IllegalStateException("exception in cluster health"));

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verifyNoMoreInteractions(serverMonitoringMetrics);

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR)).containsOnly("Failed to query ES status");
  }

  @Test
  public void task_has_default_delay(){
    Assertions.assertThat(underTest.getDelay()).isPositive();
    Assertions.assertThat(underTest.getPeriod()).isPositive();
  }

}