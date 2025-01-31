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
 */

package org.ballerinalang.stdlib.internal;

import static org.ballerinalang.util.BLangConstants.ORG_NAME_SEPARATOR;

/**
 * Constants for internal package file functions.
 */
public class Constants {
    /**
     * Organization name.
     */
    public static final String ORG_NAME = "ballerina";
    
    /**
     * Package name.
     */
    public static final String PACKAGE_NAME = "internal";
    
    /**
     * Package path to internal package.
     */
    public static final String PACKAGE_PATH = ORG_NAME + ORG_NAME_SEPARATOR + PACKAGE_NAME;
    
    /**
     * Specifies the struct definition of the path.
     */
    public static final String PATH_STRUCT = "Path";
    
    /**
     * Defines the internal struct path value.
     */
    public static final String PATH_DEFINITION_NAME = "PathDef";

    /**
     * Init function name.
     */
    public static final String INIT_FUNCTION_NAME = "__init";
}
