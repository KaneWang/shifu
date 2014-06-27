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

package ml.shifu.core.di.module;


import com.google.inject.AbstractModule;
import ml.shifu.core.util.CommonUtils;

import java.util.HashMap;
import java.util.Map;


public class StatsModule extends AbstractModule {

    private Map<String, String> injections = new HashMap<String, String>();

    public StatsModule() {
    }


    public Map<String, String> getInjections() {
        return injections;
    }

    public void setInjections(Map<String, String> injections) {
        this.injections = injections;
    }


    @Override
    protected void configure() {

        for (String spiName : injections.keySet()) {
            Class spi = CommonUtils.getClass("ml.core.core.di.spi." + spiName);
            Class impl = CommonUtils.getClass(injections.get(spiName));
            bind(spi).to(impl);
        }

    }
}
