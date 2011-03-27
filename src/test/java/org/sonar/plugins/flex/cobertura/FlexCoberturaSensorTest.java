/*
 * Sonar Flex Plugin
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.flex.cobertura;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.plugins.flex.Flex;

public class FlexCoberturaSensorTest {
  private FlexCoberturaSensor sensor;
  private Project project;

  @Before
  public void setUp() throws Exception {
    sensor = new FlexCoberturaSensor();
    project = mock(Project.class);
  }

  @Test
  public void shouldAnalyseIfReuseReports() {
    when(project.getLanguageKey()).thenReturn(Flex.KEY);
    when(project.getAnalysisType())
        .thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(sensor.shouldExecuteOnProject(project), is(true));
  }
}
