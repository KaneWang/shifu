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
package ml.shifu.core.core.dtrain;

import ml.shifu.core.container.obj.ColumnConfig;
import ml.shifu.core.util.Constants;
import ml.shifu.core.util.HDFSUtils;
import org.apache.hadoop.fs.Path;
import org.encog.Encog;
import org.encog.engine.network.activation.*;
import org.encog.mathutil.randomize.NguyenWidrowRandomizer;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;

import java.io.IOException;
import java.util.List;


/**
 * Helper class for NN distributed training.
 */
final class NNUtils {

    public static final String RESILIENTPROPAGATION = "R";
    public static final String SCALEDCONJUGATEGRADIENT = "S";
    public static final String MANHATTAN_PROPAGATION = "M";
    public static final String QUICK_PROPAGATION = "Q";
    public static final String BACK_PROPAGATION = "B";

    /**
     * The POSITIVE ETA value. This is specified by the resilient propagation
     * algorithm. This is the percentage by which the deltas are increased by if
     * the partial derivative is greater than zero.
     */
    public static final double POSITIVE_ETA = 1.2;

    /**
     * The NEGATIVE ETA value. This is specified by the resilient propagation
     * algorithm. This is the percentage by which the deltas are increased by if
     * the partial derivative is less than zero.
     */
    public static final double NEGATIVE_ETA = 0.5;

    /**
     * The minimum delta value for a weight matrix value.
     */
    public static final double DELTA_MIN = 1e-6;

    /**
     * The starting update for a delta.
     */
    public static final double DEFAULT_INITIAL_UPDATE = 0.1;

    /**
     * The maximum amount a delta can reach.
     */


    private NNUtils() {
    }

    /**
     * Check tmp dir for data set to store. If not exist, create it firstly.
     */
    private static Path getTmpDir() throws IOException {
        // If the Constants.TMP is absolute folder, there may be some conflicts for two jobs.
        Path path = new Path(Constants.TMP);
        if (!HDFSUtils.getLocalFS().exists(path)) {
            if (!HDFSUtils.getLocalFS().mkdirs(path)) {
                throw new RuntimeException("Error in creating tmp folder.");
            }
        }
        return path;
    }

    /**
     * Return testing file to store training data, if exists, delete it firstly.
     *
     * @throws IOException      if any exception on local fs operations.
     * @throws RuntimeException if error on deleting testing file.
     */
    public static Path getTestingFile() throws IOException {
        Path testingFile = new Path(getTmpDir(), NNConstants.TESTING_EGB);
        if (HDFSUtils.getLocalFS().exists(testingFile)) {
            if (!HDFSUtils.getLocalFS().delete(testingFile, true)) {
                throw new RuntimeException("error in deleting testing file");
            }
        }

        return testingFile;
    }

    /**
     * Return training file to store training data, if exists, delete it firstly.
     *
     * @throws IOException      if any exception on local fs operations.
     * @throws RuntimeException if error on deleting training file.
     */
    public static Path getTrainingFile() throws IOException {
        Path trainingFile = new Path(getTmpDir(), NNConstants.TRAINING_EGB);
        if (HDFSUtils.getLocalFS().exists(trainingFile)) {
            if (!HDFSUtils.getLocalFS().delete(trainingFile, true)) {
                throw new RuntimeException("error in deleting traing file");
            }
        }
        return trainingFile;
    }

    /**
     * Get input nodes number and output nodes number from column config.
     *
     * @throws NullPointerException if columnConfigList or ColumnConfig object in columnConfigList is null.
     */
    static int[] getInputAndOutputCounts(List<ColumnConfig> columnConfigList) {
        int input = 0;
        int output = 0;
        for (ColumnConfig config : columnConfigList) {
            if (config.isFinalSelect()) {
                input++;
            }
            if (config.isTarget()) {
                output++;
            }
        }
        return new int[]{input, output};
    }

    public static String getTmpNNModelName(String tmpModelsFolder, String trainerId, int iteration) {
        return new StringBuilder(200).append(tmpModelsFolder).append(Path.SEPARATOR_CHAR).append("model")
                .append(trainerId).append('-').append(iteration).append(".nn").toString();
    }

    static int tmpModelFactor(int epochs) {
        return Math.max(epochs / 50, 10);
    }

    /**
     * Generate basic NN network object
     */
    public static BasicNetwork generateNetwork(int in, int out, int numLayers, List<String> actFunc,
                                               List<Integer> hiddenNodeList) {

        final BasicNetwork network = new BasicNetwork();

        network.addLayer(new BasicLayer(new ActivationLinear(), true, in));

        //int hiddenNodes = 0;
        for (int i = 0; i < numLayers; i++) {
            String func = actFunc.get(i);
            Integer numHiddenNode = hiddenNodeList.get(i);
            //hiddenNodes += numHiddenNode;
            if (func.equalsIgnoreCase(NNConstants.NN_LINEAR)) {
                network.addLayer(new BasicLayer(new ActivationLinear(), true, numHiddenNode));
            } else if (func.equalsIgnoreCase(NNConstants.NN_SIGMOID)) {
                network.addLayer(new BasicLayer(new ActivationSigmoid(), true, numHiddenNode));
            } else if (func.equalsIgnoreCase(NNConstants.NN_TANH)) {
                network.addLayer(new BasicLayer(new ActivationTANH(), true, numHiddenNode));
            } else if (func.equalsIgnoreCase(NNConstants.NN_LOG)) {
                network.addLayer(new BasicLayer(new ActivationLOG(), true, numHiddenNode));
            } else if (func.equalsIgnoreCase(NNConstants.NN_SIN)) {
                network.addLayer(new BasicLayer(new ActivationSIN(), true, numHiddenNode));
            } else {
                network.addLayer(new BasicLayer(new ActivationSigmoid(), true, numHiddenNode));
            }
        }

        network.addLayer(new BasicLayer(new ActivationSigmoid(), false, out));

        network.getStructure().finalizeStructure();
        network.reset();

        return network;
    }

    /**
     * Determine the sign of the value.
     *
     * @param value The value to check.
     * @return -1 if less than zero, 1 if greater, or 0 if zero.
     */
    public static int sign(final double value) {
        if (Math.abs(value) < Encog.DEFAULT_DOUBLE_EQUAL) {
            return 0;
        } else if (value > 0) {
            return 1;
        } else {
            return -1;
        }
    }

    public static void randomize(int seed, double[] weights) {
        //ConsistentRandomizer randomizer = new ConsistentRandomizer(-1, 1, seed);
        NguyenWidrowRandomizer randomizer = new NguyenWidrowRandomizer(-1, 1);
        randomizer.randomize(weights);
    }

}
