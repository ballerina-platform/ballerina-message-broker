/*
* Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.wso2.broker.auth.user.config;

import java.util.ArrayList;
import java.util.List;

/**
 * User file mapper class.
 */
public class UsersFile {

    private List<UserConfig> users = new ArrayList<>();

    public List<UserConfig> getUserConfigs() {
        return users;
    }

    public void setUserConfigs(List<UserConfig> users) {
        this.users = users;
    }
}