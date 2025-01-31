/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.net.http.nativeimpl.connection;

import org.ballerinalang.mime.util.EntityBodyHandler;
import org.ballerinalang.mime.util.HeaderUtil;
import org.ballerinalang.mime.util.MultipartBDataSource;
import org.ballerinalang.model.values.BError;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.model.values.BValueArray;
import org.ballerinalang.net.http.BHttpUtil;
import org.ballerinalang.net.http.DataContext;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.transport.http.netty.contract.HttpConnectorListener;
import org.wso2.transport.http.netty.contract.HttpResponseFuture;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;
import org.wso2.transport.http.netty.message.HttpMessageDataStreamer;
import org.wso2.transport.http.netty.message.PooledDataStreamerFactory;

import java.io.IOException;
import java.io.OutputStream;

import static org.ballerinalang.net.http.BHttpUtil.extractEntity;

/**
 * Response writer contains utility methods to serialize response data.
 *
 * @since 0.982.0
 */
public class BResponseWriter {

    /**
     * Send outbound response to destination.
     *
     * @param dataContext      Represents data context which acts as the callback for response status
     * @param requestMessage   Represents the request that corresponds to the response
     * @param outboundResponse Represents ballerina response
     * @param responseMessage  Represents native response message
     */
    public static void sendResponseRobust(DataContext dataContext, HttpCarbonMessage requestMessage, BMap<String,
            BValue> outboundResponse, HttpCarbonMessage responseMessage) {
        String contentType = HttpUtil.getContentTypeFromTransportMessage(responseMessage);
        String boundaryString = null;
        if (HeaderUtil.isMultipart(contentType)) {
            boundaryString = HttpUtil.addBoundaryIfNotExist(responseMessage, contentType);
        }
        HttpMessageDataStreamer outboundMsgDataStreamer = getResponseDataStreamer(responseMessage);
        BMap<String, BValue> entityStruct = extractEntity(outboundResponse);
        if (entityStruct == null) {
            responseMessage.setPassthrough(true);
        }
        HttpResponseFuture outboundRespStatusFuture = HttpUtil.sendOutboundResponse(requestMessage, responseMessage);
        HttpConnectorListener outboundResStatusConnectorListener =
                new ResponseWriter.HttpResponseConnectorListener(dataContext, outboundMsgDataStreamer);
        outboundRespStatusFuture.setHttpConnectorListener(outboundResStatusConnectorListener);

        OutputStream messageOutputStream = outboundMsgDataStreamer.getOutputStream();
        if (entityStruct != null) {
            if (boundaryString != null) {
                serializeMultiparts(boundaryString, entityStruct, messageOutputStream);
            } else {
                BValue outboundMessageSource = EntityBodyHandler.getMessageDataSource(entityStruct);
                serializeDataSource(outboundMessageSource, entityStruct, messageOutputStream);
            }
        }
    }

    /**
     * Serialize multipart entity body. If an array of body parts exist, encode body parts else serialize body content
     * if it exist as a byte channel.
     *
     * @param boundaryString      Boundary string that should be used in encoding body parts
     * @param entity              Represents the entity that holds the actual body
     * @param messageOutputStream Represents the output stream
     */
    private static void serializeMultiparts(String boundaryString, BMap<String, BValue> entity,
                                            OutputStream messageOutputStream) {
        BValueArray bodyParts = EntityBodyHandler.getBodyPartArray(entity);
        try {
            if (bodyParts != null && bodyParts.size() > 0) {
                MultipartBDataSource multipartDataSource = new MultipartBDataSource(entity, boundaryString);
                serializeDataSource(multipartDataSource, entity, messageOutputStream);
                HttpUtil.closeMessageOutputStream(messageOutputStream);
            } else {
                EntityBodyHandler.writeByteChannelToOutputStream(entity, messageOutputStream);
                HttpUtil.closeMessageOutputStream(messageOutputStream);
            }
        } catch (IOException ex) {
            throw new BallerinaException("Error occurred while serializing byte channel content : ", ex);
        }
    }

    /**
     * Serialize message datasource.
     *
     * @param outboundMessageSource Outbound message datasource that needs to be serialized
     * @param entity                Represents the entity that holds headers and body content
     * @param messageOutputStream   Represents the output stream
     */
    static void serializeDataSource(BValue outboundMessageSource, BMap<String, BValue> entity,
                                    OutputStream messageOutputStream) {
        try {
            if (outboundMessageSource != null) {
                BHttpUtil.serializeDataSource(outboundMessageSource, entity, messageOutputStream);
                BHttpUtil.closeMessageOutputStream(messageOutputStream);
            } else { //When the entity body is a byte channel
                EntityBodyHandler.writeByteChannelToOutputStream(entity, messageOutputStream);
                BHttpUtil.closeMessageOutputStream(messageOutputStream);
            }
        } catch (IOException ex) {
            throw new BallerinaException("Error occurred while serializing message data source : ", ex);
        }
    }

    /**
     * Get the response data streamer that should be used for serializing data.
     *
     * @param outboundResponse Represents native response
     * @return HttpMessageDataStreamer that should be used for serializing
     */
    static HttpMessageDataStreamer getResponseDataStreamer(HttpCarbonMessage outboundResponse) {
        final HttpMessageDataStreamer outboundMsgDataStreamer;
        final PooledDataStreamerFactory pooledDataStreamerFactory = (PooledDataStreamerFactory)
                outboundResponse.getProperty(HttpConstants.POOLED_BYTE_BUFFER_FACTORY);
        if (pooledDataStreamerFactory != null) {
            outboundMsgDataStreamer = pooledDataStreamerFactory.createHttpDataStreamer(outboundResponse);
        } else {
            outboundMsgDataStreamer = new HttpMessageDataStreamer(outboundResponse);
        }
        return outboundMsgDataStreamer;
    }

    /**
     * Response listener class receives notifications once a message has been sent out.
     */
    static class HttpResponseConnectorListener implements HttpConnectorListener {

        private final DataContext dataContext;
        private HttpMessageDataStreamer outboundMsgDataStreamer;

        HttpResponseConnectorListener(DataContext dataContext) {
            this.dataContext = dataContext;
        }

        HttpResponseConnectorListener(DataContext dataContext, HttpMessageDataStreamer outboundMsgDataStreamer) {
            this.dataContext = dataContext;
            this.outboundMsgDataStreamer = outboundMsgDataStreamer;
        }

        @Override
        public void onMessage(HttpCarbonMessage httpCarbonMessage) {
            this.dataContext.notifyOutboundBResponseStatus(null);
        }

        @Override
        public void onError(Throwable throwable) {
            BError httpConnectorError = BHttpUtil.getError(dataContext.getContext(), throwable);
            if (outboundMsgDataStreamer != null) {
                if (throwable instanceof IOException) {
                    this.dataContext.getOutboundRequest().setIoException((IOException) throwable);
                } else {
                    this.dataContext.getOutboundRequest()
                            .setIoException(new IOException(throwable.getMessage(), throwable));
                }
            }
            this.dataContext.notifyOutboundBResponseStatus(httpConnectorError);
        }
    }
}
