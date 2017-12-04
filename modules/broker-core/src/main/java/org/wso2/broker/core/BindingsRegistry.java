/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.broker.core;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the bindings for a given {@link Exchange}.
 */
final class BindingsRegistry {

    private final Map<String, Set<Binding>> routingKeyToBindingMap;

    BindingsRegistry() {
        this.routingKeyToBindingMap = new ConcurrentHashMap<>();
    }

    void bind(QueueHandler queueHandler, String routingKey) {
        Binding binding = new Binding(routingKey, queueHandler.getQueue().getName());
        Set<Binding> bindingList =
                routingKeyToBindingMap.computeIfAbsent(routingKey, k -> ConcurrentHashMap.newKeySet());
        bindingList.add(binding);
    }

    void unbind(String queueName, String routingKey) {
        Set<Binding> bindings = routingKeyToBindingMap.get(routingKey);
        Iterator<Binding> iterator = bindings.iterator();
        while (iterator.hasNext()) {
            Binding binding = iterator.next();
            if (queueName.compareTo(binding.getQueueName()) == 0) {
                iterator.remove();
                break;
            }
        }
        if (bindings.isEmpty()) {
            routingKeyToBindingMap.remove(routingKey);
        }
    }

    Set<Binding> getBindingsForRoute(String routingKey) {
        Set<Binding> bindings = routingKeyToBindingMap.get(routingKey);
        if (bindings == null) {
            bindings = Collections.emptySet();
        }
        return bindings;
    }

    boolean isEmpty() {
        return routingKeyToBindingMap.isEmpty();
    }
}
