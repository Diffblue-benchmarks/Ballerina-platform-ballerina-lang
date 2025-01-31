/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.net.http.actions.httpclient;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.NonBlockingCallback;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.net.http.BHttpUtil;
import org.ballerinalang.net.http.DataContext;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.transport.http.netty.contract.HttpClientConnector;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.util.Locale;

import static org.ballerinalang.net.http.HttpUtil.checkRequestBodySizeHeadersAvailability;

/**
 * {@code Forward} action can be used to invoke an http call with incoming request httpVerb.
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "http",
        functionName = "nativeForward"
)
public class Forward extends AbstractHTTPAction {

    @Override
    public void execute(Context context, CallableUnitCallback callback) {
        DataContext dataContext = new DataContext(context, callback, createOutboundRequestMsg(context));
        // Execute the operation
        executeNonBlockingAction(dataContext, false);
    }

    @Override
    protected HttpCarbonMessage createOutboundRequestMsg(Context context) {
        String path = context.getStringArgument(1);
        BMap<String, BValue> requestStruct = ((BMap<String, BValue>) context.getRefArgument(1));

        if (requestStruct.getNativeData(HttpConstants.REQUEST) == null &&
                !BHttpUtil.isEntityDataSourceAvailable(requestStruct)) {
            throw new BallerinaException("invalid inbound request parameter");
        }
        HttpCarbonMessage outboundRequestMsg = BHttpUtil
                .getCarbonMsg(requestStruct, BHttpUtil.createHttpCarbonMessage(true));

        if (BHttpUtil.isEntityDataSourceAvailable(requestStruct)) {
            BHttpUtil.enrichOutboundMessage(outboundRequestMsg, requestStruct);
            prepareOutboundRequest(context, path, outboundRequestMsg,
                                   !BHttpUtil.checkRequestBodySizeHeadersAvailability(outboundRequestMsg));
            outboundRequestMsg.setHttpMethod(
                    BLangConnectorSPIUtil.toStruct(requestStruct).getStringField(HttpConstants.HTTP_REQUEST_METHOD));
        } else {
            prepareOutboundRequest(context, path, outboundRequestMsg,
                                   !BHttpUtil.checkRequestBodySizeHeadersAvailability(outboundRequestMsg));
            String httpVerb = outboundRequestMsg.getHttpMethod();
            outboundRequestMsg.setHttpMethod(httpVerb.trim().toUpperCase(Locale.getDefault()));
        }
        return outboundRequestMsg;
    }

    public static Object nativeForward(Strand strand, String url, MapValue config, String path,
                                       ObjectValue requestObj) {
        HttpCarbonMessage outboundRequestMsg = createOutboundRequestMsg(url, path, requestObj);
        HttpClientConnector clientConnector = (HttpClientConnector) config.getNativeData(HttpConstants.HTTP_CLIENT);
        DataContext dataContext = new DataContext(strand, clientConnector, new NonBlockingCallback(strand), requestObj,
                                                  outboundRequestMsg);
        executeNonBlockingAction(dataContext, false);
        return null;
    }

    protected static HttpCarbonMessage createOutboundRequestMsg(String serviceUri, String path,
                                                                ObjectValue requestObj) {
        if (requestObj.getNativeData(HttpConstants.REQUEST) == null &&
                !HttpUtil.isEntityDataSourceAvailable(requestObj)) {
            throw new BallerinaException("invalid inbound request parameter");
        }
        HttpCarbonMessage outboundRequestMsg = HttpUtil
                .getCarbonMsg(requestObj, HttpUtil.createHttpCarbonMessage(true));

        if (HttpUtil.isEntityDataSourceAvailable(requestObj)) {
            HttpUtil.enrichOutboundMessage(outboundRequestMsg, requestObj);
            prepareOutboundRequest(serviceUri, path, outboundRequestMsg,
                                   !checkRequestBodySizeHeadersAvailability(outboundRequestMsg));
            outboundRequestMsg.setHttpMethod(requestObj.get(HttpConstants.HTTP_REQUEST_METHOD).toString());
        } else {
            prepareOutboundRequest(serviceUri, path, outboundRequestMsg,
                                   !checkRequestBodySizeHeadersAvailability(outboundRequestMsg));
            String httpVerb = outboundRequestMsg.getHttpMethod();
            outboundRequestMsg.setHttpMethod(httpVerb.trim().toUpperCase(Locale.getDefault()));
        }
        return outboundRequestMsg;
    }
}
