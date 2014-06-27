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
package ml.shifu.core.core.alg;

import ml.shifu.core.container.obj.ModelConfig;
import ml.shifu.core.core.AbstractTrainer;
import org.encog.engine.network.activation.ActivationLinear;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.Propagation;
import org.encog.neural.networks.training.propagation.quick.QuickPropagation;
import org.encog.persist.EncogDirectoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;


/**
 * Implementation of AbstractTrainer for LogisticRegression
 */
public class LogisticRegressionTrainer extends AbstractTrainer {

    public static final String LEARNING_RATE = "LearningRate";

    private BasicNetwork classifier;
    protected Logger log = LoggerFactory.getLogger(LogisticRegressionTrainer.class);

    public LogisticRegressionTrainer(ModelConfig modelConfig, int trainerID, Boolean dryRun) {
        super(modelConfig, trainerID, dryRun);

    }

    public void setDataSet(MLDataSet masterDataSet) throws IOException {
        super.setDataSet(masterDataSet);
    }

    /**
     * {@inheritDoc}
     * </p>
     * no <code>regularization</code>
     * </p>
     * Regular will be provide later
     * </p>
     *
     * @throws IOException e
     */
    @Override
    public void train() throws IOException {
        log.info("Using logistic regression algorithm...");

        log.info("Input Size: " + trainSet.getInputSize());
        log.info("Ideal Size: " + trainSet.getIdealSize());

        classifier = new BasicNetwork();

        classifier.addLayer(new BasicLayer(new ActivationLinear(), true, trainSet.getInputSize()));
        classifier.addLayer(new BasicLayer(new ActivationSigmoid(), false, trainSet.getIdealSize()));
        classifier.getStructure().finalizeStructure();

        //resetParams(classifier);
        classifier.reset();

        //Propagation mlTrain = getMLTrain();
        Propagation propagtion = new QuickPropagation(classifier, trainSet, (Double) modelConfig.getParams().get("LearningRate"));
        int epochs = modelConfig.getNumTrainEpochs();

        log.info("Using " + (Double) modelConfig.getParams().get("LearningRate") + " training rate");

        for (int i = 0; i < epochs; i++) {

            propagtion.iteration();
            double trainError = propagtion.getError();
            double validError = classifier.calculateError(this.validSet);

            log.info("Epoch #" + (i + 1) + " Train Error:" + df.format(trainError) + " Validation Error:" + df.format(validError));
        }
        propagtion.finishTraining();

        log.info("#" + this.trainerID + " finish training");

        saveLR();
    }

    private void saveLR() throws IOException {
        File folder = new File("models");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        EncogDirectoryPersistence.saveObject(new File("./models/model" + this.trainerID + ".lr"), classifier);
    }

    public BasicNetwork getClassifier() {
        return classifier;
    }
}
