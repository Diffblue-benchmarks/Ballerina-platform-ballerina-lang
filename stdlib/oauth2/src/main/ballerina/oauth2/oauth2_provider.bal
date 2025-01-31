// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/auth;
import ballerina/config;
import ballerina/http;
import ballerina/mime;
import ballerina/runtime;

# Represents Ballerina OAuth2 provider, which calls the introspection server and validate the received credentials.
#
# + introspectionClient - Introspection client endpoint
# + tokenTypeHint - A hint about the type of the token submitted for introspection
public type OAuth2Provider object {

    *auth:AuthProvider;

    public http:Client introspectionClient;
    public string? tokenTypeHint;

    public function __init(IntrospectionServerConfig config) {
        self.tokenTypeHint = config["tokenTypeHint"];
        self.introspectionClient = new(config.url, config = config.clientConfig);
    }

    # Attempts to authenticate with credential.
    #
    # + credential - Credential
    # + return - `true` if authentication is successful, otherwise `false` or `error` if an error occurred
    public function authenticate(string credential) returns boolean|error {
        if (credential == "") {
            return false;
        }
        boolean authenticated = false;
        string username = "";
        string scopes = "";

        // Build the request to be send to the introspection endpoint.
        // Refer: https://tools.ietf.org/html/rfc7662#section-2.1
        http:Request req = new;
        string textPayload = "token=" + credential;
        var tokenTypeHint = self.tokenTypeHint;
        if (tokenTypeHint is string) {
            textPayload += "&token_type_hint=" + tokenTypeHint;
        }
        req.setTextPayload(textPayload, contentType = mime:APPLICATION_FORM_URLENCODED);
        var response = self.introspectionClient->post("", req);
        if (response is http:Response) {
            json payload = check response.getJsonPayload();
            boolean active = <boolean>payload.active;
            if (active) {
                authenticated = true;
                if (payload.username is string) {
                    username = <string>payload.username;
                }
                if (payload.scope is string) {
                    scopes = <string>payload.scope;
                }
            }
        } else {
            return response;
        }

        if (authenticated) {
            runtime:Principal principal = runtime:getInvocationContext().principal;
            principal.userId = username;
            principal.username = username;
            principal.scopes = self.getScopes(scopes);
        }
        return authenticated;
    }

    # Reads the scope(s) for the user with the given username.
    #
    # + scopes - Set of scopes seperated with a space
    # + return - Array of groups for the user denoted by the username
    public function getScopes(string scopes) returns string[] {
        return scopes.trim().split(" ");
    }
};

# Represents introspection server onfigurations.
#
# + url - URL of the introspection server
# + tokenTypeHint - A hint about the type of the token submitted for introspection
# + clientConfig - HTTP client configurations which calls the introspection server
public type IntrospectionServerConfig record {|
    string url;
    string tokenTypeHint?;
    http:ClientEndpointConfig clientConfig;
|};
