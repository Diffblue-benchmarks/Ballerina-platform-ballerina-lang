/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.net.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class holds the resource signature path parameters.
 *
 * @since 0.995.0
 */
public class HttpResourceArguments {

    private Map<String, String> resourceArgumentValues = new HashMap<>();
    private List<String> pathParamList = new ArrayList<>();

    public HttpResourceArguments() {
        resourceArgumentValues = new HashMap<>();
        pathParamList = new ArrayList<>();

    }

    public Map<String, String> getMap() {
        return resourceArgumentValues;
    }

    public List<String> getList() {
        return pathParamList;
    }
}
