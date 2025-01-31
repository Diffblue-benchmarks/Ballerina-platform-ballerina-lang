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
 */

package org.ballerinalang.stdlib.crypto.nativeimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.model.values.BValueArray;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.stdlib.crypto.Constants;
import org.ballerinalang.stdlib.crypto.CryptoUtils;

import java.security.InvalidKeyException;
import java.security.PublicKey;

/**
 * Extern function ballerina.crypto:verifyRsaSha256Signature.
 *
 * @since 0.990.4
 */
@BallerinaFunction(orgName = "ballerina", packageName = "crypto", functionName = "verifyRsaSha256Signature")
public class VerifyRsaSha256Signature extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        BValue dataBValue = context.getRefArgument(0);
        BValue signatureBValue = context.getRefArgument(1);
        BMap<String, BValue> publicKey = (BMap<String, BValue>) context.getRefArgument(2);
        byte[] data = ((BValueArray) dataBValue).getBytes();
        byte[] signature = ((BValueArray) signatureBValue).getBytes();
        try {
            context.setReturnValues(new BBoolean(CryptoUtils.verify(context, "SHA256withRSA",
                    (PublicKey) publicKey.getNativeData(Constants.NATIVE_DATA_PUBLIC_KEY), data, signature)));
        } catch (InvalidKeyException e) {
            context.setReturnValues(CryptoUtils.createCryptoError(context, "invalid uninitialized key"));
        }
    }

    public static Object verifyRsaSha256Signature(Strand strand, ArrayValue dataValue, ArrayValue signatureValue,
                                                  MapValue<?, ?> publicKey) {
        byte[] data = dataValue.getBytes();
        byte[] signature = signatureValue.getBytes();
        try {
            PublicKey key = (PublicKey) publicKey.getNativeData(Constants.NATIVE_DATA_PUBLIC_KEY);
            return CryptoUtils.verify("SHA256withRSA", key, data, signature);
        } catch (InvalidKeyException e) {
            return CryptoUtils.createCryptoError("invalid uninitialized key");
        }
    }
}
