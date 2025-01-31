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

package org.ballerinalang.messaging.rabbitmq.nativeimpl.channel;

import com.rabbitmq.client.Channel;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.messaging.rabbitmq.RabbitMQConnectorException;
import org.ballerinalang.messaging.rabbitmq.RabbitMQConstants;
import org.ballerinalang.messaging.rabbitmq.RabbitMQUtils;
import org.ballerinalang.messaging.rabbitmq.util.ChannelUtils;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Closes a RabbitMQ Channel.
 *
 * @since 0.995.0
 */
@BallerinaFunction(
        orgName = RabbitMQConstants.ORG_NAME,
        packageName = RabbitMQConstants.RABBITMQ,
        functionName = "close",
        receiver = @Receiver(type = TypeKind.OBJECT,
                structType = RabbitMQConstants.CHANNEL_OBJECT,
                structPackage = RabbitMQConstants.PACKAGE_RABBITMQ),
        isPublic = true
)
public class Close extends BlockingNativeCallableUnit {

    private static final Logger LOGGER = LoggerFactory.getLogger(Close.class);

    @Override
    public void execute(Context context) {
        @SuppressWarnings(RabbitMQConstants.UNCHECKED)
        BMap<String, BValue> channelBObject = (BMap<String, BValue>) context.getRefArgument(0);
        Channel channel = RabbitMQUtils.getNativeObject(channelBObject,
                RabbitMQConstants.CHANNEL_NATIVE_OBJECT, Channel.class, context);
        BValue closeCode = context.getNullableRefArgument(1);
        BValue closeMessage = context.getNullableRefArgument(2);
        try {
            ChannelUtils.handleCloseChannel(channel, closeCode, closeMessage);
            channelBObject.addNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT, null);
        } catch (RabbitMQConnectorException exception) {
            LOGGER.error("I/O exception while closing the channel", exception);
            RabbitMQUtils.returnError(RabbitMQConstants.RABBITMQ_CLIENT_ERROR, context, exception);
        }
    }
}
