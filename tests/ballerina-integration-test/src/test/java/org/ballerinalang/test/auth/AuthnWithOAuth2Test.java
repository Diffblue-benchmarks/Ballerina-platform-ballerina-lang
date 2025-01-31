/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.test.auth;

import org.ballerinalang.test.util.HttpResponse;
import org.ballerinalang.test.util.HttpsClientRequest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Test cases for inbound authentication with OAuth2.
 */
@Test(groups = "auth-test")
public class AuthnWithOAuth2Test extends AuthBaseTest {

    private final int servicePort = 9116;

    @Test(description = "Test inbound OAuth2 success with valid token")
    public void testOAuth2SuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer 2YotnFZFEjr1zCsicMWpAA");
        HttpResponse response = HttpsClientRequest.doGet(serverInstance.getServiceURLHttps(servicePort, "echo/test"),
                headers, serverInstance.getServerHome());
        assertOK(response);
    }

    @Test(description = "Test inbound OAuth2 failure with invalid token")
    public void testOAuth2FailureTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer invalid_token");
        HttpResponse response = HttpsClientRequest.doGet(serverInstance.getServiceURLHttps(servicePort, "echo/test"),
                headers, serverInstance.getServerHome());
        assertUnauthorized(response);
    }
}
