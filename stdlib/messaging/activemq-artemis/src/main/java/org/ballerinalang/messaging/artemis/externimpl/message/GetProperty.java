/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.ballerinalang.messaging.artemis.externimpl.message;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.messaging.artemis.ArtemisConstants;
import org.ballerinalang.messaging.artemis.ArtemisUtils;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;

/**
 * Extern function for getting a message property.
 *
 * @since 0.995.0
 */

@BallerinaFunction(
        orgName = ArtemisConstants.BALLERINA,
        packageName = ArtemisConstants.ARTEMIS,
        functionName = "getProperty",
        receiver = @Receiver(
                type = TypeKind.OBJECT,
                structType = ArtemisConstants.MESSAGE_OBJ,
                structPackage = ArtemisConstants.PROTOCOL_PACKAGE_ARTEMIS
        ),
        args = {
                @Argument(
                        name = "key",
                        type = TypeKind.STRING
                )
        }
)
public class GetProperty extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        @SuppressWarnings(ArtemisConstants.UNCHECKED)
        BMap<String, BValue> messageObj = (BMap<String, BValue>) context.getRefArgument(0);
        ClientMessage message = (ClientMessage) messageObj.getNativeData(ArtemisConstants.ARTEMIS_MESSAGE);

        String key = context.getStringArgument(0);
        Object property = message.getObjectProperty(key);
        if (property != null) {
            context.setReturnValues(ArtemisUtils.getBValueFromObj(property, context));
        } else {
            context.setReturnValues();
        }
    }
}
