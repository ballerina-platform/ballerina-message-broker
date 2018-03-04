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
package io.ballerina.messaging.broker.client.cmd.impl.create;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.ballerina.messaging.broker.client.resources.Queue;
import io.ballerina.messaging.broker.client.utils.Constants;

/**
 * Command representing MB queue creation.
 */
@Parameters(commandDescription = "Create a queue in the Broker with parameters")
public class CreateQueueCmd extends CreateCmd {

    @Parameter(description = "name of the queue",
               required = true)
    private String queueName;

    @Parameter(names = { "--autoDelete", "-a" },
               description = "is auto delete enabled")
    private boolean autoDelete = false;

    @Parameter(names = { "--durable", "-d" },
               description = "durability of the queue")
    private boolean durable = false;

    public CreateQueueCmd(String rootCommand) {
        super(rootCommand, "Queue created successfully");
    }

    @Override
    public void execute() {
        if (help) {
            processHelpLogs();
            return;
        }

        Queue queue = new Queue(queueName, autoDelete, durable);

        performResourceCreationOverHttp(Constants.QUEUES_URL_PARAM, queue.getAsJsonString());
    }

    @Override
    public void appendUsage(StringBuilder out) {
        out.append("Usage:\n");
        out.append("  " + rootCommand + " create queue [queue-name] [flag]*\n");
    }
}
