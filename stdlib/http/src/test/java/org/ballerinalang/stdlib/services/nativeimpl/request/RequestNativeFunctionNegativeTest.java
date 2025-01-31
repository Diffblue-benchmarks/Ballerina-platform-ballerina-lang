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
package org.ballerinalang.stdlib.services.nativeimpl.request;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import org.ballerinalang.model.values.BError;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.stdlib.utils.TestEntityUtils;
import org.ballerinalang.test.util.BAssertUtil;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import static org.ballerinalang.mime.util.MimeConstants.APPLICATION_JSON;
import static org.ballerinalang.mime.util.MimeConstants.APPLICATION_XML;
import static org.ballerinalang.mime.util.MimeConstants.ENTITY_HEADERS;
import static org.ballerinalang.mime.util.MimeConstants.IS_BODY_BYTE_CHANNEL_ALREADY_SET;
import static org.ballerinalang.mime.util.MimeConstants.PROTOCOL_PACKAGE_MIME;
import static org.ballerinalang.mime.util.MimeConstants.REQUEST_ENTITY_FIELD;
import static org.ballerinalang.mime.util.MimeConstants.TEXT_PLAIN;

/**
 * Test cases for ballerina/http request negative native functions.
 */
public class RequestNativeFunctionNegativeTest {

    private CompileResult bvmResult, compileResultNegative, compileResult;
    private final String reqStruct = HttpConstants.REQUEST;
    private final String entityStruct = HttpConstants.ENTITY;
    private final String protocolPackageHttp = HttpConstants.PROTOCOL_PACKAGE_HTTP;
    private final String protocolPackageMime = PROTOCOL_PACKAGE_MIME;

    @BeforeClass
    public void setup() {
        String basePath = "test-src/services/nativeimpl/request/";
        bvmResult = BCompileUtil.compileOnBVM(basePath + "in-request-native-function-negative.bal");
        compileResult = BCompileUtil.compile(basePath + "in-request-native-function-negative.bal");
        compileResultNegative = BCompileUtil.compile(basePath + "in-request-compile-negative.bal");
    }

    @Test(description = "Test when the content length header is not set")
    public void testGetContentLength() {
        BMap<String, BValue> inRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        BValue[] inputArg = { inRequest };
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testGetContentLength", inputArg);
        Assert.assertEquals(returnVals[0].stringValue(), "Content-length is not found");
    }

    @Test
    public void testGetHeader() {
        BMap<String, BValue> inRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        BString key = new BString(HttpHeaderNames.CONTENT_TYPE.toString());
        BValue[] inputArg = {inRequest, key};
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testGetHeader", inputArg);
        Assert.assertNotNull(returnVals[0]);
        Assert.assertEquals(((BString) returnVals[0]).value(), "Header not found!");
    }

    @Test(description = "Test method without json payload")
    public void testGetJsonPayloadWithoutPayload() {
        BMap<String, BValue> inRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageMime, entityStruct);
        TestEntityUtils.enrichTestEntityHeaders(entity, APPLICATION_JSON);
        inRequest.put(REQUEST_ENTITY_FIELD, entity);
        BValue[] inputArg = {inRequest};
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testGetJsonPayload", inputArg);
        Assert.assertNotNull(returnVals[0]);
        Assert.assertEquals(((BError) returnVals[0]).getDetails().stringValue(),
                            "{\"message\":\"Error occurred while extracting json data from entity: Empty content\"}");
    }

    @Test(description = "Test method with string payload")
    public void testGetJsonPayloadWithStringPayload() {
        BMap<String, BValue> inRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageMime, entityStruct);

        String payload = "ballerina";
        TestEntityUtils.enrichTestEntity(entity, TEXT_PLAIN, payload);
        inRequest.put(REQUEST_ENTITY_FIELD, entity);
        inRequest.addNativeData(IS_BODY_BYTE_CHANNEL_ALREADY_SET, true);

        BValue[] inputArg = {inRequest};
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testGetJsonPayload", inputArg);
        Assert.assertNotNull(returnVals[0]);
        Assert.assertEquals(((BError) returnVals[0]).getDetails().stringValue(),
                            "{\"message\":\"Error occurred while extracting json data from entity: " +
                                    "unrecognized token 'ballerina' at line: 1 column: 11\"}");
    }

    @Test(description = "Test getEntity method on a outRequest without a entity")
    public void testGetEntityNegative() {
        BMap<String, BValue> outRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        BValue[] inputArg = {outRequest};
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testGetEntity", inputArg);
        Assert.assertFalse(returnVals == null || returnVals.length == 0 || returnVals[0] == null,
                "Invalid Return Values.");
        Assert.assertNotNull(returnVals[0]);
    }

    @Test(description = "Test getTextPayload method without a payload")
    public void testGetTextPayloadNegative() {
        BMap<String, BValue> inRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageMime, entityStruct);
        TestEntityUtils.enrichTestEntityHeaders(entity, TEXT_PLAIN);
        inRequest.put(REQUEST_ENTITY_FIELD, entity);
        BValue[] inputArg = { inRequest };
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testGetTextPayload", inputArg);
        Assert.assertTrue(returnVals[0].stringValue()
                        .contains("Error occurred while extracting text data from entity : Empty content"));
    }

    @Test
    public void testGetXmlPayloadNegative() {
        BMap<String, BValue> inRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageMime, entityStruct);
        TestEntityUtils.enrichTestEntityHeaders(entity, APPLICATION_XML);
        inRequest.put(REQUEST_ENTITY_FIELD, entity);
        BValue[] inputArg = { inRequest };
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testGetXmlPayload", inputArg);
        Assert.assertEquals(((BError) returnVals[0]).getDetails().stringValue(),
                "{\"message\":\"Error occurred while extracting xml data from entity : Empty content\"}");
    }

    @Test
    public void testGetXmlPayloadWithStringPayload() {
        BMap<String, BValue> inRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageMime, entityStruct);

        String payload = "ballerina";
        TestEntityUtils.enrichTestEntity(entity, TEXT_PLAIN, payload);
        inRequest.put(REQUEST_ENTITY_FIELD, entity);
        inRequest.addNativeData(IS_BODY_BYTE_CHANNEL_ALREADY_SET, true);

        BValue[] inputArg = {inRequest};
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testGetXmlPayload", inputArg);
        Assert.assertNotNull(returnVals[0]);
        Assert.assertEquals(((BError) returnVals[0]).getDetails().stringValue(),
                            "{\"message\":\"Error occurred while extracting xml data from entity : " +
                                    "Unexpected character 'b' (code 98) in prolog; expected '<'"
                                    + System.lineSeparator() + " at [row,col {unknown-source}]: [1,1]\"}");
    }

    @Test
    public void testGetMethodNegative() {
        BMap<String, BValue> inRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        HttpCarbonMessage inRequestMsg = HttpUtil.createHttpCarbonMessage(true);
        HttpUtil.addCarbonMsg(inRequest, inRequestMsg);
        BValue[] inputArg = { inRequest };
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testGetMethod", inputArg);
        Assert.assertFalse(returnVals == null || returnVals.length == 0 || returnVals[0] == null,
                "Invalid Return Values.");
        Assert.assertEquals(returnVals[0].stringValue(), "");
    }

    @Test
    public void testGetRequestURLNegative() {
        BMap<String, BValue> inRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        HttpCarbonMessage inRequestMsg = HttpUtil.createHttpCarbonMessage(true);
        HttpUtil.addCarbonMsg(inRequest, inRequestMsg);
        BValue[] inputArg = {inRequest};
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testGetRequestURL", inputArg);
        Assert.assertFalse(returnVals == null || returnVals.length == 0 || returnVals[0] == null,
                "Invalid Return Values.");
        Assert.assertEquals(returnVals[0].stringValue(), "no url");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRemoveHeaderNegative() {
        BMap<String, BValue> outRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        BMap<String, BValue> entity =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageMime, entityStruct);
        String range = "Range";
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add("Expect", "100-continue");
        entity.addNativeData(ENTITY_HEADERS, httpHeaders);
        outRequest.put(REQUEST_ENTITY_FIELD, entity);
        BString key = new BString(range);
        BValue[] inputArg = {outRequest, key};
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testRemoveHeader", inputArg);

        Assert.assertFalse(returnVals == null || returnVals.length == 0 || returnVals[0] == null,
                "Invalid Return Values.");
        Assert.assertTrue(returnVals[0] instanceof BMap);
        BMap<String, BValue> entityStruct =
                (BMap<String, BValue>) ((BMap<String, BValue>) returnVals[0]).get(REQUEST_ENTITY_FIELD);
        HttpHeaders returnHeaders = (HttpHeaders) entityStruct.getNativeData(ENTITY_HEADERS);
        Assert.assertNull(returnHeaders.get(range));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRemoveAllHeadersNegative() {
        BMap<String, BValue> outRequest =
                BCompileUtil.createAndGetStruct(bvmResult.getProgFile(), protocolPackageHttp, reqStruct);
        BValue[] inputArg = {outRequest};
        BValue[] returnVals = BRunUtil.invoke(compileResult, "testRemoveAllHeaders", inputArg);
        Assert.assertFalse(returnVals == null || returnVals.length == 0 || returnVals[0] == null,
                "Invalid Return Values.");
        Assert.assertTrue(returnVals[0] instanceof BMap);
        BMap<String, BValue> entityStruct =
                (BMap<String, BValue>) ((BMap<String, BValue>) returnVals[0]).get(REQUEST_ENTITY_FIELD);
        HttpHeaders returnHeaders = (HttpHeaders) entityStruct.getNativeData(ENTITY_HEADERS);
        Assert.assertNull(returnHeaders);
    }

    @Test
    public void testCompilationErrorTestCases() {
        Assert.assertEquals(compileResultNegative.getErrorCount(), 2);
        //testRequestSetStatusCode
        BAssertUtil.validateError(compileResultNegative, 0,
                                  "undefined function 'setStatusCode' in object 'ballerina/http:Request'", 4, 5);
        BAssertUtil.validateError(compileResultNegative, 1,
                                  "undefined field 'statusCode' in object 'ballerina/http:Request'", 5, 5);
    }
}
