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

package io.ballerina.messaging.broker.core.store;

import io.ballerina.messaging.broker.core.BrokerException;
import io.ballerina.messaging.broker.core.DbBackedQueueHandlerFactory;
import io.ballerina.messaging.broker.core.ExchangeRegistry;
import io.ballerina.messaging.broker.core.ExchangeRegistryFactory;
import io.ballerina.messaging.broker.core.QueueRegistry;
import io.ballerina.messaging.broker.core.QueueRegistryFactory;
import io.ballerina.messaging.broker.core.configuration.BrokerCoreConfiguration;
import io.ballerina.messaging.broker.core.metrics.BrokerMetricManager;
import io.ballerina.messaging.broker.core.store.dao.impl.DaoFactory;
import io.ballerina.messaging.broker.eventing.EventSync;

import javax.sql.DataSource;

/**
 * Factory class for store backed objects.
 */
public class DbBackedStoreFactory implements StoreFactory {

    private final DaoFactory daoFactory;
    private final BrokerMetricManager metricManager;
    private final BrokerCoreConfiguration configuration;
    private final EventSync eventSync;

    private DbMessageStore dbMessageStore;

    public DbBackedStoreFactory(DataSource dataSource,
                                BrokerMetricManager metricManager,
                                BrokerCoreConfiguration configuration, EventSync eventSync) {
        daoFactory = new DaoFactory(dataSource, metricManager, configuration);
        this.metricManager = metricManager;
        this.configuration = configuration;
        this.eventSync = eventSync;
        int disruptorBufferSize = configuration.getDisruptorBufferSize();
        int maxDbBatchSize = configuration.getMaxDbWriteBatchSize();
        dbMessageStore = new DbMessageStore(daoFactory.createMessageDao(), disruptorBufferSize, maxDbBatchSize);
    }

    @Override
    public ExchangeRegistry getExchangeRegistry() {
        return new ExchangeRegistryFactory(daoFactory.createExchangeDao(),
                daoFactory.createBindingDao(),
                eventSync).getExchangeRegistry();
    }

    @Override
    public MessageStore getMessageStore() {
        return dbMessageStore;
    }

    @Override
    public QueueRegistry getQueueRegistry() throws BrokerException {
        return new QueueRegistryFactory(daoFactory.createQueueDao(),
                new DbBackedQueueHandlerFactory(dbMessageStore, metricManager, configuration, eventSync),
                eventSync).getQueueRegistry();
    }
}
