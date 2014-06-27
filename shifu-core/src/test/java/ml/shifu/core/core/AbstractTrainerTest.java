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
package ml.shifu.core.core;

import ml.shifu.core.container.obj.ModelConfig;
import ml.shifu.core.container.obj.RawSourceData.SourceType;
import ml.shifu.core.core.alg.NNTrainer;
import ml.shifu.core.util.CommonUtils;
import org.apache.commons.io.FileUtils;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataPair;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.Random;


public class AbstractTrainerTest {

    private Random random;

    @BeforeClass
    public void setUp() throws IOException {
        random = new Random(new Date().getTime());
    }

    //@Test
    public void testLoad1() throws IOException {
        MLDataSet set = new BasicMLDataSet();
        ModelConfig modelConfig = CommonUtils.loadModelConfig(
                "src/test/resources/unittest/ModelSets/full/ModelConfig.json",
                SourceType.LOCAL);

        double[] input = new double[modelConfig.getVarSelectFilterNum()];

        for (int j = 0; j < 1000; j++) {
            for (int i = 0; i < modelConfig.getVarSelectFilterNum(); i++) {
                input[i] = random.nextDouble();
            }

            double[] ideal = new double[1];
            ideal[0] = random.nextInt(2);
            MLDataPair pair = new BasicMLDataPair(new BasicMLData(input),
                    new BasicMLData(ideal));

            set.add(pair);
        }

        modelConfig.getTrain().setTrainOnDisk(false);
        AbstractTrainer trainer = new NNTrainer(modelConfig, 0, false);
        trainer.setDataSet(set);

        Assert.assertTrue(trainer.getTrainSet().getRecordCount() <= (1 - modelConfig
                .getCrossValidationRate())
                * modelConfig.getBaggingSampleRate()
                * set.getRecordCount() * 1.05);
        Assert.assertTrue(trainer.getTrainSet().getRecordCount() >= (1 - modelConfig
                .getCrossValidationRate())
                * modelConfig.getBaggingSampleRate()
                * set.getRecordCount() * 0.95);

        modelConfig.getTrain().setFixInitInput(true);
        trainer = new NNTrainer(modelConfig, 0, false);
        trainer.setDataSet(set);

        Assert.assertTrue(trainer.getTrainSet().getRecordCount() <= (1 - modelConfig
                .getCrossValidationRate())
                * modelConfig.getBaggingSampleRate()
                * set.getRecordCount() * 1.05);
        Assert.assertTrue(trainer.getTrainSet().getRecordCount() >= (1 - modelConfig
                .getCrossValidationRate())
                * modelConfig.getBaggingSampleRate()
                * set.getRecordCount() * 0.95);

        modelConfig.getTrain().setFixInitInput(false);
        modelConfig.getTrain().setBaggingWithReplacement(false);
        trainer = new NNTrainer(modelConfig, 0, false);
        trainer.setDataSet(set);

        Assert.assertTrue(trainer.getTrainSet().getRecordCount() <= (1 - modelConfig
                .getCrossValidationRate())
                * modelConfig.getBaggingSampleRate()
                * set.getRecordCount() * 1.05);
        Assert.assertTrue(trainer.getTrainSet().getRecordCount() >= (1 - modelConfig
                .getCrossValidationRate())
                * modelConfig.getBaggingSampleRate()
                * set.getRecordCount() * 0.95);

    }

    @AfterClass
    public void delete() throws IOException {
        File json = new File(".");
        for (File tmp : json.listFiles(filter)) {
            tmp.delete();
        }

        FileUtils.deleteDirectory(new File("tmp"));
    }

    private FilenameFilter filter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {

            return name.toLowerCase().startsWith("init") && name.toLowerCase().endsWith("json");
        }
    };
}
