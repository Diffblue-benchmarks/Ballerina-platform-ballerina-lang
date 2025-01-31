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

import com.rabbitmq.client.Channel;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.connector.api.Annotation;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.Service;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.connector.api.Value;
import org.ballerinalang.messaging.rabbitmq.RabbitMQConstants;
import org.ballerinalang.messaging.rabbitmq.util.ChannelUtils;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binds the ChannelListener to a service.
 *
 * @since 0.995.0
 */
@BallerinaFunction(
        orgName = RabbitMQConstants.ORG_NAME,
        packageName = RabbitMQConstants.RABBITMQ,
        functionName = "registerListener",
        receiver = @Receiver(type = TypeKind.OBJECT,
                structType = RabbitMQConstants.CHANNEL_LISTENER_OBJECT,
                structPackage = RabbitMQConstants.PACKAGE_RABBITMQ)
)
public class RegisterListener extends BlockingNativeCallableUnit {
    private final ArrayList<Service> services = new ArrayList<>();

    @Override
    public void execute(Context context) {
        @SuppressWarnings(RabbitMQConstants.UNCHECKED)
        BMap<String, BValue> channelListObject = (BMap<String, BValue>) context.getRefArgument(0);
        @SuppressWarnings(RabbitMQConstants.UNCHECKED)
        BMap<String, BValue> channelObj =
                (BMap<String, BValue>) channelListObject.get(RabbitMQConstants.CHANNEL_REFERENCE);
        Channel channel = (Channel) channelObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        Service service = BLangConnectorSPIUtil.getServiceRegistered(context);
        List<Annotation> annotationList = service.getAnnotationList(RabbitMQConstants.PACKAGE_RABBITMQ,
                RabbitMQConstants.SERVICE_CONFIG);
        Annotation annotation = annotationList.get(0);
        Struct value = annotation.getValue();
        Map<String, Value> queueConfig = value.getMapField(RabbitMQConstants.QUEUE_CONFIG);
        String queueName = queueConfig.get(RabbitMQConstants.ALIAS_QUEUE_NAME).getStringValue();
        boolean durable = queueConfig.get(RabbitMQConstants.ALIAS_QUEUE_DURABLE).getBooleanValue();
        boolean exclusive = queueConfig.get(RabbitMQConstants.ALIAS_QUEUE_EXCLUSIVE).getBooleanValue();
        boolean autoDelete = queueConfig.get(RabbitMQConstants.ALIAS_QUEUE_AUTODELETE).getBooleanValue();
        ChannelUtils.queueDeclare(channel, queueName, durable, exclusive, autoDelete);
        services.add(service);
        channelListObject.addNativeData(RabbitMQConstants.CONSUMER_SERVICES, services);
    }
}
