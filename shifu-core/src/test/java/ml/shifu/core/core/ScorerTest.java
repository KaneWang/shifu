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

import ml.shifu.core.container.ScoreObject;
import ml.shifu.core.container.obj.ColumnConfig;
import ml.shifu.core.container.obj.ColumnConfig.ColumnType;
import ml.shifu.core.container.obj.ModelConfig;
import ml.shifu.core.container.obj.ModelTrainConf.ALGORITHM;
import ml.shifu.core.core.alg.NNTrainer;
import ml.shifu.core.core.alg.SVMTrainer;
import ml.shifu.core.util.Constants;
import org.apache.commons.io.FileUtils;
import org.encog.ml.BasicML;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataPair;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ScorerTest {

    List<BasicML> models = new ArrayList<BasicML>();
    MLDataSet set = new BasicMLDataSet();

    @BeforeClass
    public void setup() throws IOException {
        ModelConfig config = ModelConfig.createInitModelConfig(".", ALGORITHM.NN, ".");

        config.getTrain().getParams().put("Propagation", "B");
        config.getTrain().getParams().put("NumHiddenLayers", 2);
        config.getTrain().getParams().put("LearningRate", 0.5);
        List<Integer> nodes = new ArrayList<Integer>();
        nodes.add(3);
        nodes.add(4);
        List<String> func = new ArrayList<String>();
        func.add("linear");
        func.add("tanh");
        config.getTrain().getParams().put("NumHiddenNodes", nodes);
        config.getTrain().getParams().put("ActivationFunc", func);


        NNTrainer trainer = new NNTrainer(config, 0, false);

        double[] input = {0., 0.,};
        double[] ideal = {1.};
        MLDataPair pair = new BasicMLDataPair(new BasicMLData(input),
                new BasicMLData(ideal));
        set.add(pair);

        input = new double[]{0., 1.,};
        ideal = new double[]{0.};
        pair = new BasicMLDataPair(new BasicMLData(input),
                new BasicMLData(ideal));
        set.add(pair);

        input = new double[]{1., 0.,};
        ideal = new double[]{0.};
        pair = new BasicMLDataPair(new BasicMLData(input),
                new BasicMLData(ideal));
        set.add(pair);

        input = new double[]{1., 1.,};
        ideal = new double[]{1.};
        pair = new BasicMLDataPair(new BasicMLData(input),
                new BasicMLData(ideal));
        set.add(pair);

        trainer.setTrainSet(set);
        trainer.setValidSet(set);

        trainer.train();

        config.getTrain().setAlgorithm("SVM");
        config.getTrain().getParams().put("Kernel", "rbf");
        config.getTrain().getParams().put("Const", 0.1);
        config.getTrain().getParams().put("Gamma", 1.0);
        config.getVarSelect().setFilterNum(2);

        SVMTrainer svm = new SVMTrainer(config, 1, false);
        svm.setTrainSet(set);
        svm.setValidSet(set);

        svm.train();


        models.add(trainer.getNetwork());
        models.add(svm.getSVM());

    }

    //@Test
    public void ScoreTest() {

        List<ColumnConfig> list = new ArrayList<ColumnConfig>();
        ColumnConfig col = new ColumnConfig();
        col.setColumnType(ColumnType.N);
        col.setColumnName("A");
        col.setColumnNum(0);
        col.setFinalSelect(true);
        list.add(col);

        col = new ColumnConfig();
        col.setColumnType(ColumnType.N);
        col.setColumnName("B");
        col.setColumnNum(1);
        col.setFinalSelect(true);
        list.add(col);

        Scorer s = new Scorer(models, list, "NN");

        double[] input = {0., 0.,};
        double[] ideal = {1.};
        MLDataPair pair = new BasicMLDataPair(new BasicMLData(input),
                new BasicMLData(ideal));

        ScoreObject o = s.score(pair, null);
        List<Integer> scores = o.getScores();

        Assert.assertTrue(scores.get(0) > 400);
        Assert.assertTrue(scores.get(1) == 1000);
    }

    //@Test
    public void ScoreNull() {
        Scorer s = new Scorer(models, null, "NN");

        Assert.assertNull(s.score(null, null));
    }

    //@Test
    public void ScoreModelsException() {

        List<ColumnConfig> list = new ArrayList<ColumnConfig>();
        ColumnConfig col = new ColumnConfig();
        col.setColumnType(ColumnType.N);
        col.setColumnName("A");
        col.setColumnNum(0);
        col.setFinalSelect(true);
        list.add(col);

        col = new ColumnConfig();
        col.setColumnType(ColumnType.N);
        col.setColumnName("B");
        col.setColumnNum(1);
        col.setFinalSelect(true);
        list.add(col);

        Scorer s = new Scorer(models, list, "NN");

        double[] input = {0., 0., 3.};
        double[] ideal = {1.};
        MLDataPair pair = new BasicMLDataPair(new BasicMLData(input),
                new BasicMLData(ideal));

        Assert.assertNull(s.score(pair, null));
    }

    @AfterClass
    public void delete() throws IOException {
        FileUtils.deleteDirectory(new File("tmp"));

        FileUtils.deleteDirectory(new File("models"));
        FileUtils.deleteDirectory(new File("test-output"));
        FileUtils.deleteQuietly(new File(Constants.DEFAULT_META_COLUMN_FILE));
        FileUtils.deleteQuietly(new File(Constants.DEFAULT_CATEGORICAL_COLUMN_FILE));
        FileUtils.deleteQuietly(new File(Constants.DEFAULT_FORCESELECT_COLUMN_FILE));
        FileUtils.deleteQuietly(new File(Constants.DEFAULT_FORCEREMOVE_COLUMN_FILE));
        FileUtils.deleteQuietly(new File("Eval1" + Constants.DEFAULT_EVALSCORE_META_COLUMN_FILE));
    }
}
