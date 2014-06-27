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
import ml.shifu.core.core.alg.SVMTrainer;
import org.encog.Encog;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataPair;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.ml.svm.SVM;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;


public class SVMTrainerTest {

    SVMTrainer trainer;
    ModelConfig config;

    private final static MLDataSet xor_Trainset = new BasicMLDataSet();
    //private final static Integer numberXorSet = 4 * 3;
    private final static MLDataSet xor_Validset = new BasicMLDataSet();

    static {
        double[] input = {0., 0.,};
        double[] ideal = {0.};
        MLDataPair pair = new BasicMLDataPair(new BasicMLData(input),
                new BasicMLData(ideal));
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Validset.add(pair);

        input = new double[]{0., 1.,};
        ideal = new double[]{1.};
        pair = new BasicMLDataPair(new BasicMLData(input), new BasicMLData(
                ideal));
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Validset.add(pair);

        input = new double[]{1., 0.,};
        ideal = new double[]{1.};
        pair = new BasicMLDataPair(new BasicMLData(input), new BasicMLData(
                ideal));
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Validset.add(pair);

        input = new double[]{1., 1.,};
        ideal = new double[]{0.};
        pair = new BasicMLDataPair(new BasicMLData(input), new BasicMLData(
                ideal));
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Trainset.add(pair);
        xor_Validset.add(pair);
    }

    //MLDataSet dataSet;
    //MLDataSet trainSet;
    //MLDataSet validSet, testSet;
    Random random;

    SVM svm;

    @BeforeClass
    public void setUp() throws IOException {
        config = new ModelConfig(); //.createInitModelConfig("./", "./");
        config.getTrain().setAlgorithm("SVM");
        config.getDataSet().setSource(SourceType.LOCAL);
        config.getVarSelect().setFilterNum(2);
        config.getDataSet().setDataDelimiter(",");
        config.getDataSet().setSource(SourceType.HDFS);

        config.getTrain().setParams(new HashMap<String, Object>());
        config.getTrain().getParams().put("Const", 1.1);
        config.getTrain().getParams().put("Gamma", 0.95);
        config.getTrain().getParams().put("Kernel", "rbf");
        config.getTrain().setBaggingSampleRate(1.0);
        config.getTrain().setBaggingWithReplacement(false);

        trainer = new SVMTrainer(config, 0, false);
        trainer.setTrainSet(xor_Trainset);
        trainer.setValidSet(xor_Validset);
    }

    @Test
    public void SVMTest() throws IOException {

        trainer.train();

        svm = trainer.getSVM();

        Assert.assertEquals(4, trainer.getValidSet().getRecordCount());

    }

    @AfterClass
    public void shutDown() {
        File file = new File("./models/");
        if (file.exists() && file.isDirectory()) {
            for (File f : file.listFiles()) {
                f.delete();
            }
            file.delete();
        }
        file = new File("./modelsTmp/");
        if (file.exists() && file.isDirectory()) {
            for (File f : file.listFiles()) {
                f.delete();
            }
            file.delete();
        }
        Encog.getInstance().shutdown();
    }

}
