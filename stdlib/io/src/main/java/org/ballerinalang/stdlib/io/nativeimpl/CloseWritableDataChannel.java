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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.ballerinalang.stdlib.io.nativeimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.NonBlockingCallback;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BError;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.stdlib.io.channels.base.DataChannel;
import org.ballerinalang.stdlib.io.events.EventContext;
import org.ballerinalang.stdlib.io.events.EventRegister;
import org.ballerinalang.stdlib.io.events.EventResult;
import org.ballerinalang.stdlib.io.events.Register;
import org.ballerinalang.stdlib.io.events.data.CloseDataChannelEvent;
import org.ballerinalang.stdlib.io.utils.IOConstants;
import org.ballerinalang.stdlib.io.utils.IOUtils;

/**
 * Extern function ballerina.io#DataChannel.close().
 *
 * @since 0.982.0
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "io",
        functionName = "close",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "WritableDataChannel",
                structPackage = "ballerina.io"),
        returnType = {@ReturnType(type = TypeKind.ERROR)},
        isPublic = true
)
public class CloseWritableDataChannel implements NativeCallableUnit {
    /**
     * The index of the DataChannel.
     */
    private static final int DATA_CHANNEL_INDEX = 0;

    /**
     * Close data channel.
     *
     * @param result the response received after the channel is closed.
     * @return the result of the close response.
     */
    private static EventResult closeResponse(EventResult<Boolean, EventContext> result) {
        EventContext eventContext = result.getContext();
        Context context = eventContext.getContext();
        CallableUnitCallback callback = eventContext.getCallback();
        Throwable error = eventContext.getError();
        if (null != error) {
            BError errorStruct = IOUtils.createError(context, IOConstants.IO_ERROR_CODE, error.getMessage());
            context.setReturnValues(errorStruct);
        }
        callback.notifySuccess();
        return result;
    }

    /**
     * <p>
     * Close data channel.
     * </p>
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void execute(Context context, CallableUnitCallback callback) {
        BMap<String, BValue> dataChannelStruct = (BMap<String, BValue>) context.getRefArgument(DATA_CHANNEL_INDEX);
        DataChannel channel = (DataChannel) dataChannelStruct.getNativeData(IOConstants.DATA_CHANNEL_NAME);
        EventContext eventContext = new EventContext(context, callback);
        CloseDataChannelEvent dataChannelCloseEvt = new CloseDataChannelEvent(channel, eventContext);
        Register register = EventRegister.getFactory().register(dataChannelCloseEvt,
                CloseWritableDataChannel::closeResponse);
        eventContext.setRegister(register);
        register.submit();
    }

    @Override
    public boolean isBlocking() {
        return false;
    }

    public static Object close(Strand strand, ObjectValue dataChannel) {
        DataChannel channel = (DataChannel) dataChannel.getNativeData(IOConstants.DATA_CHANNEL_NAME);
        EventContext eventContext = new EventContext(new NonBlockingCallback(strand));
        CloseDataChannelEvent dataChannelCloseEvt = new CloseDataChannelEvent(channel, eventContext);
        Register register = EventRegister.getFactory().register(dataChannelCloseEvt,
                                                                CloseWritableDataChannel::closeChannel);
        eventContext.setRegister(register);
        register.submit();
        return null;
    }

    private static EventResult closeChannel(EventResult<Boolean, EventContext> result) {
        EventContext eventContext = result.getContext();
        NonBlockingCallback callback = eventContext.getNonBlockingCallback();
        Throwable error = eventContext.getError();
        if (null != error) {
            callback.setReturnValues(IOUtils.createError(error.getMessage()));
        }
        callback.notifySuccess();
        return result;
    }
}
