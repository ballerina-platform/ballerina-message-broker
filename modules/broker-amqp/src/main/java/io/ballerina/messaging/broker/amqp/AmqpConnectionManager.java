/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package io.ballerina.messaging.broker.amqp;

import io.ballerina.messaging.broker.amqp.codec.handlers.AmqpConnectionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AmqpConnectionManager stores the list of amqp connection established and manages those connections.
 */
public class AmqpConnectionManager {

    /**
     * List of {@link AmqpConnectionHandler} representing AMQP connections.
     */
    private List<AmqpConnectionHandler> connectionHandlers;

    AmqpConnectionManager() {
        connectionHandlers = new ArrayList<>();
    }

    /**
     * Adds a connection handler upon registration of an AMQP connection.
     *
     * @param handler {@link AmqpConnectionHandler} representing AMQP connections
     */
    public void addConnectionHandler(AmqpConnectionHandler handler) {
        synchronized (connectionHandlers) {
            connectionHandlers.add(handler);
        }
    }

    /**
     * Removes a connection handler upon closing of an AMQP connection.
     *
     * @param handler {@link AmqpConnectionHandler} representing AMQP connections
     */
    public void removeConnectionHandler(AmqpConnectionHandler handler) {
        synchronized (connectionHandlers) {
            connectionHandlers.remove(handler);
        }
    }

    /**
     * Retrieves the AMQP connections that are established.
     *
     * @return a list of {@link AmqpConnectionHandler} representing AMQP connections
     */
    public List<AmqpConnectionHandler> getConnections() {
        return Collections.unmodifiableList(connectionHandlers);
    }
}
