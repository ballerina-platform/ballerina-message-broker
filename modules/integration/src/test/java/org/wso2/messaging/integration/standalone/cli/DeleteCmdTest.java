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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.messaging.integration.standalone.cli;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.wso2.messaging.integration.util.TestConstants.CLI_CONFIG_LOCATION;
import static org.wso2.messaging.integration.util.TestConstants.CLI_CONFIG_SYSTEM_PROPERTY;

/**
 * Test class containing tests of 'delete' command.
 */
public class DeleteCmdTest {

    @BeforeClass
    private void init() {
        // set the config file path
        System.setProperty(CLI_CONFIG_SYSTEM_PROPERTY, CLI_CONFIG_LOCATION);
    }

    @AfterMethod
    private void resetStream() {
        // reset the print stream after each test
        PrintStreamHandler.resetStream();
    }

    @Test(groups = "StreamReading",
          description = "test command 'delete'")
    public void testDelete() {
        String[] cmd = { "delete" };
        String expectedLog = "a command is expected after 'delete'";
        String errorMessage = "error when executing 'delete' command";

        org.wso2.broker.client.Main.main(cmd);

        Assert.assertTrue(PrintStreamHandler.readStream().contains(expectedLog), errorMessage);
    }

    @Test(groups = "StreamReading",
          description = "test command 'delete --help'")
    public void testDeleteHelp() {
        String[] cmd = { "delete", "--help" };
        String expectedLog = "delete a resource in MB";
        String errorMessage = "error when executing 'delete --help' command";

        org.wso2.broker.client.Main.main(cmd);

        Assert.assertTrue(PrintStreamHandler.readStream().contains(expectedLog), errorMessage);
    }
}