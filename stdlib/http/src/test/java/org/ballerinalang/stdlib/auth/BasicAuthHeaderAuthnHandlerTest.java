/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.stdlib.auth;

import org.ballerinalang.config.ConfigRegistry;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Basic auth header authentication handler testcase.
 */
public class BasicAuthHeaderAuthnHandlerTest {

    private static final String BALLERINA_CONF = "ballerina.conf";
    private CompileResult compileResult;
    private Path secretCopyPath;

    @BeforeClass
    public void setup() throws IOException {
        String resourceRoot = Paths.get("src", "test", "resources").toAbsolutePath().toString();
        Path sourceRoot = Paths.get(resourceRoot, "test-src", "auth");
        Path ballerinaConfPath = Paths.get(resourceRoot, "datafiles", "config", "authprovider", BALLERINA_CONF);

        // Copy the ballerina.conf to the source root before starting the tests
        compileResult = BCompileUtil.compile(sourceRoot.resolve("basic-auth-header-authn-handler-test.bal").toString());

        String secretFile = "secret.txt";
        Path secretFilePath = Paths.get(resourceRoot, "datafiles", "config", secretFile);
        secretCopyPath = Paths.get(resourceRoot, "datafiles", "config", "authprovider", secretFile);
        Files.deleteIfExists(secretCopyPath);
        copySecretFile(secretFilePath.toString(), secretCopyPath.toString());

        // load configs
        ConfigRegistry registry = ConfigRegistry.getInstance();
        registry.initRegistry(Collections.singletonMap("b7a.config.secret", secretCopyPath.toString()),
                ballerinaConfPath.toString(), null);
    }

    private void copySecretFile(String from, String to) throws IOException {
        Files.copy(Paths.get(from), Paths.get(to));
    }

    @Test(description = "Test case for basic auth header interceptor canHandle method, without the basic auth header")
    public void testCanHandleHttpBasicAuthWithoutHeader() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testCanHandleHttpBasicAuthWithoutHeader");
        Assert.assertTrue(returns[0] instanceof BBoolean);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    @Test(description = "Test case for basic auth header interceptor canHandle method")
    public void testCanHandleHttpBasicAuth() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testCanHandleHttpBasicAuth");
        Assert.assertTrue(returns[0] instanceof BBoolean);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test(description = "Test case for basic auth header interceptor authentication failure")
    public void testHandleHttpBasicAuthFailure() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testHandleHttpBasicAuthFailure");
        Assert.assertTrue(returns[0] instanceof BBoolean);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    @Test(description = "Test case for basic auth header interceptor authentication success")
    public void testHandleHttpBasicAuth() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testHandleHttpBasicAuth");
        Assert.assertTrue(returns[0] instanceof BBoolean);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test(description = "Test case for extracting basic auth header value")
    public void testExtractBasicAuthHeaderValue() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testExtractBasicAuthHeaderValue");
        Assert.assertTrue(returns[0] instanceof BString);
        // no error should be returned
        Assert.assertEquals(returns[0].stringValue(), "Basic aXN1cnU6eHh4");
    }

    @AfterClass
    public void tearDown() throws IOException {
        Files.deleteIfExists(secretCopyPath);
    }
}
