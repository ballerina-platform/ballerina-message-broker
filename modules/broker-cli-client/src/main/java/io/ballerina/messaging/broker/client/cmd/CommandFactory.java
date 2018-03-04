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
package io.ballerina.messaging.broker.client.cmd;

import io.ballerina.messaging.broker.client.cmd.impl.InitCmd;
import io.ballerina.messaging.broker.client.cmd.impl.RootCmd;
import io.ballerina.messaging.broker.client.cmd.impl.create.CreateBindingCmd;
import io.ballerina.messaging.broker.client.cmd.impl.create.CreateCmd;
import io.ballerina.messaging.broker.client.cmd.impl.create.CreateExchangeCmd;
import io.ballerina.messaging.broker.client.cmd.impl.create.CreateQueueCmd;
import io.ballerina.messaging.broker.client.cmd.impl.delete.DeleteCmd;
import io.ballerina.messaging.broker.client.cmd.impl.delete.DeleteExchangeCmd;
import io.ballerina.messaging.broker.client.cmd.impl.delete.DeleteQueueCmd;
import io.ballerina.messaging.broker.client.cmd.impl.list.ListBindingCmd;
import io.ballerina.messaging.broker.client.cmd.impl.list.ListCmd;
import io.ballerina.messaging.broker.client.cmd.impl.list.ListExchangeCmd;
import io.ballerina.messaging.broker.client.cmd.impl.list.ListQueueCmd;

/**
 * Factory class to generate different command instances.
 */
public class CommandFactory {

    /**
     * This will hold the value loaded from the script.
     */
    private final String rootCommand;

    public CommandFactory(String rootCommand) {
        this.rootCommand = rootCommand;
    }

    public InitCmd createInitCommand() {
        return new InitCmd(rootCommand);
    }

    public RootCmd createRootCommand() {
        return new RootCmd(rootCommand);
    }

    public CreateCmd createCreateCommand() {
        return new CreateCmd(rootCommand);
    }

    public DeleteCmd createDeleteCommand() {
        return new DeleteCmd(rootCommand);
    }

    public ListCmd createListCommand() {
        return new ListCmd(rootCommand);
    }

    public CreateExchangeCmd createCreateExchangeCommand() {
        return new CreateExchangeCmd(rootCommand);
    }

    public DeleteExchangeCmd createDeleteExchangeCommand() {
        return new DeleteExchangeCmd(rootCommand);
    }

    public ListExchangeCmd createListExchangeCommand() {
        return new ListExchangeCmd(rootCommand);
    }

    public CreateQueueCmd createCreateQueueCommand() {
        return new CreateQueueCmd(rootCommand);
    }
    public DeleteQueueCmd createDeleteQueueCommand() {
        return new DeleteQueueCmd(rootCommand);
    }

    public ListQueueCmd createListQueueCommand() {
        return new ListQueueCmd(rootCommand);
    }

    public CreateBindingCmd createCreateBindingCommand() {
        return new CreateBindingCmd(rootCommand);
    }

    public ListBindingCmd createListBindingCommand() {
        return new ListBindingCmd(rootCommand);
    }
}
