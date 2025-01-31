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

package org.ballerinalang.messaging.rabbitmq.nativeimpl.channel.listener;

import org.ballerinalang.bre.bvm.BLangVMErrors;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.model.values.BError;
import org.ballerinalang.services.ErrorHandlerUtils;

import java.util.concurrent.CountDownLatch;

/**
 * The resource call back implementation for RabbitMQ async consumer.
 *
 * @since 0.995.0
 */
public class RabbitMQResourceCallback implements CallableUnitCallback {
    private CountDownLatch countDownLatch;

    RabbitMQResourceCallback(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void notifySuccess() {
        countDownLatch.countDown();
    }

    @Override
    public void notifyFailure(BError error) {
        countDownLatch.countDown();
        ErrorHandlerUtils.printError("RabbitMQ Error: " + BLangVMErrors.getPrintableStackTrace(error));
    }
}
