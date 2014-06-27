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
package ml.shifu.core.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.routing.RoundRobinRouter;
import ml.shifu.core.actor.worker.DataLoadWorker;
import ml.shifu.core.actor.worker.TrainDataPrepWorker;
import ml.shifu.core.actor.worker.TrainModelWorker;
import ml.shifu.core.container.obj.ColumnConfig;
import ml.shifu.core.container.obj.ModelConfig;
import ml.shifu.core.core.AbstractTrainer;
import ml.shifu.core.message.AkkaActorInputMessage;
import ml.shifu.core.message.ExceptionMessage;
import ml.shifu.core.message.ScanStatsRawDataMessage;
import ml.shifu.core.message.TrainResultMessage;
import ml.shifu.core.util.Environment;
import org.encog.Encog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;


/**
 * TrainDtModelActor class
 */
public class TrainDtModelActor extends AbstractActor {

    private static Logger log = LoggerFactory.getLogger(CalculateStatsActor.class);

    private ActorRef dataLoadRef;
    private ActorRef trainDataPrepRef;
    private ActorRef trainModelRef;

    private int trainerCnt;
    private int resultCnt;

    /**
     * @param modelConfig
     * @param columnConfigList
     * @param akkaStatus
     */
    public TrainDtModelActor(final ModelConfig modelConfig,
                             final List<ColumnConfig> columnConfigList, AkkaExecStatus akkaStatus, final List<AbstractTrainer> trainers) {
        super(modelConfig, columnConfigList, akkaStatus);

        log.info("Creating Master Actor ...");

        this.resultCnt = 0;
        this.trainerCnt = trainers.size();

        final ActorRef parentActorRef = getSelf();

        // actors to training models
        trainModelRef = this.getContext().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = -5719806635080547488L;

            public UntypedActor create() {
                return new TrainModelWorker(modelConfig, columnConfigList, parentActorRef, parentActorRef);
            }
        }).withRouter(new RoundRobinRouter(this.modelConfig.getBaggingNum())), "ModelTrainWorker");

        // actors to aggregate all training data
        trainDataPrepRef = this.getContext().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = -5719806635080547488L;

            public UntypedActor create() throws IOException {
                return new TrainDataPrepWorker(modelConfig, columnConfigList, parentActorRef, trainModelRef, trainers);
            }
        }).withRouter(new RoundRobinRouter(1)), "DataPrepWorker");

        // actors to load data
        dataLoadRef = this.getContext().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = -6869659846227133318L;

            public UntypedActor create() {
                return new DataLoadWorker(modelConfig, columnConfigList, parentActorRef, trainDataPrepRef);
            }
        }).withRouter(new RoundRobinRouter(Environment.getInt(Environment.LOCAL_NUM_PARALLEL, 16))), "DataLoaderWorker");
    }

    /* (non-Javadoc)
     * @see akka.actor.UntypedActor#onReceive(java.lang.Object)
     */
    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof AkkaActorInputMessage) {
            resultCnt = 0;

            AkkaActorInputMessage msg = (AkkaActorInputMessage) message;
            List<Scanner> scanners = msg.getScanners();

            log.debug("Num of Scanners: " + scanners.size());

            for (Scanner scanner : scanners) {
                dataLoadRef.tell(
                        new ScanStatsRawDataMessage(scanners.size(), scanner), getSelf());
            }
        } else if (message instanceof TrainResultMessage) {
            resultCnt++;
            if (resultCnt == trainerCnt) {
                log.info("Received " + resultCnt + " finish message. Close System.");
                Encog.getInstance().shutdown();
                getContext().system().shutdown();
            }
        } else if (message instanceof ExceptionMessage) {
            // since some children actors meet some exception, shutdown the system
            ExceptionMessage msg = (ExceptionMessage) message;
            getContext().system().shutdown();

            // and wrapper the exception into Return status
            addExceptionIntoCondition(msg.getException());
        } else {
            unhandled(message);
        }
    }

}
