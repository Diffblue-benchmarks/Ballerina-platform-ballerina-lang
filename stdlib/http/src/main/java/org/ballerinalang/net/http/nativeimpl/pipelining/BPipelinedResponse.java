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

package org.ballerinalang.net.http.nativeimpl.pipelining;

import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.net.http.DataContext;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.util.Objects;

import static org.wso2.transport.http.netty.contract.Constants.MEANINGFULLY_EQUAL;

/**
 * Represents a pipelined response. Response order can be determined based on the sequence number.
 *
 * @since 0.982.0
 */
public class BPipelinedResponse implements Comparable<BPipelinedResponse> {

    private final HttpCarbonMessage inboundRequestMsg;
    private final HttpCarbonMessage outboundResponseMsg;
    private DataContext dataContext;
    private BMap<String, BValue> outboundResponse; //Ballerina outbound response object
    private final long sequenceId; //Identifies the response order

    public BPipelinedResponse(HttpCarbonMessage inboundRequestMsg, HttpCarbonMessage
            outboundResponseMsg, DataContext dataContext, BMap<String, BValue> outboundResponse) {
        this.inboundRequestMsg = inboundRequestMsg;
        this.outboundResponseMsg = outboundResponseMsg;
        this.dataContext = dataContext;
        this.outboundResponse = outboundResponse;
        this.sequenceId = inboundRequestMsg.getSequenceId();
    }

    BPipelinedResponse(HttpCarbonMessage inboundRequestMsg, HttpCarbonMessage outboundResponseMsg) {
        this.inboundRequestMsg = inboundRequestMsg;
        this.outboundResponseMsg = outboundResponseMsg;
        this.sequenceId = inboundRequestMsg.getSequenceId();
    }

    long getSequenceId() {
        return sequenceId;
    }

    HttpCarbonMessage getOutboundResponseMsg() {
        return outboundResponseMsg;
    }

    HttpCarbonMessage getInboundRequestMsg() {
        return inboundRequestMsg;
    }

    DataContext getDataContext() {
        return dataContext;
    }

    BMap<String, BValue> getOutboundResponse() {
        return outboundResponse;
    }

    @Override
    public int compareTo(BPipelinedResponse other) {
        return Long.compare(this.sequenceId, other.getSequenceId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BPipelinedResponse) {
            return compareTo((BPipelinedResponse) obj) == MEANINGFULLY_EQUAL;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sequenceId);
    }
}
