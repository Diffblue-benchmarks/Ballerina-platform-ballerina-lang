/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.net.http.actions.websocketconnector;

import io.netty.channel.ChannelFuture;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.NonBlockingCallback;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.model.values.BValueArray;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.http.BHttpUtil;
import org.ballerinalang.net.http.BWebSocketOpenConnectionInfo;
import org.ballerinalang.net.http.BWebSocketUtil;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.http.WebSocketConstants;
import org.ballerinalang.net.http.WebSocketOpenConnectionInfo;
import org.ballerinalang.net.http.WebSocketUtil;

import java.nio.ByteBuffer;

/**
 * {@code Get} is the GET action implementation of the HTTP Connector.
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "ping",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = WebSocketConstants.WEBSOCKET_CONNECTOR,
                             structPackage = "ballerina/http"),
        args = {@Argument(name = "data", type = TypeKind.ARRAY, elementType = TypeKind.BYTE)}
)
public class Ping implements NativeCallableUnit {

    @Override
    public void execute(Context context, CallableUnitCallback callback) {
        try {
            BMap<String, BValue> wsConnection = (BMap<String, BValue>) context.getRefArgument(0);
            BWebSocketOpenConnectionInfo connectionInfo = (BWebSocketOpenConnectionInfo) wsConnection
                    .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
            byte[] binaryData = ((BValueArray) context.getRefArgument(1)).getBytes();
            ChannelFuture future = connectionInfo.getWebSocketConnection().ping(ByteBuffer.wrap(binaryData));
            BWebSocketUtil.handleWebSocketCallback(context, callback, future);
        } catch (Exception e) {
            context.setReturnValues(BHttpUtil.getError(context, e.getMessage()));
            callback.notifySuccess();
        }
    }

    public static void ping(Strand strand, ObjectValue wsConnection, byte[] binaryData) {
        //TODO : NonBlockingCallback is temporary fix to handle non blocking call
        NonBlockingCallback callback = new NonBlockingCallback(strand);
        try {
            WebSocketOpenConnectionInfo connectionInfo = (WebSocketOpenConnectionInfo) wsConnection
                    .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
            ChannelFuture future = connectionInfo.getWebSocketConnection().ping(ByteBuffer.wrap(binaryData));
            WebSocketUtil.handleWebSocketCallback(callback, future);
        } catch (Exception e) {
            //TODO remove this call back
            callback.setReturnValues(HttpUtil.getError(e.getMessage()));
            callback.notifySuccess();
        }
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}
