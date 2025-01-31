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

package org.ballerinalang.test.auth;

import org.ballerinalang.test.BaseTest;
import org.ballerinalang.test.context.BServerInstance;
import org.ballerinalang.test.util.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;

import java.io.File;

/**
 * Base test class for Auth integration test cases which starts/stops the Auth services as ballerina packages before
 * and after tests are run.
 */
public class AuthBaseTest extends BaseTest {

    protected static BServerInstance serverInstance;
    private static EmbeddedDirectoryServer embeddedDirectoryServer;

    @BeforeGroups(value = "auth-test", alwaysRun = true)
    public void start() throws Exception {
        int[] requiredPorts = new int[]{9090, 9091, 9092, 9093, 9094, 9095, 9096, 9097, 9098, 9099, 9100, 9101, 9102,
                9103, 9104, 9105, 9106, 9107, 9108, 9109, 9110, 9111, 9112, 9113, 9114, 9115, 9116, 9190, 9195, 9196};
        embeddedDirectoryServer = new EmbeddedDirectoryServer();
        embeddedDirectoryServer.startLdapServer(9389);

        String basePath = new File("src" + File.separator + "test" + File.separator + "resources" + File.separator +
                "auth").getAbsolutePath();
        String ballerinaConfPath = basePath + File.separator + "ballerina.conf";
        String[] args = new String[]{"--config", ballerinaConfPath};
        serverInstance = new BServerInstance(balServer);
        serverInstance.startServer(basePath, "authservices", args, requiredPorts);
    }

    @AfterGroups(value = "auth-test", alwaysRun = true)
    public void cleanup() throws Exception {
        embeddedDirectoryServer.stopLdapService();
        serverInstance.removeAllLeechers();
        serverInstance.shutdownServer();
    }

    void assertOK(HttpResponse response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    void assertUnauthorized(HttpResponse response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 401, "Response code mismatched");
    }

    void assertForbidden(HttpResponse response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 403, "Response code mismatched");
    }

    void assertContains(HttpResponse response, String text) {
        Assert.assertTrue(response.getData().contains(text));
    }
}
