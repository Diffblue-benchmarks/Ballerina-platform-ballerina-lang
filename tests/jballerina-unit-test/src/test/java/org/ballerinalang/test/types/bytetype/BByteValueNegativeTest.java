/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.test.types.bytetype;

import org.ballerinalang.model.values.BError;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.test.util.BAssertUtil;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This test class will test the byte value negative test cases.
 */
public class BByteValueNegativeTest {

    private CompileResult result;

    @BeforeClass
    public void setup() {
        result = BCompileUtil.compile("test-src/types/byte/byte-value-runtime-negative.bal");
    }

    @Test(description = "Test byte value negative")
    public void testByteValueNegative() {
        CompileResult result = BCompileUtil.compile("test-src/types/byte/byte-value-negative.bal");
        Assert.assertEquals(result.getErrorCount(), 26);
        String msg1 = "incompatible types: expected 'byte', found 'int'";
        String msg2 = "incompatible types: expected 'byte', found 'float'";
        String msg3 = "incompatible types: expected 'byte', found 'string'";
        String msg4 = "incompatible types: expected 'byte', found 'byte|error'";
        String msg5 = "unnecessary condition: expression will always evaluate to 'true'";
        String msg6 = "invalid usage of the 'check' expression operator: no error type return in enclosing invokable";
        BAssertUtil.validateError(result, 0, msg1 , 2, 15);
        BAssertUtil.validateError(result, 1, msg1 , 3, 15);
        BAssertUtil.validateError(result, 2, msg1 , 4, 15);
        BAssertUtil.validateError(result, 3, msg1 , 5, 15);
        BAssertUtil.validateError(result, 4, msg2 , 6, 15);
        BAssertUtil.validateError(result, 5, msg2 , 7, 15);
        BAssertUtil.validateError(result, 6, msg2 , 8, 15);
        BAssertUtil.validateError(result, 7, msg1 , 9, 24);
        BAssertUtil.validateError(result, 8, msg1 , 10, 21);
        BAssertUtil.validateError(result, 9, msg2 , 11, 21);
        BAssertUtil.validateError(result, 10, msg1 , 11, 30);
        BAssertUtil.validateError(result, 11, msg1 , 11, 35);
        BAssertUtil.validateError(result, 12, msg2 , 12, 21);
        BAssertUtil.validateError(result, 13, msg2 , 12, 27);
        BAssertUtil.validateError(result, 14, msg1 , 12, 35);
        BAssertUtil.validateError(result, 15, msg1 , 15, 14);
        BAssertUtil.validateError(result, 16, msg2 , 18, 14);
        BAssertUtil.validateError(result, 17, msg3 , 21, 14);
        BAssertUtil.validateError(result, 18, msg4 , 24, 15);
        BAssertUtil.validateError(result, 19, msg4 , 27, 15);
        BAssertUtil.validateError(result, 20, msg4 , 30, 15);
        BAssertUtil.validateError(result, 22, msg5, 35, 60);
        BAssertUtil.validateError(result, 25, msg6, 45, 16);
    }

    @Test(description = "Test byte shift operators negative")
    public void invalidByteShiftOperators() {
        CompileResult result = BCompileUtil.compile("test-src/types/byte/byte-shift-operators-negative.bal");
        Assert.assertEquals(result.getErrorCount(), 8);
        String msg = "invalid shift operator";
        BAssertUtil.validateError(result, 0, msg , 2, 18);
        BAssertUtil.validateError(result, 1, msg , 3, 18);
        BAssertUtil.validateError(result, 2, msg , 4, 18);
        BAssertUtil.validateError(result, 3, msg , 5, 19);
        BAssertUtil.validateError(result, 4, msg , 6, 18);
        BAssertUtil.validateError(result, 5, msg , 6, 20);
        BAssertUtil.validateError(result, 6, msg , 7, 21);
        BAssertUtil.validateError(result, 7, msg , 7, 27);
    }

    @Test(description = "Test int to byte conversion negative")
    public void byteValueRuntimeNegative1() {
        BValue[] returnValue = BRunUtil.invoke(result, "invalidByteLiteral1", new BValue[]{});
        Assert.assertEquals(returnValue.length, 1);
        Assert.assertTrue(returnValue[0] instanceof BError);
        Assert.assertEquals(returnValue[0].stringValue(), "{ballerina}NumberConversionError {\"message\":" +
                "\"incompatible convert operation: 'int' value '-12' cannot be converted as 'byte'\"}");
    }

    @Test(description = "Test int to byte conversion negative")
    public void byteValueRuntimeNegative2() {
        BValue[] returnValue = BRunUtil.invoke(result, "invalidByteLiteral2", new BValue[]{});
        Assert.assertEquals(returnValue.length, 1);
        Assert.assertTrue(returnValue[0] instanceof BError);
        Assert.assertEquals(returnValue[0].stringValue(), "{ballerina}NumberConversionError {\"message\":" +
                "\"incompatible convert operation: 'int' value '-257' cannot be converted as 'byte'\"}");
    }

    @Test(description = "Test int to byte conversion negative")
    public void byteValueRuntimeNegative3() {
        BValue[] returnValue = BRunUtil.invoke(result, "invalidByteLiteral3", new BValue[]{});
        Assert.assertEquals(returnValue.length, 1);
        Assert.assertTrue(returnValue[0] instanceof BError);
        Assert.assertEquals(returnValue[0].stringValue(), "{ballerina}NumberConversionError {\"message\":" +
                "\"incompatible convert operation: 'int' value '12,345' cannot be converted as 'byte'\"}");
    }
}
