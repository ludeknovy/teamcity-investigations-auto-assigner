/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.heuristics.Heuristic;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.users.SUser;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Test
public class ResponsibleUserFinderTest extends BaseTestCase {

  private ResponsibleUserFinder myUserFinder;
  private Heuristic myHeuristic;
  private Heuristic myHeuristic2;
  private SBuild mySBuild;
  private STestRun mySTestRun;
  private SProject mySProject;
  private List<STestRun> myTestWrapper;
  private CustomParameters myCustomParameters;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myHeuristic = Mockito.mock(Heuristic.class);
    myHeuristic2 = Mockito.mock(Heuristic.class);
    mySBuild = Mockito.mock(SBuild.class);
    mySProject = Mockito.mock(SProject.class);
    mySTestRun = Mockito.mock(STestRun.class);
    myCustomParameters = Mockito.mock(CustomParameters.class);
    myTestWrapper = Collections.singletonList(mySTestRun);
    myUserFinder = new ResponsibleUserFinder(Arrays.asList(myHeuristic, myHeuristic2), myCustomParameters);
    HeuristicResult heuristicResult1 = new HeuristicResult();
    HeuristicResult heuristicResult2 = new HeuristicResult();
    when(myHeuristic.findResponsibleUser(any())).thenReturn(heuristicResult1);
    when(myHeuristic.getId()).thenReturn("heuristicId1");
    when(myHeuristic2.getId()).thenReturn("heuristicId2");
    when(myHeuristic2.findResponsibleUser(any())).thenReturn(heuristicResult2);
    when(myCustomParameters.isHeuristicsDisabled(anyString())).thenReturn(false);
  }

  public void Test_FindResponsibleUser_ResponsibleNotFound() {
    HeuristicResult result =
      myUserFinder.findResponsibleUser(mySBuild, mySProject, Collections.emptyList(), myTestWrapper);

    Assert.assertTrue(result.isEmpty());
  }

  public void Test_FindResponsibleUser_CheckSecondIfNotFoundInFirst() {
    HeuristicResult emptyHeuristicResult = new HeuristicResult();
    when(myHeuristic.findResponsibleUser(any())).thenReturn(emptyHeuristicResult);

    myUserFinder.findResponsibleUser(mySBuild, mySProject, Collections.emptyList(), myTestWrapper);

    Mockito.verify(myHeuristic2, Mockito.atLeastOnce()).findResponsibleUser(any());
  }

  public void Test_FindResponsibleUser_NotCallSecondIfFoundInFirst() {
    SUser sUser = Mockito.mock(SUser.class);
    HeuristicResult heuristicResult = new HeuristicResult();
    heuristicResult.addResponsibility(mySTestRun, new Responsibility(sUser, "Failed description"));
    when(myHeuristic.findResponsibleUser(any())).thenReturn(heuristicResult);

    myUserFinder.findResponsibleUser(mySBuild, mySProject, Collections.emptyList(), myTestWrapper);

    Mockito.verify(myHeuristic2, Mockito.never()).findResponsibleUser(any());
  }

  public void Test_FindResponsibleUser_TakeFirstFound() {
    SUser sUser = Mockito.mock(SUser.class);
    HeuristicResult heuristicResult = new HeuristicResult();
    HeuristicResult heuristicResult2 = new HeuristicResult();
    heuristicResult.addResponsibility(mySTestRun, new Responsibility(sUser,"Failed description"));
    when(myHeuristic.findResponsibleUser(any())).thenReturn(heuristicResult);
    heuristicResult2.addResponsibility(mySTestRun, new Responsibility(sUser,"Failed description 2"));
    when(myHeuristic2.findResponsibleUser(any())).thenReturn(heuristicResult2);

    HeuristicResult result =
      myUserFinder.findResponsibleUser(mySBuild, mySProject, Collections.emptyList(), myTestWrapper);
    Assert.assertFalse(result.isEmpty());
    Assert.assertNotNull(result.getResponsibility(mySTestRun));
    Responsibility responsibility = result.getResponsibility(mySTestRun);
    assert responsibility != null;
    Assert.assertEquals(responsibility.getDescription(), "Failed description");
  }

  public void Test_FindResponsibleUser_HeuristicIsDisabled() {
    when(myCustomParameters.isHeuristicsDisabled(myHeuristic.getId())).thenReturn(true);
    when(myCustomParameters.isHeuristicsDisabled(myHeuristic2.getId())).thenReturn(false);
    myUserFinder.findResponsibleUser(mySBuild, mySProject, Collections.emptyList(), myTestWrapper);

    Mockito.verify(myHeuristic2, Mockito.atLeastOnce()).findResponsibleUser(any());
  }
}
