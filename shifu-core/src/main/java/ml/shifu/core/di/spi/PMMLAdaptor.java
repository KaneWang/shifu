package ml.shifu.core.di.spi;

public interface PMMLAdaptor {

    public void convertToPMML(String pathInput, String pathOutput);

    public void convertFromPMML(String pathInput, String PathOutput);

}
