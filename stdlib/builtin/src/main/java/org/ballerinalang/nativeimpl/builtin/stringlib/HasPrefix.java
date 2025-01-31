/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.nativeimpl.builtin.stringlib;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;

/**
 * Extern function ballerina.model.strings:hasPrefix.
 *
 * @since 0.8.0
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "builtin",
        functionName = "string.hasPrefix",
        args = {@Argument(name = "mainString", type = TypeKind.STRING),
                @Argument(name = "prefix", type = TypeKind.STRING)},
        returnType = {@ReturnType(type = TypeKind.BOOLEAN)},
        isPublic = true
)
public class HasPrefix extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        String param1 = context.getStringArgument(0);
        String prefix = context.getStringArgument(1);

        BBoolean booleanValue = new BBoolean(param1.startsWith(prefix));
        context.setReturnValues(booleanValue);
    }

    public static boolean hasPrefix(Strand strand, String value, String prefix) {
        StringUtils.checkForNull(value, prefix);
        return value.startsWith(prefix);
    }
}
