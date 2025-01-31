/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

package org.ballerinalang.mime.nativeimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.mime.util.EntityBodyHandler;
import org.ballerinalang.mime.util.MimeUtil;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.stdlib.io.utils.IOConstants;

import static org.ballerinalang.mime.util.MimeConstants.ENTITY_BYTE_CHANNEL;
import static org.ballerinalang.mime.util.MimeConstants.FIRST_PARAMETER_INDEX;
import static org.ballerinalang.mime.util.MimeConstants.MESSAGE_DATA_SOURCE;
import static org.ballerinalang.mime.util.MimeConstants.OCTET_STREAM;
import static org.ballerinalang.mime.util.MimeConstants.SECOND_PARAMETER_INDEX;

/**
 * Set byte channel as entity body.
 *
 * @since 0.963.0
 */
@BallerinaFunction(orgName = "ballerina", packageName = "mime",
        functionName = "setByteChannel",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "Entity", structPackage = "ballerina/mime"),
        args = {@Argument(name = "byteChannel", type = TypeKind.OBJECT), @Argument(name = "contentType",
                type = TypeKind.STRING)},
        isPublic = true
)
public class SetByteChannel extends BlockingNativeCallableUnit {
    @Override
    public void execute(Context context) {
        BMap<String, BValue> entityStruct = (BMap<String, BValue>) context.getRefArgument(FIRST_PARAMETER_INDEX);
        BMap<String, BValue> byteChannel = (BMap<String, BValue>) context.getRefArgument(SECOND_PARAMETER_INDEX);
        String contentType = context.getStringArgument(FIRST_PARAMETER_INDEX);
        entityStruct.addNativeData(ENTITY_BYTE_CHANNEL, byteChannel.getNativeData
                (IOConstants.BYTE_CHANNEL_NAME));
        BValue dataSource = EntityBodyHandler.getMessageDataSource(entityStruct);
        if (dataSource != null) { //Clear message data source when the user set a byte channel to entity
            entityStruct.addNativeData(MESSAGE_DATA_SOURCE, null);
        }
        MimeUtil.setMediaTypeToEntity(context, entityStruct, contentType);
        context.setReturnValues();
    }

    public static void setByteChannel(Strand strand, ObjectValue entityObj, ObjectValue byteChannel,
                                      String contentType) {
        entityObj.addNativeData(ENTITY_BYTE_CHANNEL, byteChannel.getNativeData(IOConstants.BYTE_CHANNEL_NAME));
        Object dataSource = EntityBodyHandler.getMessageDataSource(entityObj);
        if (dataSource != null) { //Clear message data source when the user set a byte channel to entity
            entityObj.addNativeData(MESSAGE_DATA_SOURCE, null);
        }
        MimeUtil.setMediaTypeToEntity(entityObj, contentType != null ? contentType : OCTET_STREAM);
    }
}
