package ml.shifu.core.di.builtin.dataDictionary;

import com.google.common.base.Splitter;
import ml.shifu.core.di.spi.DataDictionaryInitializer;
import ml.shifu.core.request.RequestObject;
import ml.shifu.core.util.PMMLUtils;
import ml.shifu.core.util.Params;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CSVDataDictionaryInitializer implements DataDictionaryInitializer {

    static Logger log = LoggerFactory.getLogger(CSVDataDictionaryInitializer.class);

    public DataDictionary init(RequestObject req) {
        DataDictionary dict = new DataDictionary();

        Params globalParams = req.getGlobalParams();


        String pathCSV = (String) globalParams.get("pathCSV");
        String delimiter = (String) globalParams.get("delimiter", ",");


        String header;
        Scanner scanner = null;

        try {
            scanner = new Scanner(new BufferedReader(new FileReader(pathCSV)));
            header = scanner.nextLine();

            List<DataField> fields = new ArrayList<DataField>();
            for (String fieldNameString : Splitter.on(delimiter).split(header)) {
                DataField field = new DataField();
                field.setName(FieldName.create(fieldNameString));

                Params fieldParams = req.getFieldParams(fieldNameString);

                field.setOptype(PMMLUtils.getOpTypeFromParams(fieldParams));
                field.setDataType(PMMLUtils.getDataTypeFromParams(fieldParams));

                fields.add(field);
            }

            dict.withDataFields(fields);
            dict.setNumberOfFields(fields.size());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot load file.");
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        return dict;
    }
}
