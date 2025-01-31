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

package org.ballerinalang.net.http;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.wso2.transport.http.netty.contract.websocket.WebSocketConnection;

/**
 * This class represent already opened WebSocket connection. Which include all necessary details needed after for
 * dispatching after a successful handshake.
 */
public class BWebSocketOpenConnectionInfo {

    private final BWebSocketService webSocketService;
    private final BMap<String, BValue> webSocketEndpoint;
    private final WebSocketConnection webSocketConnection;
    private String aggregateString = "";
    private Context context;

    public BWebSocketOpenConnectionInfo(BWebSocketService webSocketService, WebSocketConnection webSocketConnection,
                                        BMap<String, BValue> webSocketEndpoint, Context context) {
        this.webSocketService = webSocketService;
        this.webSocketConnection = webSocketConnection;
        this.webSocketEndpoint = webSocketEndpoint;
        this.context = context;
    }

    public BWebSocketService getService() {
        return webSocketService;
    }

    public BMap<String, BValue> getWebSocketEndpoint() {
        return webSocketEndpoint;
    }

    public WebSocketConnection getWebSocketConnection() throws IllegalAccessException {
        if (webSocketConnection != null) {
            return webSocketConnection;
        } else {
            throw new IllegalAccessException("The WebSocket connection has not been made");
        }
    }

    String getAggregateString() {
        return aggregateString;
    }

    void appendAggregateString(String aggreageString) {
        this.aggregateString += aggreageString;
    }

    void resetAggregateString() {
        this.aggregateString = "";
    }

    public Context getContext() {
        return context;
    }
}
