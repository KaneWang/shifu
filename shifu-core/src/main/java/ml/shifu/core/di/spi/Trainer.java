package ml.shifu.core.di.spi;

import ml.shifu.core.container.PMMLDataSet;
import ml.shifu.core.util.Params;

public interface Trainer {

    public void train(PMMLDataSet pmmlDataSet, Params params) throws Exception;

}
