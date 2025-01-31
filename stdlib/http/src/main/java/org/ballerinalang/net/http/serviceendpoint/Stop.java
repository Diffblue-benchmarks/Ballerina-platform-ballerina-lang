/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.net.http.serviceendpoint;

import org.ballerinalang.bre.Context;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.http.HttpConstants;

import static org.ballerinalang.net.http.HttpConstants.HTTP_LISTENER_ENDPOINT;

/**
 * Get the ID of the connection.
 *
 * @since 0.966
 */

@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "stop",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = HTTP_LISTENER_ENDPOINT,
                             structPackage = "ballerina/http"),
        isPublic = true
)
public class Stop extends AbstractHttpNativeFunction {

    @Override
    public void execute(Context context) {
        Struct serverEndpoint = BLangConnectorSPIUtil.getConnectorEndpointStruct(context);
        getServerConnector(serverEndpoint).stop();
        serverEndpoint.addNativeData(HttpConstants.CONNECTOR_STARTED, false);
        resetRegistry(serverEndpoint);
        context.setReturnValues();
    }

    public static void stop(Strand strand, ObjectValue serverEndpoint) {
        getServerConnector(serverEndpoint).stop();
        serverEndpoint.addNativeData(HttpConstants.CONNECTOR_STARTED, false);
        resetRegistry(serverEndpoint);
    }
}
