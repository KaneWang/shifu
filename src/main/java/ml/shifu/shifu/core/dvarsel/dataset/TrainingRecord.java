package ml.shifu.shifu.core.dvarsel.dataset;
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

import java.util.List;
import java.util.Set;

import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataPair;

/**
 * Created on 11/24/2014.
 */
public class TrainingRecord {
    private String[] raw;
    private double[] inputs;
    private double[] ideal;
    private double significance;

    public TrainingRecord(String[] raw, double[] inputs, double[] ideal, double significance) {
        this.raw = raw;
        this.inputs = inputs;
        this.ideal = ideal;
        this.significance = significance;
    }

    public String[] getRaw() {
        return this.raw;
    }

    public double[] getInputs() {
        return this.inputs;
    }

    public double[] getIdeal() {
        return  this.ideal;
    }

    public double getSignificance() {
        return this.significance;
    }

    public MLDataPair toMLDataPair(List<Integer> dataColumnIdList, Set<Integer> workingColumnSet) {
        double[] params = new double[workingColumnSet.size()];

        int pos = 0;
        for (int i = 0; i < dataColumnIdList.size(); i ++) {
            if ( workingColumnSet.contains(dataColumnIdList.get(i)) ) {
                params[pos++] = inputs[i];
            }
        }

        return new BasicMLDataPair(new BasicMLData(params), new BasicMLData(this.ideal));
    }

}
