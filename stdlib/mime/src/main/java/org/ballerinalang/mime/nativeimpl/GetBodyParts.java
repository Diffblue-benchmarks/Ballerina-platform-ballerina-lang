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
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.mime.util.EntityBodyHandler;
import org.ballerinalang.mime.util.HeaderUtil;
import org.ballerinalang.mime.util.MimeUtil;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.model.values.BValueArray;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.stdlib.io.channels.base.Channel;

import java.util.Locale;

import static org.ballerinalang.mime.util.MimeConstants.ENTITY_BYTE_CHANNEL;
import static org.ballerinalang.mime.util.MimeConstants.FIRST_PARAMETER_INDEX;
import static org.ballerinalang.mime.util.MimeConstants.MESSAGE_AS_PRIMARY_TYPE;
import static org.ballerinalang.mime.util.MimeConstants.MULTIPART_AS_PRIMARY_TYPE;

/**
 * Extract body parts from a given entity.
 *
 * @since 0.963.0
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "mime",
        functionName = "getBodyParts",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "Entity", structPackage = "ballerina/mime"),
        returnType = {@ReturnType(type = TypeKind.ARRAY), @ReturnType(type = TypeKind.RECORD)},
        isPublic = true
)
public class GetBodyParts extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        BValueArray partsArray;
        try {
            BMap<String, BValue> entityStruct = (BMap<String, BValue>) context.getRefArgument(FIRST_PARAMETER_INDEX);
            String baseType = HeaderUtil.getBaseType(entityStruct);
            if (baseType != null && (baseType.toLowerCase(Locale.getDefault()).startsWith(MULTIPART_AS_PRIMARY_TYPE) ||
                    baseType.toLowerCase(Locale.getDefault()).startsWith(MESSAGE_AS_PRIMARY_TYPE))) {
                //Get the body parts from entity's multipart data field, if they've been already been decoded
                partsArray = EntityBodyHandler.getBodyPartArray(entityStruct);
                if (partsArray == null || partsArray.size() < 1) {
                    Channel byteChannel = EntityBodyHandler.getByteChannel(entityStruct);
                    if (byteChannel != null) {
                        EntityBodyHandler.decodeEntityBody(context, entityStruct, byteChannel);
                        //Check the body part availability for the second time, since the parts will be by this
                        // time populated from bytechannel
                        partsArray = EntityBodyHandler.getBodyPartArray(entityStruct);
                        //Set byte channel that belongs to parent entity to null, once the message body parts have
                        // been decoded
                        entityStruct.addNativeData(ENTITY_BYTE_CHANNEL, null);
                    }
                }
                context.setReturnValues(partsArray);
            } else {
                context.setReturnValues(MimeUtil.createError(context, "Entity body is not a type of " +
                        "composite media type. Received content-type : " + baseType));
            }
        } catch (Throwable e) {
            context.setReturnValues(MimeUtil.createError(context, "Error occurred while extracting body parts " +
                    "from entity: " + e.getMessage()));
        }
    }

    public static Object getBodyParts(Strand strand, ObjectValue entityObj) {
        ArrayValue partsArray;
        try {
            String baseType = HeaderUtil.getBaseType(entityObj);
            if (baseType != null && (baseType.toLowerCase(Locale.getDefault()).startsWith(MULTIPART_AS_PRIMARY_TYPE) ||
                    baseType.toLowerCase(Locale.getDefault()).startsWith(MESSAGE_AS_PRIMARY_TYPE))) {
                //Get the body parts from entity's multipart data field, if they've been already been decoded
                partsArray = EntityBodyHandler.getBodyPartArray(entityObj);
                if (partsArray == null || partsArray.size() < 1) {
                    Channel byteChannel = EntityBodyHandler.getByteChannel(entityObj);
                    if (byteChannel != null) {
                        EntityBodyHandler.decodeEntityBody(entityObj, byteChannel);
                        //Check the body part availability for the second time, since the parts will be by this
                        // time populated from bytechannel
                        partsArray = EntityBodyHandler.getBodyPartArray(entityObj);
                        //Set byte channel that belongs to parent entity to null, once the message body parts have
                        // been decoded
                        entityObj.addNativeData(ENTITY_BYTE_CHANNEL, null);
                    }
                }
                return partsArray;
            } else {
                return MimeUtil.createError("Entity body is not a type of " +
                        "composite media type. Received content-type : " + baseType);
            }
        } catch (Throwable e) {
            return MimeUtil.createError("Error occurred while extracting body parts from entity: " + e.getMessage());
        }
    }
}
