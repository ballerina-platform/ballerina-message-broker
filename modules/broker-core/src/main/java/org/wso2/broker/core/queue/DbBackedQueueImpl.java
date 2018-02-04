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

package org.wso2.broker.core.queue;

import org.wso2.broker.core.BrokerException;
import org.wso2.broker.core.Message;
import org.wso2.broker.core.Metadata;
import org.wso2.broker.core.store.dao.SharedMessageStore;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Database backed queue implementation.
 */
public class DbBackedQueueImpl extends Queue {

    private final java.util.Queue<Message> memQueue;

    private final SharedMessageStore sharedMessageStore;

    public DbBackedQueueImpl(String queueName, boolean autoDelete,
                      SharedMessageStore sharedMessageStore) throws BrokerException {
        super(queueName, true, autoDelete);
        this.sharedMessageStore = sharedMessageStore;
        this.memQueue = new LinkedBlockingQueue<>();
        Collection<Message> messages = sharedMessageStore.readStoredMessages(queueName);
        memQueue.addAll(messages);
    }

    @Override
    public int capacity() {
        return Queue.UNBOUNDED;
    }

    @Override
    public int size() {
        return memQueue.size();
    }

    @Override
    public boolean enqueue(Message message) throws BrokerException {
        if (message.getMetadata().getByteProperty(Metadata.DELIVERY_MODE) == Metadata.PERSISTENT_MESSAGE) {
            sharedMessageStore.attach(getName(), message.getMetadata().getInternalId());
        }
        return memQueue.offer(message);
    }

    @Override
    public Message dequeue() {
        return memQueue.poll();
    }

    @Override
    public void detach(Message message) {
        sharedMessageStore.detach(getName(), message);
    }
}
