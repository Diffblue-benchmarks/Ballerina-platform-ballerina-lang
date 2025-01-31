/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.nativeimpl.builtin.maplib;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;

/**
 * Extern function to check existence of key.
 * ballerina.model.map:hasKey(string)
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "builtin",
        functionName = "map.hasKey",
        args = {@Argument(name = "m", type = TypeKind.MAP),
                @Argument(name = "key", type = TypeKind.STRING)},
        returnType = {@ReturnType(type = TypeKind.BOOLEAN)},
        isPublic = true
)
public class HasKey extends BlockingNativeCallableUnit {

    public void execute(Context ctx) {
        BMap<String, BValue> map = (BMap<String, BValue>) ctx.getRefArgument(0);
        String key = ctx.getStringArgument(0);
        ctx.setReturnValues(new BBoolean(map.hasKey(key)));
    }

    public static boolean hasKey(Strand strand, MapValue<?, ?> map, String key) {
        return map.containsKey(key);
    }
}

