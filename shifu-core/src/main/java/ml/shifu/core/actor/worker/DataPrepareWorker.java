/**
 * Copyright [2012-2013] eBay Software Foundation
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
package ml.shifu.core.actor.worker;

import akka.actor.ActorRef;
import ml.shifu.core.container.CaseScoreResult;
import ml.shifu.core.container.ColumnScoreObject;
import ml.shifu.core.container.RawValueObject;
import ml.shifu.core.container.obj.ColumnConfig;
import ml.shifu.core.container.obj.ModelConfig;
import ml.shifu.core.exception.ShifuErrorCode;
import ml.shifu.core.exception.ShifuException;
import ml.shifu.core.message.ColumnScoreMessage;
import ml.shifu.core.message.RunModelResultMessage;
import ml.shifu.core.message.StatsPartRawDataMessage;
import ml.shifu.core.message.StatsValueObjectMessage;
import ml.shifu.core.util.CommonUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;


/**
 * DataPrepareWorker class convert data into all kinds of format.
 * StatsPartRawDataMessage - convert row-based data into column-based training data for calculating stats
 * RunModelResultMessage - convert model-result from row-based to column-based
 * NormPartRawDataMessage - filter data for normalization
 */
public class DataPrepareWorker extends AbstractWorkerActor {

    private static Logger log = LoggerFactory.getLogger(DataPrepareWorker.class);
    private Random random = new Random(System.currentTimeMillis());
    private Map<Integer, ActorRef> columnNumToActorMap;
    private String[] trainDataHeader;

    private int weightedColumnNum = -1;

    /**
     * @param modelConfig
     * @param columnConfigList
     * @param parentActorRef
     * @param columnNumToActorMap
     * @throws IOException
     */
    public DataPrepareWorker(
            ModelConfig modelConfig,
            List<ColumnConfig> columnConfigList,
            ActorRef parentActorRef,
            ActorRef nextActorRef) throws IOException {
        super(modelConfig, columnConfigList, parentActorRef, nextActorRef);
        trainDataHeader = CommonUtils.getHeaders(modelConfig.getHeaderPath(),
                modelConfig.getHeaderDelimiter(),
                modelConfig.getDataSet().getSource());
    }

    /**
     * @param modelConfig
     * @param columnConfigList
     * @param parentActorRef
     * @param columnNumToActorMap
     * @throws IOException
     */
    public DataPrepareWorker(
            ModelConfig modelConfig,
            List<ColumnConfig> columnConfigList,
            ActorRef parentActorRef,
            Map<Integer, ActorRef> columnNumToActorMap) throws IOException {
        this(modelConfig, columnConfigList, parentActorRef, (ActorRef) null);
        this.columnNumToActorMap = columnNumToActorMap;

        if (!StringUtils.isEmpty(this.modelConfig.getDataSet().getWeightColumnName())) {
            String weightColumnName = this.modelConfig.getDataSet().getWeightColumnName();

            for (int i = 0; i < this.columnConfigList.size(); i++) {
                ColumnConfig config = this.columnConfigList.get(i);
                if (config.getColumnName().equals(weightColumnName)) {
                    this.weightedColumnNum = i;
                    break;
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see akka.actor.UntypedActor#onReceive(java.lang.Object)
     */
    @Override
    public void handleMsg(Object message) {
        if (message instanceof StatsPartRawDataMessage) {
            StatsPartRawDataMessage partData = (StatsPartRawDataMessage) message;
            Map<Integer, List<RawValueObject>> columnVoListMap = buildColumnVoListMap(partData.getRawDataList().size());
            DataPrepareStatsResult rt = convertRawDataIntoValueObject(partData.getRawDataList(), columnVoListMap);
            int totalMsgCnt = partData.getTotalMsgCnt();

            for (Integer columnNum : columnVoListMap.keySet()) {
                columnNumToActorMap.get(columnNum)
                        .tell(new StatsValueObjectMessage(totalMsgCnt, columnNum, columnVoListMap.get(columnNum), rt.getMissingMap().containsKey(columnNum) ? rt.getMissingMap().get(columnNum) : 0, rt.getTotal()), getSelf());
            }
        } else if (message instanceof RunModelResultMessage) {
            RunModelResultMessage msg = (RunModelResultMessage) message;
            Map<Integer, List<ColumnScoreObject>> columnScoreListMap = buildColumnScoreListMap();
            convertModelResultIntoColScore(msg.getScoreResultList(), columnScoreListMap);
            int totalMsgCnt = msg.getTotalStreamCnt();

            for (Integer columnNum : columnScoreListMap.keySet()) {
                columnNumToActorMap.get(columnNum)
                        .tell(new ColumnScoreMessage(totalMsgCnt, columnNum, columnScoreListMap.get(columnNum)), getSelf());
            }
        } else {
            unhandled(message);
        }

    }

    /**
     * Create the Map<ColumnID, List<ValueObject>> to prepare the data for calculating stats of each column
     * If the input message doesn't contain any data, the actor won't send message into next-actor who is waiting the message.
     * Under this situation, it will cause AKKA to wait infinitely.
     * <p/>
     * This bug is report by Huang, Jianshi - https://github.paypal.com/xinzhong/core/issues/5
     *
     * @return initialed map for final candidate columns
     */
    private Map<Integer, List<RawValueObject>> buildColumnVoListMap(int capacity) {
        Map<Integer, List<RawValueObject>> columnVoListMap = new HashMap<Integer, List<RawValueObject>>();
        for (ColumnConfig columnConfig : columnConfigList) {
            if (columnConfig.isCandidate()) {
                columnVoListMap.put(columnConfig.getColumnNum(), new ArrayList<RawValueObject>(capacity));
            }
        }
        return columnVoListMap;
    }

    /**
     * Create the Map<ColumnID, List<ColumnScore>> to prepare the data for calculating average score of each column
     * If the input message doesn't contain any data, the actor won't send message into next-actor who is waiting the message.
     * Under this situation, it will cause AKKA to wait infinitely.
     * <p/>
     * This bug is report by Huang, Jianshi - https://github.paypal.com/xinzhong/core/issues/5
     *
     * @return initialed map for final select columns
     */
    private Map<Integer, List<ColumnScoreObject>> buildColumnScoreListMap() {
        Map<Integer, List<ColumnScoreObject>> columnScoreListMap = new HashMap<Integer, List<ColumnScoreObject>>();
        for (ColumnConfig columnConfig : columnConfigList) {
            if (columnConfig.isCandidate() && columnConfig.isFinalSelect()) {
                columnScoreListMap.put(columnConfig.getColumnNum(), new ArrayList<ColumnScoreObject>());
            }
        }
        return columnScoreListMap;
    }

    /**
     * Convert raw data into @ValueObject for calculating stats
     *
     * @param rawDataList     - raw data for training
     * @param columnVoListMap <column-id --> @ValueObject list>
     * @throws ShifuException if the data field length is not equal header length
     */
    private DataPrepareStatsResult convertRawDataIntoValueObject(List<String> rawDataList, Map<Integer, List<RawValueObject>> columnVoListMap) throws ShifuException {
        double sampleRate = modelConfig.getBinningSampleRate();

        long total = 0l;
        Map<Integer, Long> missingMap = new HashMap<Integer, Long>();


        for (String line : rawDataList) {

            total++;

            String[] raw = CommonUtils.split(line, modelConfig.getDataSetDelimiter());

            if (raw.length != columnConfigList.size()) {
                log.error("Expected Columns: " + columnConfigList.size() + ", but got: " + raw.length);
                throw new ShifuException(ShifuErrorCode.ERROR_NO_EQUAL_COLCONFIG);
            }

            String tag = raw[targetColumnNum];

            if (modelConfig.isBinningSampleNegOnly()) {
                if (modelConfig.getNegTags().contains(tag) && random.nextDouble() > sampleRate) {
                    continue;
                }
            } else {
                if (random.nextDouble() > sampleRate) {
                    continue;
                }
            }

            for (int i = 0; i < raw.length; i++) {
                if (!columnNumToActorMap.containsKey(i)) {
                    // ignore non-used columns
                    continue;
                }

                //ValueObject vo = new ValueObject();


                if (i >= columnConfigList.size()) {
                    log.error("The input size is longer than expected, need to check your data");
                    continue;
                }

                RawValueObject rvo = new RawValueObject();
                rvo.setValue(raw[i].trim());

                ColumnConfig config = columnConfigList.get(i);
                /*
                if (config.isNumerical()) { // NUMERICAL
					try {
						vo.setValue(Double.valueOf(raw[i].trim()));
						vo.setRaw(null);
						
					} catch (Exception e) {
						log.debug("Column " + config.getColumnNum() + ": " + config.getColumnName() + " is expected to be NUMERICAL, however received: " + raw[i]);
						incMap(i, missingMap);
						continue;
					}
				} else if (config.isCategorical()) { // CATEGORICAL
					if(StringUtils.isEmpty(raw[i])) {
						incMap(i, missingMap);
					}
					vo.setRaw(raw[i].trim());
					vo.setValue(null);
				} else { // AUTO TYPE
					try {
						vo.setValue(Double.valueOf(raw[i]));
						vo.setRaw(null);
					} catch (Exception e) {
						incMap(i, missingMap);
						vo.setRaw(raw[i]);
						vo.setValue(null);
					}
				}              */

                if (this.weightedColumnNum != -1) {
                    try {
                        rvo.setWeight(Double.valueOf(raw[weightedColumnNum]));
                    } catch (NumberFormatException e) {
                        rvo.setWeight(1.0);
                    }
					/*
					if( i == weightedColumnNum) {
						//weight and its column, set to 1
						rvo.setWeight(1.0);
					} else {
						rvo.setWeight(1.0);
					}              */
                }

                rvo.setTag(raw[targetColumnNum]);

                List<RawValueObject> voList = columnVoListMap.get(i);
                if (voList == null) {
                    voList = new ArrayList<RawValueObject>();
                    columnVoListMap.put(i, voList);
                }

                voList.add(rvo);
            }
        }

        DataPrepareStatsResult rt = new DataPrepareStatsResult(total, missingMap);

        return rt;
    }

    private void incMap(int index, Map<Integer, Long> mapping) {

        Long count = mapping.get(index);
        if (count == null) {
            mapping.put(index, Long.valueOf(1));
        } else {
            mapping.put(index, count + 1);
        }
    }

    public class DataPrepareStatsResult {

        public DataPrepareStatsResult(long total, Map<Integer, Long> missingMap) {
            this.total = total;
            this.missingMap = missingMap;
        }

        private long total;

        private Map<Integer, Long> missingMap;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public Map<Integer, Long> getMissingMap() {
            return missingMap;
        }

        public void setMissingMap(Map<Integer, Long> missingMap) {
            this.missingMap = missingMap;
        }
    }

    /**
     * Convert model result data into column-based
     */
    private void convertModelResultIntoColScore(List<CaseScoreResult> scoreResultList, Map<Integer, List<ColumnScoreObject>> columnScoreListMap) {
        for (CaseScoreResult scoreResult : scoreResultList) {
            Map<String, String> rawDataMap = CommonUtils.convertDataIntoMap(
                    scoreResult.getInputData(),
                    super.modelConfig.getDataSetDelimiter(),
                    this.trainDataHeader);

            for (ColumnConfig config : columnConfigList) {
                if (config.isFinalSelect()) {
                    ColumnScoreObject columnScore = new ColumnScoreObject(config.getColumnNum(), rawDataMap.get(config.getColumnName()));
                    columnScore.setScores(scoreResult.getScores());
                    columnScore.setMaxScore(scoreResult.getMaxScore());
                    columnScore.setMinScore(scoreResult.getMinScore());
                    columnScore.setAvgScore(scoreResult.getAvgScore());

                    List<ColumnScoreObject> csList = columnScoreListMap.get(config.getColumnNum());
                    if (csList == null) {
                        csList = new ArrayList<ColumnScoreObject>();
                        columnScoreListMap.put(config.getColumnNum(), csList);
                    }

                    csList.add(columnScore);
                }
            }
        }
    }

}
