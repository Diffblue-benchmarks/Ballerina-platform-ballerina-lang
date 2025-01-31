/**
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 **/

package org.ballerinalang.nativeimpl.builtin.jsonlib;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.util.exceptions.BLangExceptionHelper;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.util.JSONUtils;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.nativeimpl.lang.utils.ErrorHandler;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.util.exceptions.BallerinaErrorReasons;

/**
 * Remove the element(s) that matches the given key.
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "builtin",
        functionName = "json.remove",
        args = {@Argument(name = "j", type = TypeKind.JSON),
                @Argument(name = "key", type = TypeKind.STRING)},
        isPublic = true
)
public class Remove extends BlockingNativeCallableUnit {

    private static final String OPERATION = "remove element from json";

    @Override
    public void execute(Context ctx) {
        try {
            // Accessing Parameters.
            BValue json = ctx.getNullableRefArgument(0);

            // Removing the element
            if (json != null) {
                String fieldName = ctx.getStringArgument(0);
                JSONUtils.remove(json, fieldName);
            }
        } catch (Throwable e) {
            ErrorHandler.handleJsonException(BallerinaErrorReasons.JSON_OPERATION_ERROR, OPERATION, e);
        }

        ctx.setReturnValues();
    }

    public static void remove(Strand strand, Object json, String fieldName) {
        if (json == null) {
            return;
        }

        try {
            org.ballerinalang.jvm.JSONUtils.remove(json, fieldName);
        } catch (Throwable e) {
            throw BLangExceptionHelper.getJsonError(BallerinaErrorReasons.JSON_OPERATION_ERROR, OPERATION, e);
        }
    }
}
