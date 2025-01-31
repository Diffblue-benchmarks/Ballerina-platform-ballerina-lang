/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ballerinalang.net.http;

import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.model.values.BError;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

/**
 * {@code BHttpCallableUnitCallback} is the responsible for acting on notifications received from Ballerina side.
 *
 * @since 0.94
 */
public class BHttpCallableUnitCallback implements CallableUnitCallback {
    private HttpCarbonMessage requestMessage;

    public BHttpCallableUnitCallback(HttpCarbonMessage requestMessage) {
        this.requestMessage = requestMessage;
    }

    @Override
    public void notifySuccess() {
        requestMessage.waitAndReleaseAllEntities();
    }

    @Override
    public void notifyFailure(BError error) {
        BHttpUtil.handleFailure(requestMessage, error);
        requestMessage.waitAndReleaseAllEntities();
    }

}
