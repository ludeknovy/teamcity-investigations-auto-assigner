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

package jetbrains.buildServer.investigationsAutoAssigner.persistent

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants
import jetbrains.buildServer.serverSide.ServerSettings
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class SuggestionsDaoTest {

    private lateinit var mySettings: ServerSettings
    private lateinit var myInstance: SuggestionsDao
    private lateinit var myArtifactsFile: Path
    private var myCorrectUUID = "239-239-239"
    private var myIncorrectUUID = "30-30-30"

    @BeforeMethod
    fun setUp() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val artifactsDir = fs.getPath("/" + Constants.ARTIFACT_DIRECTORY)
        Files.createDirectory(artifactsDir)
        myArtifactsFile = artifactsDir.resolve(Constants.ARTIFACT_FILENAME)
        Files.createFile(myArtifactsFile)

        mySettings = mock(ServerSettings::class.java)
        `when`(mySettings.serverUUID).thenReturn(myCorrectUUID)


        myInstance = SuggestionsDao(mySettings)
    }

    @Test
    fun testWriteOneRow() {
        val resInfo = ResponsibilityPersistentInfo("111", "1", "any reason")
        myInstance.write(myArtifactsFile, Arrays.asList(resInfo))

        val result = String(Files.readAllBytes(myArtifactsFile))

        Assert.assertEquals(result, readGold("SuggestionsDaoTest_TestOneRow_Gold.txt"))
    }

    @Test
    fun testWriteTwoRows() {
        val resInfo = ResponsibilityPersistentInfo("111", "1", "any reason")
        val resInfo2 = ResponsibilityPersistentInfo("112", "2", "any reason 2")
        myInstance.write(myArtifactsFile, Arrays.asList(resInfo, resInfo2))

        val result = String(Files.readAllBytes(myArtifactsFile))

        Assert.assertEquals(result, readGold("SuggestionsDaoTest_TestTwoRows_Gold.txt"))
    }

    @Test
    fun testReadOneRow() {
        Files.write(myArtifactsFile, readGold("SuggestionsDaoTest_TestOneRow_Gold.txt").toByteArray())

        val result = myInstance.read(myArtifactsFile)

        Assert.assertEquals(result.size, 1)
        Assert.assertEquals(result[0].testNameId, "111")
        Assert.assertEquals(result[0].investigatorId, "1")
        Assert.assertEquals(result[0].reason, "any reason")
    }

    @Test
    fun testReadTwoRows() {
        Files.write(myArtifactsFile, readGold("SuggestionsDaoTest_TestTwoRows_Gold.txt").toByteArray())

        val result = myInstance.read(myArtifactsFile)

        Assert.assertEquals(result.size, 2)
        Assert.assertEquals(result[0].testNameId, "111")
        Assert.assertEquals(result[0].investigatorId, "1")
        Assert.assertEquals(result[0].reason, "any reason")
        Assert.assertEquals(result[1].testNameId, "112")
        Assert.assertEquals(result[1].investigatorId, "2")
        Assert.assertEquals(result[1].reason, "any reason 2")
    }

    @Test
    fun testReadIncorrectUUID() {
        `when`(mySettings.serverUUID).thenReturn(myIncorrectUUID)
        Files.write(myArtifactsFile, readGold("SuggestionsDaoTest_TestOneRow_Gold.txt").toByteArray())

        val result = myInstance.read(myArtifactsFile)

        Assert.assertEquals(result.size, 0)
    }

    private fun readGold(resourceName: String): String {
        val resource = SuggestionsDao::class.java.getResource("/gold/$resourceName")

        return resource.readText()
    }
}