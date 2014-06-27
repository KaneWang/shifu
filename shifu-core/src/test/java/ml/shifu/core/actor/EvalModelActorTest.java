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
package ml.shifu.core.actor;

import akka.actor.*;
import ml.shifu.core.container.obj.ColumnConfig;
import ml.shifu.core.container.obj.EvalConfig;
import ml.shifu.core.container.obj.ModelConfig;
import ml.shifu.core.container.obj.RawSourceData.SourceType;
import ml.shifu.core.fs.ShifuFileUtils;
import ml.shifu.core.message.AkkaActorInputMessage;
import ml.shifu.core.util.CommonUtils;
import ml.shifu.core.util.Environment;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;


/**
 * EvalModelActorTest class
 */
public class EvalModelActorTest {

    private ModelConfig modelConfig;
    private List<ColumnConfig> columnConfigList;
    private EvalConfig evalConfig;

    private ActorSystem actorSystem;

    @BeforeClass
    public void setUp() throws IOException {
        modelConfig = CommonUtils.loadModelConfig("src/test/resources/unittest/ModelSets/full/ModelConfig.json", SourceType.LOCAL);
        columnConfigList = CommonUtils.loadColumnConfigList("src/test/resources/unittest/ModelSets/full/ColumnConfig.json", SourceType.LOCAL);
        evalConfig = modelConfig.getEvalConfigByName("Eval1");
        actorSystem = ActorSystem.create("shifuActorSystem");
    }

    //@Test
    public void testActor() throws IOException, InterruptedException {
        Environment.setProperty(Environment.SHIFU_HOME, ".");

        File tmpModels = new File("models");
        File tmpCommon = new File("common");

        File models = new File("src/test/resources/unittest/ModelSets/full/models");
        FileUtils.copyDirectory(models, tmpModels);

        File tmpEval1 = new File("evals");
        tmpEval1.mkdir();

        ActorRef modelEvalRef = actorSystem.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = -1437127862571741369L;

            public UntypedActor create() {
                return new EvalModelActor(modelConfig, columnConfigList, new AkkaExecStatus(true), evalConfig);
            }
        }), "model-evaluator");

        List<Scanner> scanners = ShifuFileUtils.getDataScanners("src/test/resources/unittest/DataSet/wdbc.eval", SourceType.LOCAL);
        modelEvalRef.tell(new AkkaActorInputMessage(scanners), modelEvalRef);

        while (!modelEvalRef.isTerminated()) {
            Thread.sleep(5000);
        }


        File outputFile = new File("evals/Eval1/EvalScore");
        Assert.assertTrue(outputFile.exists());

        FileUtils.deleteDirectory(tmpModels);
        FileUtils.deleteDirectory(tmpCommon);
        FileUtils.deleteDirectory(tmpEval1);
    }

}
