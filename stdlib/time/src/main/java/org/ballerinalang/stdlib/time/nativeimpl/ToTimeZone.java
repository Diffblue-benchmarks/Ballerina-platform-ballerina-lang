/*
*   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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
package org.ballerinalang.stdlib.time.nativeimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.jvm.Strand;
import org.ballerinalang.jvm.values.ErrorValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.stdlib.time.util.TimeUtils;
import org.ballerinalang.util.exceptions.BallerinaException;

/**
 * Change the timezone associated with the given time.
 *
 * @since 0.89
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "time",
        functionName = "toTimeZone"
)
public class ToTimeZone extends AbstractTimeFunction {

    @Override
    public void execute(Context context) {
        BMap<String, BValue> timeStruct = ((BMap<String, BValue>) context.getRefArgument(0));
        String zoneId = context.getStringArgument(0);
        try {
            context.setReturnValues(changeTimezone(context, timeStruct, zoneId));
        } catch (BallerinaException e) {
            context.setReturnValues(TimeUtils.getTimeError(context, e.getMessage()));
        }
    }

    public static Object toTimeZone(Strand strand, MapValue<String, Object> timeRecord, String zoneId) {
        try {
            return changeTimezone(timeRecord, zoneId);
        } catch (ErrorValue e) {
            return e;
        }
    }
}
