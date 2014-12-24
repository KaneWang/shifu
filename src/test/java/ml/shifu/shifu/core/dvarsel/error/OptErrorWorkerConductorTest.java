package ml.shifu.shifu.core.dvarsel.error;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.Assert;
import ml.shifu.shifu.container.obj.ColumnConfig;
import ml.shifu.shifu.container.obj.ModelConfig;
import ml.shifu.shifu.container.obj.RawSourceData;
import ml.shifu.shifu.core.Normalizer;
import ml.shifu.shifu.core.dtrain.NNConstants;
import ml.shifu.shifu.core.dvarsel.VarSelMasterResult;
import ml.shifu.shifu.core.dvarsel.VarSelWorkerResult;
import ml.shifu.shifu.core.dvarsel.dataset.TrainingDataSet;
import ml.shifu.shifu.core.dvarsel.dataset.TrainingRecord;
import ml.shifu.shifu.core.dvarsel.wrapper.WrapperWorkerConductor;
import ml.shifu.shifu.util.CommonUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 12/4/2014.
 */
public class OptErrorWorkerConductorTest {

    @Test
    public void testErrorConductor() throws IOException {
        ModelConfig modelConfig = CommonUtils.loadModelConfig(
                "src/test/resources/example/cancer-judgement/ModelStore/ModelSet1/ModelConfig.json",
                RawSourceData.SourceType.LOCAL);
        List<ColumnConfig> columnConfigList = CommonUtils.loadColumnConfigList(
                "src/test/resources/example/cancer-judgement/ModelStore/ModelSet1/ColumnConfig.json",
                RawSourceData.SourceType.LOCAL);

        OptErrorWorkerConductor optErrorWorker = new OptErrorWorkerConductor(modelConfig, columnConfigList);
        TrainingDataSet trainingDataSet = genTrainingDataSet(modelConfig, columnConfigList);
        optErrorWorker.retainData(trainingDataSet);

        List<Integer> workingList = new ArrayList<Integer>();
        while ( workingList.size() < 5 ) {
            optErrorWorker.consumeMasterResult(new VarSelMasterResult(workingList));
            VarSelWorkerResult result = optErrorWorker.generateVarSelResult();
            if ( result.getColumnIdList().get(0) != -1  ) {
                workingList.add(result.getColumnIdList().get(0));
            }
        }

        System.out.println(workingList.toString());
        Assert.assertEquals(5, workingList.size());
    }

    public TrainingDataSet genTrainingDataSet(ModelConfig modelConfig, List<ColumnConfig> columnConfigList) throws IOException {
        List<Integer> columnIdList = new ArrayList<Integer>();
        for (ColumnConfig columnConfig : columnConfigList) {
            if (columnConfig.isCandidate()) {
                columnIdList.add(columnConfig.getColumnNum());
            }
        }

        TrainingDataSet trainingDataSet = new TrainingDataSet(columnIdList);
        List<String> recordsList = IOUtils.readLines(
                new FileInputStream("src/test/resources/example/cancer-judgement/DataStore/DataSet1/part-00"));
        for (String record : recordsList) {
            addRecordIntoTrainDataSet(modelConfig, columnConfigList, trainingDataSet, record);
        }

        return trainingDataSet;
    }

    public void addRecordIntoTrainDataSet(ModelConfig modelConfig,
                                          List<ColumnConfig> columnConfigList,
                                          TrainingDataSet trainingDataSet,
                                          String record) {
        String[] fields = CommonUtils.split(record, modelConfig.getDataSetDelimiter());

        int targetColumnId = CommonUtils.getTargetColumnNum(columnConfigList);
        String tag = StringUtils.trim(fields[targetColumnId]);

        double[] inputs = new double[trainingDataSet.getDataColumnIdList().size()];
        String[] raw = new String[trainingDataSet.getDataColumnIdList().size()];
        double[] ideal = new double[1];

        double significance = NNConstants.DEFAULT_SIGNIFICANCE_VALUE;

        ideal[0] = (modelConfig.getPosTags().contains(tag) ? 1.0d : 0.0d);

        int i = 0;
        for ( Integer columnId : trainingDataSet.getDataColumnIdList() ) {
            raw[i] = fields[columnId];
            inputs[i++] = Normalizer.normalize(columnConfigList.get(columnId), fields[columnId]);
        }

        trainingDataSet.addTrainingRecord(new TrainingRecord(raw, inputs, ideal, significance));
    }
}
