/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.stdlib.internal.file;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMErrors;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.jvm.BallerinaErrors;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.stdlib.internal.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.ballerinalang.bre.bvm.BLangVMErrors.STRUCT_GENERIC_ERROR;
import static org.ballerinalang.util.BLangConstants.BALLERINA_BUILTIN_PKG;

/**
 * Deletes a given file or a directory.
 *
 * @since 0.970.0-alpha1
 */
@BallerinaFunction(
        orgName = Constants.ORG_NAME,
        packageName = Constants.PACKAGE_NAME,
        functionName = "delete",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = Constants.PATH_STRUCT,
                             structPackage = Constants.PACKAGE_PATH)
        ,
        returnType = {
                @ReturnType(type = TypeKind.OBJECT, structType = STRUCT_GENERIC_ERROR,
                            structPackage = BALLERINA_BUILTIN_PKG)
        },
        isPublic = true
)
public class Delete extends BlockingNativeCallableUnit {
    private static final Logger log = LoggerFactory.getLogger(Delete.class);
    
    @Override
    public void execute(Context context) {
        BMap<String, BValue> pathStruct = (BMap<String, BValue>) context.getRefArgument(0);
        Path path = (Path) pathStruct.getNativeData(Constants.PATH_DEFINITION_NAME);
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            if (Files.exists(path)) {
                String msg = "File/Directory could not be properly deleted: " + path;
                log.error(msg);
                context.setReturnValues(BLangVMErrors.createError(context, msg));
            }
        } catch (IOException ex) {
            String msg = "IO error occurred while deleting file/directory: " + path;
            log.error(msg, ex);
            context.setReturnValues(BLangVMErrors.createError(context, msg));
        }
    }

    public static Object delete(Strand strand, ObjectValue self) {
        Path path = (Path) self.getNativeData(Constants.PATH_DEFINITION_NAME);
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            if (Files.exists(path)) {
                String msg = "File/Directory could not be properly deleted: " + path;
                log.error(msg);
                return BallerinaErrors.createError(msg);
            }
        } catch (IOException ex) {
            String msg = "IO error occurred while deleting file/directory: " + path;
            log.error(msg, ex);
            return BallerinaErrors.createError(msg);
        }
        return null;
    }
    
}
