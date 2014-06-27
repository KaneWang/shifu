/**
 * Copyright [2012-2014] eBay Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ml.shifu.core.util;

import ml.shifu.core.container.obj.RawSourceData.SourceType;
import ml.shifu.core.fs.ShifuFileUtils;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * ManagerTest class
 */
public class ShifuCLITest {

    @ObjectFactory
    public IObjectFactory setObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @BeforeClass
    public void setUp() {
        Environment.setProperty(Environment.SHIFU_HOME, ".");
    }

    // @Test
    public void testInitializeModelOld() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(new File(
                "src/test/resources/unittest/ModelStore/ModelSet1/ModelConfig.json")));
        BufferedReader reader2 = new BufferedReader(new FileReader(new File(
                "src/test/resources/common/VariableStore.json")));
        String[] headers = "id|diagnosis|column_3|column_4|column_5|column_6|column_7|column_8|column_9|column_10|column_11|column_12|column_13|column_14|column_15|column_16|column_17|column_18|column_19|column_20|column_21|column_22|column_23|column_24|column_25|column_26|column_27|column_28|column_29|column_30|column_31|column_32|result"
                .split("\\|");

        PowerMock.mockStaticPartial(CommonUtils.class, "getReader", "getHeaders");

        EasyMock.expect(ShifuFileUtils.getReader("./ModelConfig.json", SourceType.LOCAL)).andReturn(reader).anyTimes();
        EasyMock.expect(ShifuFileUtils.getReader("common/VariableStore.json", SourceType.LOCAL)).andReturn(reader2)
                .anyTimes();
        EasyMock.expect(
                CommonUtils.getHeaders("./src/test/resources/unittest/DataStore/DataSet1/.pig_header", "|",
                        SourceType.LOCAL)
        ).andReturn(headers).anyTimes();

        PowerMock.replayAll(CommonUtils.class);

        ShifuCLI.initializeModel();

        File columnConfig = new File("./ColumnConfig.json");
        File tmp = new File("tmp");

        Assert.assertTrue(columnConfig.exists());
        Assert.assertTrue(tmp.exists());

        columnConfig.deleteOnExit();
        FileUtils.deleteDirectory(tmp);

        reader.close();
        reader2.close();
    }

    @Test
    public void testCreateModel() throws Exception {
        Environment.setProperty(Environment.SHIFU_HOME, "src/test/resources");
        ShifuCLI.createNewModel("TestModel", null, "It's a model for Unittest");

        File file = new File("TestModel");
        Assert.assertTrue(file.exists());
        FileUtils.deleteDirectory(file);
    }

    @Test
    public void testInitializeModel() throws Exception {
        File originModel = new File("src/test/resources/unittest/ModelSets/full/ModelConfig.json");
        File tmpModel = new File("ModelConfig.json");

        FileUtils.copyFile(originModel, tmpModel);

        ShifuCLI.initializeModel();

        File file = new File("ColumnConfig.json");
        Assert.assertTrue(file.exists());
        FileUtils.deleteQuietly(file);
        FileUtils.deleteQuietly(tmpModel);
    }

    @Test
    public void testCalculateModelStats() throws Exception {
        File originModel = new File("src/test/resources/unittest/ModelSets/full/ModelConfig.json");
        File tmpModel = new File("ModelConfig.json");

        File originColumn = new File(
                "src/test/resources/unittest/ModelSets/full/ColumnConfig.json");
        File tmpColumn = new File("ColumnConfig.json");

        FileUtils.copyFile(originModel, tmpModel);
        FileUtils.copyFile(originColumn, tmpColumn);
        long timestamp = tmpColumn.lastModified();

        ShifuCLI.initializeModel();
        ShifuCLI.calModelStats();

        Assert.assertTrue(tmpColumn.lastModified() > timestamp);
        FileUtils.deleteQuietly(tmpModel);
        FileUtils.deleteQuietly(tmpColumn);
    }

    @Test
    public void testSelectModelVar() throws Exception {
        File originModel = new File("src/test/resources/unittest/ModelSets/full/ModelConfig.json");
        File tmpModel = new File("ModelConfig.json");

        File originColumn = new File(
                "src/test/resources/unittest/ModelSets/full/ColumnConfig.json");
        File tmpColumn = new File("ColumnConfig.json");

        FileUtils.copyFile(originModel, tmpModel);
        FileUtils.copyFile(originColumn, tmpColumn);

        long timestamp = tmpColumn.lastModified();
        ShifuCLI.selectModelVar();
        Assert.assertTrue(tmpColumn.lastModified() > timestamp);

        FileUtils.deleteQuietly(tmpModel);
        FileUtils.deleteQuietly(tmpColumn);
    }

    @Test
    public void testNormalizeData() throws Exception {
        File originModel = new File("src/test/resources/unittest/ModelSets/full/ModelConfig.json");
        File tmpModel = new File("ModelConfig.json");

        File originColumn = new File(
                "src/test/resources/unittest/ModelSets/full/ColumnConfig.json");
        File tmpColumn = new File("ColumnConfig.json");

        FileUtils.copyFile(originModel, tmpModel);
        FileUtils.copyFile(originColumn, tmpColumn);

        ShifuCLI.initializeModel();
        ShifuCLI.calModelStats();
        ShifuCLI.normalizeTrainData();

        File normalizedData = new File("tmp/NormalizedData");
        File selectedData = new File("tmp/SelectedRawData");
        Assert.assertTrue(normalizedData.exists());
        Assert.assertTrue(selectedData.exists());

        FileUtils.deleteQuietly(tmpModel);
        FileUtils.deleteQuietly(tmpColumn);
        FileUtils.deleteDirectory(new File("tmp"));
    }

    @Test
    public void testTrainModel() throws Exception {
        File originModel = new File("src/test/resources/unittest/ModelSets/full/ModelConfig.json");
        File tmpModel = new File("ModelConfig.json");

        File originColumn = new File(
                "src/test/resources/unittest/ModelSets/full/ColumnConfig.json");
        File tmpColumn = new File("ColumnConfig.json");

        FileUtils.copyFile(originModel, tmpModel);
        FileUtils.copyFile(originColumn, tmpColumn);
        // run normalization
        ShifuCLI.normalizeTrainData();

        // run train
        ShifuCLI.trainModel(false, false);

        File modelFile = new File("models/model1.nn");
        Assert.assertTrue(modelFile.exists());

        FileUtils.deleteQuietly(tmpModel);
        FileUtils.deleteQuietly(tmpColumn);
        FileUtils.deleteDirectory(new File("tmp"));
        FileUtils.deleteDirectory(new File("models"));
    }

    @Test
    public void testPostTrainModel() throws Exception {
        File originModel = new File("src/test/resources/unittest/ModelSets/full/ModelConfig.json");
        File tmpModel = new File("ModelConfig.json");

        File originColumn = new File(
                "src/test/resources/unittest/ModelSets/full/ColumnConfig.json");
        File tmpColumn = new File("ColumnConfig.json");

        File modelsDir = new File("src/test/resources/unittest/ModelSets/full/models");
        File tmpModelsDir = new File("models");

        FileUtils.copyFile(originModel, tmpModel);
        FileUtils.copyFile(originColumn, tmpColumn);
        FileUtils.copyDirectory(modelsDir, tmpModelsDir);

        long timestamp = tmpColumn.lastModified();
        // run post-train
        ShifuCLI.initializeModel();
        ShifuCLI.calModelStats();
        ShifuCLI.selectModelVar();
        ShifuCLI.normalizeTrainData();
        ShifuCLI.postTrainModel();
        Assert.assertTrue(tmpColumn.lastModified() > timestamp);

        FileUtils.deleteQuietly(tmpModel);
        FileUtils.deleteQuietly(tmpColumn);
        FileUtils.deleteDirectory(new File("tmp"));
        FileUtils.deleteDirectory(new File("models"));
    }

    @Test
    public void testRunEvalAll() throws Exception {
        File originModel = new File("src/test/resources/unittest/ModelSets/full/ModelConfig.json");
        File tmpModel = new File("ModelConfig.json");

        File originColumn = new File(
                "src/test/resources/unittest/ModelSets/full/ColumnConfig.json");
        File tmpColumn = new File("ColumnConfig.json");

        File modelsDir = new File("src/test/resources/unittest/ModelSets/full/models");
        File tmpModelsDir = new File("models");

        FileUtils.copyFile(originModel, tmpModel);
        FileUtils.copyFile(originColumn, tmpColumn);
        FileUtils.copyDirectory(modelsDir, tmpModelsDir);

        // run evaluation set
        ShifuCLI.runEvalSet(false);
        File evalScore = new File("evals/Eval1/EvalScore");
        Assert.assertTrue(evalScore.exists());

        FileUtils.deleteQuietly(tmpModel);
        FileUtils.deleteQuietly(tmpColumn);
        FileUtils.deleteQuietly(new File("evals/Eval1/EvalConfusionMatrix"));
        FileUtils.deleteQuietly(new File("evals/EvalB/EvalConfusionMatrix"));
    }

    @Test
    public void testCreateEvalSet() throws Exception {
        File originModel = new File("src/test/resources/unittest/ModelSets/full/ModelConfig.json");
        File tmpModel = new File("ModelConfig.json");
        File originColumn = new File(
                "src/test/resources/unittest/ModelSets/full/ColumnConfig.json");
        File tmpColumn = new File("ColumnConfig.json");

        FileUtils.copyFile(originModel, tmpModel);
        FileUtils.copyFile(originColumn, tmpColumn);

        long timestamp = tmpModel.lastModified();
        // run create evaluation set
        ShifuCLI.createNewEvalSet("EvalC");
        Assert.assertTrue(tmpModel.lastModified() > timestamp);

        FileUtils.deleteQuietly(tmpModel);
        FileUtils.deleteQuietly(new File("EvalC" + Constants.DEFAULT_EVALSCORE_META_COLUMN_FILE));
    }

    @AfterTest
    public void delete() throws IOException {
        FileUtils.deleteDirectory(new File("evals"));
    }

}
