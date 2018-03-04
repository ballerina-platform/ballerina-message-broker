/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package io.ballerina.messaging.broker.core.configuration;

/**
 * Represents configuration for broker.
 */
public class BrokerCoreConfiguration {

    /**
     * Namespace used in config file
     */
    public static final String NAMESPACE = "ballerina.broker.core";
    /**
     * Name of the configuration file.
     */
    public static final String BROKER_FILE_NAME = "broker.yaml";
    
    /**
     * system property to specify the path of the broker configuration file.
     */
    public static final String SYSTEM_PARAM_BROKER_CONFIG_FILE = "broker.config";

    private String nonDurableQueueMaxDepth = "10000";

    private String durableQueueInMemoryCacheLimit = "10000";

    private DeliveryTask deliveryTask = new DeliveryTask();

    /**
     * Getter for nonDurableQueueMaxDepth
     */
    public String getNonDurableQueueMaxDepth() {
        return nonDurableQueueMaxDepth;
    }

    public void setNonDurableQueueMaxDepth(String nonDurableQueueMaxDepth) {
        this.nonDurableQueueMaxDepth = nonDurableQueueMaxDepth;
    }

    /**
     * Getter for durableQueueInMemoryCacheLimit.
     */
    public String getDurableQueueInMemoryCacheLimit() {
        return durableQueueInMemoryCacheLimit;
    }

    public void setDurableQueueInMemoryCacheLimit(String durableQueueInMemoryCacheLimit) {
        this.durableQueueInMemoryCacheLimit = durableQueueInMemoryCacheLimit;
    }

    /**
     * Getter for deliveryTask
     */
    public DeliveryTask getDeliveryTask() {
        return deliveryTask;
    }

    public void setDeliveryTask(DeliveryTask deliveryTask) {
        this.deliveryTask = deliveryTask;
    }

    /**
     * Represent delivery task related configurations.
     */
    public static class DeliveryTask {
        private String workerCount = "5";

        private String idleTaskDelay = "50";

        /**
         * Getter for workerCount
         */
        public String getWorkerCount() {
            return workerCount;
        }

        public void setWorkerCount(String workerCount) {
            this.workerCount = workerCount;
        }

        /**
         * Getter for idleTaskDelay
         */
        public String getIdleTaskDelay() {
            return idleTaskDelay;
        }

        public void setIdleTaskDelay(String idleTaskDelay) {
            this.idleTaskDelay = idleTaskDelay;
        }
    }
}
