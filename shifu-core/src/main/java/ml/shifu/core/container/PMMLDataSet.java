package ml.shifu.core.container;

import org.dmg.pmml.MiningSchema;

import java.util.List;

public class PMMLDataSet {

    public MiningSchema getMiningSchema() {
        return miningSchema;
    }

    public void setMiningSchema(MiningSchema miningSchema) {
        this.miningSchema = miningSchema;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public void setRows(List<List<Object>> rows) {
        this.rows = rows;
    }

    private MiningSchema miningSchema;
    private List<List<Object>> rows;

}
