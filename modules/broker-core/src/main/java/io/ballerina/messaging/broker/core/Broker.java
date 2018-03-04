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

package io.ballerina.messaging.broker.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.ballerina.messaging.broker.auth.AuthManager;
import io.ballerina.messaging.broker.auth.authorization.enums.ResourceActions;
import io.ballerina.messaging.broker.auth.authorization.enums.ResourceAuthScopes;
import io.ballerina.messaging.broker.auth.authorization.enums.ResourceTypes;
import io.ballerina.messaging.broker.auth.authorization.handler.AuthorizationHandler;
import io.ballerina.messaging.broker.auth.exception.BrokerAuthException;
import io.ballerina.messaging.broker.common.ResourceNotFoundException;
import io.ballerina.messaging.broker.common.StartupContext;
import io.ballerina.messaging.broker.common.ValidationException;
import io.ballerina.messaging.broker.common.config.BrokerCommonConfiguration;
import io.ballerina.messaging.broker.common.config.BrokerConfigProvider;
import io.ballerina.messaging.broker.common.data.types.FieldTable;
import io.ballerina.messaging.broker.common.util.function.ThrowingRunnable;
import io.ballerina.messaging.broker.coordination.BasicHaListener;
import io.ballerina.messaging.broker.coordination.HaListener;
import io.ballerina.messaging.broker.coordination.HaStrategy;
import io.ballerina.messaging.broker.core.configuration.BrokerCoreConfiguration;
import io.ballerina.messaging.broker.core.metrics.BrokerMetricManager;
import io.ballerina.messaging.broker.core.metrics.DefaultBrokerMetricManager;
import io.ballerina.messaging.broker.core.metrics.NullBrokerMetricManager;
import io.ballerina.messaging.broker.core.rest.api.ExchangesApi;
import io.ballerina.messaging.broker.core.rest.api.QueuesApi;
import io.ballerina.messaging.broker.core.store.DbBackedStoreFactory;
import io.ballerina.messaging.broker.core.store.MemBackedStoreFactory;
import io.ballerina.messaging.broker.core.store.MessageStore;
import io.ballerina.messaging.broker.core.store.StoreFactory;
import io.ballerina.messaging.broker.core.task.TaskExecutorService;
import io.ballerina.messaging.broker.core.transaction.BranchFactory;
import io.ballerina.messaging.broker.core.transaction.BrokerTransactionFactory;
import io.ballerina.messaging.broker.core.transaction.LocalTransaction;
import io.ballerina.messaging.broker.core.util.MessageTracer;
import io.ballerina.messaging.broker.rest.BrokerServiceRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.metrics.core.MetricService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.sql.DataSource;
import javax.transaction.xa.Xid;

/**
 * Broker API class.
 */
public final class Broker {

    private static final Logger LOGGER = LoggerFactory.getLogger(Broker.class);

    /**
     * Internal queue used to put unprocessable messages.
     */
    public static final String DEFAULT_DEAD_LETTER_QUEUE = "amq.dlq";

    /**
     * Generated header names when putting a file to dead letter queue.
     */
    public static final String ORIGIN_QUEUE_HEADER = "x-origin-queue";
    public static final String ORIGIN_EXCHANGE_HEADER = "x-origin-exchange";
    public static final String ORIGIN_ROUTING_KEY_HEADER = "x-origin-routing-key";

    /**
     * Used to manage metrics related to broker
     */
    private final BrokerMetricManager metricManager;

    /**
     * The {@link HaStrategy} for which the HA listener is registered.
     */
    private HaStrategy haStrategy;

    private BrokerHelper brokerHelper;

    private final BrokerTransactionFactory brokerTransactionFactory;

    private final QueueRegistry queueRegistry;

    private final TaskExecutorService<MessageDeliveryTask> deliveryTaskService;

    private final ExchangeRegistry exchangeRegistry;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final MessageStore messageStore;

    /**
     * In memory message id.
     */
    private final MessageIdGenerator messageIdGenerator;

    /**
     * The @{@link AuthorizationHandler} to handle authorization.
     */
    private final AuthorizationHandler authHandler;

    public Broker(StartupContext startupContext) throws Exception {
        MetricService metrics = startupContext.getService(MetricService.class);
        metricManager = getMetricManager(metrics);
        authHandler = new AuthorizationHandler(startupContext.getService(AuthManager.class));

        BrokerConfigProvider configProvider = startupContext.getService(BrokerConfigProvider.class);
        BrokerCoreConfiguration configuration = configProvider.getConfigurationObject(BrokerCoreConfiguration.NAMESPACE,
                                                                                      BrokerCoreConfiguration.class);
        StoreFactory storeFactory = getStoreFactory(startupContext, configProvider, configuration);

        exchangeRegistry = storeFactory.getExchangeRegistry();
        messageStore = storeFactory.getMessageStore();
        queueRegistry = storeFactory.getQueueRegistry();
        exchangeRegistry.retrieveFromStore(queueRegistry);

        this.deliveryTaskService = createTaskExecutorService(configuration);
        messageIdGenerator = new MessageIdGenerator();

        initDefaultDeadLetterQueue();

        this.brokerTransactionFactory = new BrokerTransactionFactory(new BranchFactory(this, storeFactory));

        initRestApi(startupContext);
        initHaSupport(startupContext);

        startupContext.registerService(Broker.class, this);
    }

    private StoreFactory getStoreFactory(StartupContext startupContext,
                                         BrokerConfigProvider configProvider,
                                         BrokerCoreConfiguration configuration) throws Exception {
        BrokerCommonConfiguration commonConfigs
                = configProvider.getConfigurationObject(BrokerCommonConfiguration.NAMESPACE,
                                                        BrokerCommonConfiguration.class);
        if (Objects.isNull(commonConfigs)) {
            commonConfigs = new BrokerCommonConfiguration();
        }
        DataSource dataSource = startupContext.getService(DataSource.class);

        if (commonConfigs.getEnableInMemoryMode()) {
            return new MemBackedStoreFactory(metricManager, configuration);
        } else {
            return new DbBackedStoreFactory(dataSource, metricManager, configuration);
        }
    }

    private void initRestApi(StartupContext startupContext) {
        BrokerServiceRunner serviceRunner = startupContext.getService(BrokerServiceRunner.class);
        if (Objects.nonNull(serviceRunner)) {
            serviceRunner.deploy(new QueuesApi(this), new ExchangesApi(this));
        }
    }

    private void initHaSupport(StartupContext startupContext) {
        haStrategy = startupContext.getService(HaStrategy.class);
        if (haStrategy == null) {
            brokerHelper = new BrokerHelper();
        } else {
            LOGGER.info("Broker is in PASSIVE mode"); //starts up in passive mode
            brokerHelper = new HaEnabledBrokerHelper();
        }
    }

    private void initDefaultDeadLetterQueue() throws BrokerException, ValidationException {
        try {
            createQueue(DEFAULT_DEAD_LETTER_QUEUE, false, true, false, () -> {
            }, () -> {
            });
            bind(DEFAULT_DEAD_LETTER_QUEUE,
                 ExchangeRegistry.DEFAULT_DEAD_LETTER_EXCHANGE,
                 DEFAULT_DEAD_LETTER_QUEUE,
                 FieldTable.EMPTY_TABLE);
        } catch (BrokerAuthException e) {
            throw new BrokerException("Auth error while initializing dead letter queue", e);
        }
    }

    private BrokerMetricManager getMetricManager(MetricService metrics) {
        if (Objects.nonNull(metrics)) {
            return new DefaultBrokerMetricManager(metrics);
        } else {
            return new NullBrokerMetricManager();
        }
    }

    private TaskExecutorService<MessageDeliveryTask> createTaskExecutorService(BrokerCoreConfiguration configuration) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("MessageDeliveryTaskThreadPool-%d")
                                                                .build();
        int workerCount = Integer.parseInt(configuration.getDeliveryTask().getWorkerCount());
        int idleTaskDelay = Integer.parseInt(configuration.getDeliveryTask().getIdleTaskDelay());
        return new TaskExecutorService<>(workerCount, idleTaskDelay, threadFactory);
    }


    public void publish(Message message) throws BrokerException {
        lock.readLock().lock();
        try {
            Metadata metadata = message.getMetadata();
            Exchange exchange = exchangeRegistry.getExchange(metadata.getExchangeName());
            if (exchange != null) {
                String routingKey = metadata.getRoutingKey();
                BindingSet bindingSet = exchange.getBindingsForRoute(routingKey);

                if (bindingSet.isEmpty()) {
                    LOGGER.info("Dropping message since no queues found for routing key {} in {}",
                                routingKey, exchange);
                    MessageTracer.trace(message, MessageTracer.NO_ROUTES);
                } else {
                    try {
                        messageStore.add(message.shallowCopy());
                        Set<QueueHandler> uniqueQueues = getUniqueQueueHandlersForBinding(metadata, bindingSet);
                        publishToQueues(message, uniqueQueues);
                    } finally {
                        messageStore.flush(message.getInternalId());
                    }
                }
            } else {
                MessageTracer.trace(message, MessageTracer.UNKNOWN_EXCHANGE);
                throw new BrokerException("Message publish failed. Unknown exchange: " + metadata.getExchangeName());
            }
        } finally {
            lock.readLock().unlock();
            // Release the original message. Shallow copies are distributed
            message.release();
        }

    }

    private Set<QueueHandler> getUniqueQueueHandlersForBinding(Metadata metadata, BindingSet bindingSet) {
        Set<QueueHandler> uniqueQueues = new HashSet<>();
        for (Binding binding : bindingSet.getUnfilteredBindings()) {
            uniqueQueues.add(binding.getQueue().getQueueHandler());
        }

        for (Binding binding : bindingSet.getFilteredBindings()) {
            if (binding.getFilterExpression().evaluate(metadata)) {
                uniqueQueues.add(binding.getQueue().getQueueHandler());
            }
        }
        return uniqueQueues;
    }

    private void publishToQueues(Message message, Set<QueueHandler> uniqueQueueHandlers) throws BrokerException {
        // Unique queues can be empty due to un-matching selectors.
        if (uniqueQueueHandlers.isEmpty()) {
            LOGGER.info("Dropping message since message didn't have any routes to {}",
                        message.getMetadata().getRoutingKey());
            MessageTracer.trace(message, MessageTracer.NO_ROUTES);
            return;
        }

        for (QueueHandler handler : uniqueQueueHandlers) {
            handler.enqueue(message.shallowCopy());
        }
        metricManager.markPublish();
    }

    /**
     * Acknowledge single or a given set of messages. Removes the message from underlying queue
     * @param queueName   name of the queue the relevant messages belongs to
     * @param message delivery tag of the message sent by the broker
     */
    public void acknowledge(String queueName, Message message) throws BrokerException {
        lock.readLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
            queueHandler.dequeue(message);
            metricManager.markAcknowledge();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<QueueHandler> prepareEnqueue(Xid xid, Message message) throws BrokerException {
        lock.readLock().lock();
        try {
            Metadata metadata = message.getMetadata();
            Exchange exchange = exchangeRegistry.getExchange(metadata.getExchangeName());
            if (Objects.nonNull(exchange)) {
                BindingSet bindingsForRoute = exchange.getBindingsForRoute(metadata.getRoutingKey());
                Set<QueueHandler> uniqueQueueHandlers = getUniqueQueueHandlersForBinding(metadata, bindingsForRoute);
                if (uniqueQueueHandlers.isEmpty()) {
                    return uniqueQueueHandlers;
                }
                messageStore.add(xid, message.shallowCopy());
                for (QueueHandler handler : uniqueQueueHandlers) {
                    handler.prepareForEnqueue(xid, message.shallowCopy());
                }
                return uniqueQueueHandlers;
            } else {
                throw new BrokerException("Message published to unknown exchange " + metadata.getExchangeName());
            }
        } finally {
            lock.readLock().unlock();
            message.release();
        }
    }

    public QueueHandler prepareDequeue(Xid xid, String queueName, Message message) throws BrokerException {
        lock.readLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
            queueHandler.prepareForDetach(xid, message);
            return queueHandler;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Adds a consumer for a queue. Queue will be the queue returned from {@link Consumer#getQueueName()}
     *
     * @param consumer {@link Consumer} implementation
     * @throws BrokerException throws {@link BrokerException} if unable to add the consumer
     */
    public void addConsumer(Consumer consumer) throws BrokerException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Consume request received for {}", consumer.getQueueName());
        }

        lock.readLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(consumer.getQueueName());
            if (queueHandler != null) {
                synchronized (queueHandler) {
                    if (queueHandler.addConsumer(consumer) && queueHandler.consumerCount() == 1) {
                        deliveryTaskService.add(new MessageDeliveryTask(queueHandler));
                    }
                }
            } else {
                throw new BrokerException("Cannot add consumer. Queue [ " + consumer.getQueueName() + " ] "
                                                  + "not found. Create the queue before attempting to consume.");
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void removeConsumer(Consumer consumer) {
        lock.readLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(consumer.getQueueName());
            if (queueHandler != null) {
                synchronized (queueHandler) {
                    if (queueHandler.removeConsumer(consumer) && queueHandler.consumerCount() == 0) {
                        deliveryTaskService.remove(queueHandler.getQueue().getName());
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void declareExchange(String exchangeName, String type,
                                boolean passive, boolean durable) throws BrokerException, ValidationException {
        lock.writeLock().lock();
        try {
            exchangeRegistry.declareExchange(exchangeName, type, passive, durable);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void createExchange(String exchangeName, String type, boolean durable) throws BrokerException,
                                                                                         ValidationException {
        lock.writeLock().lock();
        try {
            exchangeRegistry.createExchange(exchangeName, Exchange.Type.from(type), durable);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean deleteExchange(String exchangeName, boolean ifUnused) throws BrokerException, ValidationException {
        lock.writeLock().lock();
        try {
            return exchangeRegistry.deleteExchange(exchangeName, ifUnused);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean createQueue(String queueName, boolean passive, boolean durable, boolean autoDelete)
            throws BrokerException, ValidationException, BrokerAuthException {
        return createQueue(queueName, passive, durable, autoDelete,
                           () -> authHandler.handle(ResourceAuthScopes.QUEUES_CREATE),
                           () -> authHandler.createAuthResource(ResourceTypes.QUEUE, queueName, durable));
    }

    private boolean createQueue(String queueName, boolean passive, boolean durable, boolean autoDelete,
                                ThrowingRunnable<BrokerAuthException> queueAuthorizer,
                                ThrowingRunnable<BrokerAuthException> authResourceCreator)
            throws BrokerException, BrokerAuthException, ValidationException {
        lock.writeLock().lock();
        try {
            queueAuthorizer.run();
            boolean queueAdded = queueRegistry.addQueue(queueName, passive, durable, autoDelete);
            if (queueAdded) {
                authResourceCreator.run();
                QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
                // We need to bind every queue to the default exchange
                exchangeRegistry.getDefaultExchange().bind(queueHandler, queueName, FieldTable.EMPTY_TABLE);
            }
            return queueAdded;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int deleteQueue(String queueName, boolean ifUnused, boolean ifEmpty)
            throws BrokerException, ValidationException, ResourceNotFoundException, BrokerAuthException {
        lock.writeLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
            if (Objects.nonNull(queueHandler) && Objects.nonNull(queueHandler.getQueue())) {
                authHandler.handle(ResourceTypes.QUEUE, queueName, ResourceActions.DELETE);
            }
            int messageCount = queueRegistry.removeQueue(queueName, ifUnused, ifEmpty);
            authHandler.deleteAuthResource(ResourceTypes.QUEUE, queueName);
            return messageCount;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void bind(String queueName, String exchangeName,
                     String routingKey, FieldTable arguments) throws BrokerException, ValidationException {
        lock.writeLock().lock();
        try {
            Exchange exchange = exchangeRegistry.getExchange(exchangeName);
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
            if (exchange == null) {
                throw new ValidationException("Unknown exchange name: " + exchangeName);
            }

            if (queueHandler == null) {
                throw new ValidationException("Unknown queue name: " + queueName);
            }

            if (!routingKey.isEmpty()) {
                exchange.bind(queueHandler, routingKey, arguments);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unbind(String queueName, String exchangeName, String routingKey)
            throws BrokerException, ValidationException {
        lock.writeLock().lock();
        try {
            Exchange exchange = exchangeRegistry.getExchange(exchangeName);
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);

            if (exchange == null) {
                throw new ValidationException("Unknown exchange name: " + exchangeName);
            }

            if (queueHandler == null) {
                throw new ValidationException("Unknown queue name: " + queueName);
            }

            exchange.unbind(queueHandler.getQueue(), routingKey);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void startMessageDelivery() {
        brokerHelper.startMessageDelivery();
    }

    public void stopMessageDelivery() {
        LOGGER.info("Stopping message delivery threads.");
        deliveryTaskService.stop();
    }

    public void shutdown() {
        brokerHelper.shutdown();
    }

    public long getNextMessageId() {
        return messageIdGenerator.getNextId();
    }

    public void requeue(String queueName, Message message) throws BrokerException {
        lock.readLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
            queueHandler.requeue(message);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Collection<QueueHandler> getAllQueues() {
        lock.readLock().lock();
        try {
            return queueRegistry.getAllQueues();
        } finally {
            lock.readLock().unlock();
        }
    }

    public QueueHandler getQueue(String queueName) {
        lock.readLock().lock();
        try {
            return queueRegistry.getQueueHandler(queueName);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void moveToDlc(String queueName, Message message) throws BrokerException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Moving message to DLC: {}", message);
        }
        try {
            Message dlcMessage = message.shallowCopyWith(getNextMessageId(),
                                                         DEFAULT_DEAD_LETTER_QUEUE,
                                                         ExchangeRegistry.DEFAULT_DEAD_LETTER_EXCHANGE);
            dlcMessage.getMetadata().addHeader(ORIGIN_QUEUE_HEADER, queueName);
            dlcMessage.getMetadata().addHeader(ORIGIN_EXCHANGE_HEADER, message.getMetadata().getExchangeName());
            dlcMessage.getMetadata().addHeader(ORIGIN_ROUTING_KEY_HEADER, message.getMetadata().getRoutingKey());

            publish(dlcMessage);
            acknowledge(queueName, message);
        } finally {
            message.release();
        }
    }

    public Collection<Exchange> getAllExchanges() {
        lock.readLock().lock();
        try {
            return exchangeRegistry.getAllExchanges();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, BindingSet> getAllBindingsForExchange(String exchangeName) throws ValidationException {
        lock.readLock().lock();

        try {
            Exchange exchange = exchangeRegistry.getExchange(exchangeName);
            if (Objects.isNull(exchange)) {
                throw new ValidationException("Non existing exchange name " + exchangeName);
            }

            return exchange.getBindingsRegistry().getAllBindings();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Exchange getExchange(String exchangeName) {
        lock.readLock().lock();
        try {
            return exchangeRegistry.getExchange(exchangeName);
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Start local transaction flow
     */
    public LocalTransaction newLocalTransaction() {
        return brokerTransactionFactory.createLocalTransaction();
    }

    private class BrokerHelper {

        public void startMessageDelivery() {
            LOGGER.info("Starting message delivery threads.");
            deliveryTaskService.start();
        }

        public void shutdown() {
            stopMessageDelivery();
        }

    }

    private class HaEnabledBrokerHelper extends BrokerHelper implements HaListener {

        private BasicHaListener basicHaListener;

        HaEnabledBrokerHelper() {
            basicHaListener = new BasicHaListener(this);
            haStrategy.registerListener(basicHaListener, 1);
        }

        @Override
        public synchronized void startMessageDelivery() {
            basicHaListener.setStartCalled(); //to allow starting when the node becomes active when HA is enabled
            if (!basicHaListener.isActive()) {
                return;
            }
            super.startMessageDelivery();
        }

        @Override
        public void shutdown() {
            haStrategy.unregisterListener(basicHaListener);
            super.shutdown();
        }

        /**
         * {@inheritDoc}
         */
        public void activate() {
            try {
                queueRegistry.reloadQueuesOnBecomingActive();
                exchangeRegistry.reloadExchangesOnBecomingActive(queueRegistry);
            } catch (BrokerException e) {
                LOGGER.error("Error on loading data from the database on becoming active ", e);
            }
            startMessageDeliveryOnBecomingActive();
            LOGGER.info("Broker mode changed from PASSIVE to ACTIVE");
        }

        /**
         * {@inheritDoc}
         */
        public void deactivate() {
            stopMessageDelivery();
            LOGGER.info("Broker mode changed from ACTIVE to PASSIVE");
        }

        /**
         * Method to start message delivery by the broker, only if startMessageDelivery()} has been called, prior to
         * becoming the active node.
         */
        private synchronized void startMessageDeliveryOnBecomingActive() {
            if (basicHaListener.isStartCalled()) {
                startMessageDelivery();
            }
        }

    }
}
