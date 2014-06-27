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
package ml.shifu.core.container.obj;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ml.shifu.core.container.obj.RawSourceData.SourceType;
import ml.shifu.core.fs.PathFinder;
import ml.shifu.core.util.CommonUtils;
import ml.shifu.core.util.Constants;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EvalConfig class
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvalConfig {

    private String name;
    private RawSourceData dataSet;

    private Integer performanceBucketNum = 10;
    private String performanceScoreSelector = "mean";
    private String scoreMetaColumnNameFile;
    private Map<String, String> customPaths;

    public EvalConfig() {
        customPaths = new HashMap<String, String>(1);
        /**
         * Since most user won't use this function,
         * hidden the custom paths for creating new model.
         */
        /*customPaths.put(Constants.KEY_MODELS_PATH, null);
        customPaths.put(Constants.KEY_SCORE_PATH, null);
	    customPaths.put(Constants.KEY_CONFUSION_MATRIX_PATH, null);
	    customPaths.put(Constants.KEY_PERFORMANCE_PATH, null);*/
    }

    /**
     * @return the models_path
     */
    @JsonIgnore
    public String getModelsPath() {
        return ((customPaths == null) ? null : customPaths.get(Constants.KEY_MODELS_PATH));
    }

    /**
     * @return the score_path
     */
    @JsonIgnore
    public String getScorePath() {
        return ((customPaths == null) ? null : customPaths.get(Constants.KEY_SCORE_PATH));
    }

    /**
     * @return the performance_path
     */
    @JsonIgnore
    public String getPerformancePath() {
        return ((customPaths == null) ? null : customPaths.get(Constants.KEY_PERFORMANCE_PATH));
    }

    /**
     * @return the confusionMatrixPath
     */
    @JsonIgnore
    public String getConfusionMatrixPath() {
        return ((customPaths == null) ? null : customPaths.get(Constants.KEY_CONFUSION_MATRIX_PATH));
    }

    /**
     * @return
     * @throws IOException
     */
    @JsonIgnore
    public List<String> getScoreMetaColumns(ModelConfig modelConfig) throws IOException {
        String path = scoreMetaColumnNameFile;
        if (StringUtils.isNotBlank(scoreMetaColumnNameFile)
                && SourceType.HDFS.equals(dataSet.getSource())) {
            PathFinder pathFinder = new PathFinder(modelConfig);
            File file = new File(scoreMetaColumnNameFile);
            path = new Path(pathFinder.getEvalSetPath(this), file.getName()).toString();
        }

        return CommonUtils.readConfFileIntoList(path, dataSet.getSource(), dataSet.getHeaderDelimiter());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RawSourceData getDataSet() {
        return dataSet;
    }

    public void setDataSet(RawSourceData dataSet) {
        this.dataSet = dataSet;
    }

    public Integer getPerformanceBucketNum() {
        return performanceBucketNum;
    }

    public void setPerformanceBucketNum(Integer performanceBucketNum) {
        this.performanceBucketNum = performanceBucketNum;
    }

    public String getScoreMetaColumnNameFile() {
        return scoreMetaColumnNameFile;
    }

    public void setScoreMetaColumnNameFile(String scoreMetaColumnNameFile) {
        this.scoreMetaColumnNameFile = scoreMetaColumnNameFile;
    }

    public String getPerformanceScoreSelector() {
        return performanceScoreSelector;
    }

    public void setPerformanceScoreSelector(String performanceScoreSelector) {
        this.performanceScoreSelector = performanceScoreSelector;
    }

    public Map<String, String> getCustomPaths() {
        return customPaths;
    }

    public void setCustomPaths(Map<String, String> customPaths) {
        this.customPaths = customPaths;
    }

}
