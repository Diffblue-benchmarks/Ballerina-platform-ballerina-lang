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

package org.ballerinalang.net.http.nativeimpl.connection;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.util.exceptions.BallerinaConnectorException;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.NonBlockingCallback;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.http.WebSocketConstants;
import org.wso2.transport.http.netty.contract.websocket.WebSocketHandshaker;

/**
 * {@code CancelWebSocketUpgrade} is the action to cancel a WebSocket upgrade.
 *
 * @since 0.970
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "cancelWebSocketUpgrade",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = HttpConstants.CALLER,
                             structPackage = "ballerina/http"),
        args = {
                @Argument(name = "status", type = TypeKind.INT),
                @Argument(name = "reason", type = TypeKind.STRING)
        },
        isPublic = true
)
public class CancelWebSocketUpgrade implements NativeCallableUnit {
    @Override
    public void execute(Context context, CallableUnitCallback callback) {
//        try {
//            BMap<String, BValue> httpConnection = (BMap<String, BValue>) context.getRefArgument(0);
//            int statusCode = (int) context.getIntArgument(0);
//            String reason = context.getStringArgument(0);
//            WebSocketHandshaker webSocketHandshaker =
//                    (WebSocketHandshaker) httpConnection.getNativeData(WebSocketConstants.WEBSOCKET_MESSAGE);
//            if (webSocketHandshaker == null) {
//                throw new BallerinaConnectorException("Not a WebSocket upgrade request. Cannot cancel the request");
//            }
//            ChannelFuture future = webSocketHandshaker.cancelHandshake(statusCode, reason);
//            future.addListener((ChannelFutureListener) channelFuture -> {
//                Throwable cause = future.cause();
//                if (!future.isSuccess() && cause != null) {
//                    context.setReturnValues(HttpUtil.getError(context, cause));
//                } else {
//                    context.setReturnValues();
//                }
//                if (channelFuture.channel().isOpen()) {
//                    channelFuture.channel().close();
//                }
//                callback.notifySuccess();
//            });
//        } catch (Exception e) {
//            //Return this error.
//            context.setReturnValues(HttpUtil.getError(context, e));
//            callback.notifySuccess();
//        }
    }

    public static void cancelWebSocketUpgrade(Strand strand, ObjectValue connectionObj, int statusCode, String reason) {
        //TODO : NonBlockingCallback is used to handle non blocking call
        NonBlockingCallback callback = new NonBlockingCallback(strand);

        try {
            WebSocketHandshaker webSocketHandshaker =
                    (WebSocketHandshaker) connectionObj.getNativeData(WebSocketConstants.WEBSOCKET_MESSAGE);
            if (webSocketHandshaker == null) {
                throw new BallerinaConnectorException("Not a WebSocket upgrade request. Cannot cancel the request");
            }
            ChannelFuture future = webSocketHandshaker.cancelHandshake(statusCode, reason);
            future.addListener((ChannelFutureListener) channelFuture -> {
                Throwable cause = future.cause();
                if (!future.isSuccess() && cause != null) {
                    callback.setReturnValues(HttpUtil.getError(cause));
                } else {
                    callback.setReturnValues(null);
                }
                if (channelFuture.channel().isOpen()) {
                    channelFuture.channel().close();
                }
                callback.notifySuccess();
            });
        } catch (Exception e) {
            //Return this error.
            callback.setReturnValues(HttpUtil.getError(e));
            callback.notifySuccess();
        }
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}
