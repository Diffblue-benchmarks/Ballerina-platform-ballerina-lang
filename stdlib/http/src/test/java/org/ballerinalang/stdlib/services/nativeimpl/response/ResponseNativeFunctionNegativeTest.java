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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.stdlib.services.nativeimpl.response;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import org.ballerinalang.model.values.BError;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.stdlib.utils.TestEntityUtils;
import org.ballerinalang.test.util.BAssertUtil;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.ballerinalang.mime.util.MimeConstants.APPLICATION_JSON;
import static org.ballerinalang.mime.util.MimeConstants.APPLICATION_XML;
import static org.ballerinalang.mime.util.MimeConstants.ENTITY_HEADERS;
import static org.ballerinalang.mime.util.MimeConstants.IS_BODY_BYTE_CHANNEL_ALREADY_SET;
import static org.ballerinalang.mime.util.MimeConstants.PROTOCOL_PACKAGE_MIME;
import static org.ballerinalang.mime.util.MimeConstants.RESPONSE_ENTITY_FIELD;
import static org.ballerinalang.mime.util.MimeConstants.TEXT_PLAIN;

/**
 * Test cases for ballerina/http inbound response negative native functions.
 */
public class ResponseNativeFunctionNegativeTest {

    private CompileResult resultOnBVM, result, resultNegative;
    private final String inRespStruct = HttpConstants.RESPONSE;
    private final String entityStruct = HttpConstants.ENTITY;
    private final String protocolPackageHttp = HttpConstants.PROTOCOL_PACKAGE_HTTP;
    private final String protocolPackageMime = PROTOCOL_PACKAGE_MIME;

    @BeforeClass
    public void setup() {
        String basePath = "test-src/services/nativeimpl/response/";
        resultOnBVM = BCompileUtil.compileOnBVM(basePath + "in-response-native-function-negative.bal");
        result = BCompileUtil.compile(basePath + "in-response-native-function-negative.bal");
        resultNegative = BCompileUtil.compile(basePath + "in-response-compile-negative.bal");
    }

    @Test
    public void testGetHeader() {
        try {
            BMap<String, BValue> inResponse =
                    BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageHttp, inRespStruct);
            BString key = new BString(HttpHeaderNames.CONTENT_TYPE.toString());
            BValue[] inputArg = { inResponse, key };
            BValue[] returnVals = BRunUtil.invoke(result, "testGetHeader", inputArg);
            Assert.assertNull(returnVals[0]);
        } catch (Exception exception) {
            String errorMessage = exception.getMessage();
            Assert.assertTrue(errorMessage.contains("error: Http Header does not exist!"));
        }
    }

    @Test(description = "Test method without json payload")
    public void testGetJsonPayloadWithoutPayload() {
        BMap<String, BValue> inResponse =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageHttp, inRespStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageMime, entityStruct);
        TestEntityUtils.enrichTestEntityHeaders(entity, APPLICATION_JSON);
        inResponse.put(RESPONSE_ENTITY_FIELD, entity);
        BValue[] inputArg = { inResponse };
        BValue[] returnVals = BRunUtil.invoke(result, "testGetJsonPayload", inputArg);
        Assert.assertNotNull(returnVals[0]);
        Assert.assertEquals(((BError) returnVals[0]).getDetails().stringValue(),
                "{\"message\":\"Error occurred while extracting json data from entity: Empty content\"}");
    }

    @Test(description = "Test method with string payload")
    public void testGetJsonPayloadWithStringPayload() {
        BMap<String, BValue> inResponse =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageHttp, inRespStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageMime, entityStruct);

        String payload = "ballerina";
        TestEntityUtils.enrichTestEntity(entity, TEXT_PLAIN, payload);
        inResponse.put(RESPONSE_ENTITY_FIELD, entity);
        inResponse.addNativeData(IS_BODY_BYTE_CHANNEL_ALREADY_SET, true);
        BValue[] inputArg = { inResponse };
        BValue[] returnVals = BRunUtil.invoke(result, "testGetJsonPayload", inputArg);
        Assert.assertNotNull(returnVals[0]);
        Assert.assertEquals(((BError) returnVals[0]).getDetails().stringValue(),
                "{\"message\":\"Error occurred while extracting json data from entity: " +
                        "unrecognized token 'ballerina' at line: 1 column: 11\"}");
    }

    @Test(description = "Test getTextPayload method without a payload")
    public void testGetTextPayloadNegative() {
        BMap<String, BValue> inResponse =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageHttp, inRespStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageMime, entityStruct);
        TestEntityUtils.enrichTestEntityHeaders(entity, TEXT_PLAIN);
        inResponse.put(RESPONSE_ENTITY_FIELD, entity);
        BValue[] inputArg = { inResponse };
        BValue[] returnVals = BRunUtil.invoke(result, "testGetTextPayload", inputArg);
        Assert.assertFalse(returnVals == null || returnVals.length == 0 || returnVals[0] == null,
                "Invalid Return Values.");
        Assert.assertTrue(returnVals[0].stringValue()
                .contains("Error occurred while extracting text data from entity : Empty content"));
    }

    @Test
    public void testGetXmlPayloadNegative() {
        BMap<String, BValue> inResponse =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageHttp, inRespStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageMime, entityStruct);
        TestEntityUtils.enrichTestEntityHeaders(entity, APPLICATION_XML);
        inResponse.put(RESPONSE_ENTITY_FIELD, entity);
        BValue[] inputArg = { inResponse };
        BValue[] returnVals = BRunUtil.invoke(result, "testGetXmlPayload", inputArg);
        Assert.assertEquals(((BError) returnVals[0]).getDetails().stringValue(),
                "{\"message\":\"Error occurred while extracting xml data from entity : Empty content\"}");
    }

    @Test
    public void testGetXmlPayloadWithStringPayload() {
        BMap<String, BValue> inResponse =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageHttp, inRespStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageMime, entityStruct);

        String payload = "ballerina";
        TestEntityUtils.enrichTestEntity(entity, TEXT_PLAIN, payload);
        inResponse.put(RESPONSE_ENTITY_FIELD, entity);
        inResponse.addNativeData(IS_BODY_BYTE_CHANNEL_ALREADY_SET, true);
        BValue[] inputArg = { inResponse };
        BValue[] returnVals = BRunUtil.invoke(result, "testGetXmlPayload", inputArg);
        Assert.assertNotNull(returnVals[0]);
        Assert.assertEquals(((BError) returnVals[0]).getDetails().stringValue(),
                            "{\"message\":\"Error occurred while extracting xml data from entity : "
                                    + "Unexpected character 'b' (code 98) in prolog; expected '<'\n"
                                    + " at [row,col {unknown-source}]: [1,1]\"}");
    }

    @Test(description = "Test getEntity method on a response without a entity")
    public void testGetEntityNegative() {
        BMap<String, BValue> outResponse =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageHttp, inRespStruct);
        BValue[] inputArg = { outResponse };
        BValue[] returnVals = BRunUtil.invoke(result, "testGetEntity", inputArg);
        Assert.assertFalse(returnVals == null || returnVals.length == 0 || returnVals[0] == null,
                "Invalid Return Values.");
        Assert.assertNotNull(returnVals[0]);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRemoveHeaderNegative() {
        BMap<String, BValue> outResponse =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageHttp, inRespStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageMime, entityStruct);
        String range = "Range";
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add("Expect", "100-continue");
        entity.addNativeData(ENTITY_HEADERS, httpHeaders);
        outResponse.put(RESPONSE_ENTITY_FIELD, entity);
        BString key = new BString(range);
        BValue[] inputArg = { outResponse, key };
        BValue[] returnVals = BRunUtil.invoke(result, "testRemoveHeader", inputArg);

        Assert.assertFalse(returnVals == null || returnVals.length == 0 || returnVals[0] == null,
                "Invalid Return Values.");
        Assert.assertTrue(returnVals[0] instanceof BMap);
        BMap<String, BValue> entityStruct =
                (BMap<String, BValue>) ((BMap<String, BValue>) returnVals[0]).get(RESPONSE_ENTITY_FIELD);
        HttpHeaders returnHeaders = (HttpHeaders) entityStruct.getNativeData(ENTITY_HEADERS);
        Assert.assertNull(returnHeaders.get("range"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRemoveAllHeadersNegative() {
        BMap<String, BValue> outResponse =
                BCompileUtil.createAndGetStruct(resultOnBVM.getProgFile(), protocolPackageHttp, inRespStruct);
        BValue[] inputArg = { outResponse };
        BValue[] returnVals = BRunUtil.invoke(result, "testRemoveAllHeaders", inputArg);
        Assert.assertFalse(returnVals == null || returnVals.length == 0 || returnVals[0] == null,
                "Invalid Return Values.");
        Assert.assertTrue(returnVals[0] instanceof BMap);
        BMap<String, BValue> entityStruct =
                (BMap<String, BValue>) ((BMap<String, BValue>) returnVals[0]).get(RESPONSE_ENTITY_FIELD);
        HttpHeaders returnHeaders = (HttpHeaders) entityStruct.getNativeData(ENTITY_HEADERS);
        Assert.assertNull(returnHeaders.get("Expect"));
    }

    @Test
    public void testCompilationErrorTestCases() {
        Assert.assertEquals(resultNegative.getErrorCount(), 2);
        // testInResponseSetStatusCodeWithString
        BAssertUtil.validateError(resultNegative, 0, "incompatible types: expected 'int', found 'string'", 4, 22);
        // testInResponseGetMethod
        BAssertUtil.validateError(resultNegative, 1, "undefined field 'method' in object 'ballerina/http:Response'", 9,
                21);
    }
}
