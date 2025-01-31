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
 */

package org.ballerinalang.stdlib.jwt;

import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BError;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.stdlib.crypto.Constants;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Test JWT token creation and verification.
 */
public class JwtTest {

    private String resourceRoot;
    private CompileResult compileResult;
    private String jwtToken;
    private String jwtTokenWithoutIssAndSub;
    private String jwtTokenWithoutAudAndSub;
    private String keyStorePath;
    private String trustStorePath;

    @BeforeClass
    public void setup() {
        keyStorePath = Paths.get("src", "test", "resources", "datafiles", 
                "keystore", "ballerinaKeystore.p12").toAbsolutePath().toString();
        trustStorePath = Paths.get("src", "test", "resources", "datafiles", 
            "keystore", "ballerinaTruststore.p12").toAbsolutePath().toString();
        resourceRoot = Paths.get("src", "test", "resources").toAbsolutePath().toString();
        Path sourceRoot = Paths.get(resourceRoot, "test-src");
        compileResult = BCompileUtil.compile(sourceRoot.resolve("jwt-test.bal").toString());
    }

    @Test(priority = 1, description = "Test case for issuing JWT token with valid data")
    public void testIssueJwt() {
        BValue[] inputBValues = {new BString(keyStorePath)};
        BValue[] returns = BRunUtil.invoke(compileResult, "testIssueJwt", inputBValues);
        Assert.assertTrue(returns[0] instanceof BString);
        jwtToken = returns[0].stringValue();
        String[] parts = jwtToken.split("\\.");
        String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Assert.assertEquals("{\"alg\":\"RS256\", \"typ\":\"JWT\"}", header);
        Assert.assertTrue(payload.startsWith("{\"sub\":\"John\", \"iss\":\"wso2\", \""));
        Assert.assertTrue(payload.endsWith("\", \"aud\":[\"ballerina\", \"ballerinaSamples\"]}"));
    }

    @Test(priority = 1, description = "Test case for issuing JWT token with valid data and a single audience")
    public void testIssueJwtWithSingleAud() {
        BValue[] inputBValues = {new BString(keyStorePath)};
        BValue[] returns = BRunUtil.invoke(compileResult, "testIssueJwtWithSingleAud", inputBValues);
        Assert.assertTrue(returns[0] instanceof BString);
        String localJwtToken = returns[0].stringValue();
        String[] parts = localJwtToken.split("\\.");
        String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Assert.assertEquals("{\"alg\":\"RS256\", \"typ\":\"JWT\"}", header);
        Assert.assertTrue(payload.startsWith("{\"sub\":\"John\", \"iss\":\"wso2\", \""));
        Assert.assertTrue(payload.endsWith("\", \"aud\":\"ballerina\"}"));
    }

    @Test(priority = 1, description = "Test case for issuing JWT token with valid data and a single audience, " +
            "but with audienceAsArray enabled")
    public void testIssueJwtWithSingleAudAndAudAsArray() {
        BValue[] inputBValues = {new BString(keyStorePath)};
        BValue[] returns = BRunUtil.invoke(compileResult, "testIssueJwtWithSingleAudAndAudAsArray", inputBValues);
        Assert.assertTrue(returns[0] instanceof BString);
        String localJwtToken = returns[0].stringValue();
        String[] parts = localJwtToken.split("\\.");
        String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Assert.assertEquals("{\"alg\":\"RS256\", \"typ\":\"JWT\"}", header);
        Assert.assertTrue(payload.startsWith("{\"sub\":\"John\", \"iss\":\"wso2\", \""));
        Assert.assertTrue(payload.endsWith("\", \"aud\":[\"ballerina\"]}"));
    }

    @Test(priority = 1, description = "Test case for issuing JWT token without issuer or subject")
    public void testIssueJwtWithNoIssOrSub() {
        BValue[] inputBValues = {new BString(keyStorePath)};
        BValue[] returns = BRunUtil.invoke(compileResult, "testIssueJwtWithNoIssOrSub", inputBValues);
        Assert.assertTrue(returns[0] instanceof BString);
        jwtTokenWithoutIssAndSub = returns[0].stringValue();
        String[] parts = jwtTokenWithoutIssAndSub.split("\\.");
        String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Assert.assertEquals("{\"alg\":\"RS256\", \"typ\":\"JWT\"}", header);
        Assert.assertTrue(payload.startsWith("{\"exp\":"));
        Assert.assertTrue(payload.endsWith("\", \"aud\":[\"ballerina\", \"ballerinaSamples\"]}"));
    }

    @Test(priority = 1, description = "Test case for issuing JWT token without issuer or audience")
    public void testIssueJwtWithNoAudOrSub() {
        BValue[] inputBValues = {new BString(keyStorePath)};
        BValue[] returns = BRunUtil.invoke(compileResult, "testIssueJwtWithNoAudOrSub", inputBValues);
        Assert.assertTrue(returns[0] instanceof BString);
        jwtTokenWithoutAudAndSub = returns[0].stringValue();
        String[] parts = jwtTokenWithoutAudAndSub.split("\\.");
        String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Assert.assertEquals("{\"alg\":\"RS256\", \"typ\":\"JWT\"}", header);
        Assert.assertTrue(payload.startsWith("{\"sub\":\"John\", \"iss\":\"wso2\", \"exp\":"));
        Assert.assertTrue(payload.endsWith("\"}"));
    }

    @Test(priority = 2, description = "Test case for validating JWT token")
    public void testCompleteValidator() {
        BValue[] inputBValues = {new BString(jwtToken), new BString(trustStorePath)};
        BValue[] returns = BRunUtil.invoke(compileResult, "testValidateJwt", inputBValues);
        Assert.assertTrue((returns[0]) instanceof BBoolean);
    }

    @Test(priority = 2, description = "Test case for validating JWT token without issuer or subject information, " +
            "using a validator configured to validate issuer and subject")
    public void testCompleteValidatorWithNoIssOrSubNegative() {
        BValue[] inputBValues = {new BString(jwtTokenWithoutIssAndSub), new BString(trustStorePath)};
        BValue[] returns = BRunUtil.invoke(compileResult, "testValidateJwt", inputBValues);
        Assert.assertTrue((returns[0]) instanceof BError);
        Assert.assertEquals(((BMap) ((BError) returns[0]).getDetails()).get(Constants.MESSAGE).stringValue(),
                "JWT must contain a valid issuer name.");
    }

    @Test(priority = 2, description = "Test case for validating JWT token without issuer or subject information, " +
            "using a validator configured to validate audience and subject")
    public void testCompleteValidatorWithNoAudOrSubNegative() {
        BValue[] inputBValues = {new BString(jwtTokenWithoutAudAndSub), new BString(trustStorePath)};
        BValue[] returns = BRunUtil.invoke(compileResult, "testValidateJwt", inputBValues);
        Assert.assertTrue((returns[0]) instanceof BError);
        Assert.assertEquals(((BMap) ((BError) returns[0]).getDetails()).get(Constants.MESSAGE).stringValue(),
                "JWT must contain a valid audience.");
    }

    @Test(priority = 2, description = "Test case for validating JWT token without issuer or subject information, " +
            "using a validator configured not to validate issuer and subject")
    public void testPartialValidatorWithNoIssOrSub() {
        BValue[] inputBValues = {new BString(jwtTokenWithoutIssAndSub), new BString(trustStorePath)};
        BValue[] returns = BRunUtil.invoke(compileResult, "testValidateJwtWithNoIssOrSub", inputBValues);
        Assert.assertTrue((returns[0]) instanceof BBoolean);
    }

    @Test(priority = 2, description = "Test case for validating JWT token without issuer or subject information, " +
            "using a validator configured to validate issuer and subject")
    public void testPartialValidatorWithIssAndSub() {
        BValue[] inputBValues = {new BString(jwtToken), new BString(trustStorePath)};
        BValue[] returns = BRunUtil.invoke(compileResult, "testValidateJwtWithNoIssOrSub", inputBValues);
        Assert.assertTrue((returns[0]) instanceof BBoolean);
    }
}
