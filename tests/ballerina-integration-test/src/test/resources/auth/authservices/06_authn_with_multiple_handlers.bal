// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/http;
import ballerina/jwt;

jwt:JWTAuthProvider jwtAuthProvider06_1 = new({
    issuer: "example1",
    audience: ["ballerina"],
    certificateAlias: "ballerina",
    trustStore: {
        path: "${ballerina.home}/bre/security/ballerinaTruststore.p12",
        password: "ballerina"
    }
});

jwt:JWTAuthProvider jwtAuthProvider06_2 = new({
    issuer: "example2",
    audience: ["ballerina"],
    certificateAlias: "ballerina",
    trustStore: {
        path: "${ballerina.home}/bre/security/ballerinaTruststore.p12",
        password: "ballerina"
    }
});


http:BearerAuthHeaderAuthnHandler jwtAuthnHandler06_1 = new(jwtAuthProvider06_1);
http:BearerAuthHeaderAuthnHandler jwtAuthnHandler06_2 = new(jwtAuthProvider06_2);

listener http:Listener listener06 = new(9097, config = {
    auth: {
        authnHandlers: [jwtAuthnHandler06_1, jwtAuthnHandler06_2]
    },
    secureSocket: {
        keyStore: {
            path: "${ballerina.home}/bre/security/ballerinaKeystore.p12",
            password: "ballerina"
        }
    }
});

@http:ServiceConfig {
    basePath: "/echo"
}
service echo06 on listener06 {

    resource function test(http:Caller caller, http:Request req) {
        checkpanic caller->respond(());
    }
}
