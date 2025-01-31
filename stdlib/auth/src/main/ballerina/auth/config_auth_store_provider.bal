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

import ballerina/config;
import ballerina/crypto;
import ballerina/encoding;
import ballerina/runtime;

const string CONFIG_USER_SECTION = "b7a.users";

# Represents Ballerina configuration file based auth store provider.
public type ConfigAuthStoreProvider object {

    *AuthProvider;

    # Attempts to authenticate with credential.
    #
    # + credential - Credential
    # + return - `true` if authentication is successful, otherwise `false` or `error` occurred while extracting credentials
    public function authenticate(string credential) returns boolean|error {
        if (credential == EMPTY_STRING) {
            return false;
        }
        string username;
        string password;
        [username, password] = check extractUsernameAndPassword(credential);
        string passwordFromConfig = readPassword(username);
        boolean authenticated = false;
        // This check is added to avoid having to go through multiple condition evaluations, when value is plain text.
        if (passwordFromConfig.hasPrefix(CONFIG_PREFIX)) {
            if (passwordFromConfig.hasPrefix(CONFIG_PREFIX_SHA256)) {
                authenticated = encoding:encodeHex(crypto:hashSha256(password.toByteArray(DEFAULT_CHARSET)))
                                    .equalsIgnoreCase(extractHash(passwordFromConfig));
            } else if (passwordFromConfig.hasPrefix(CONFIG_PREFIX_SHA384)) {
                authenticated = encoding:encodeHex(crypto:hashSha384(password.toByteArray(DEFAULT_CHARSET)))
                                    .equalsIgnoreCase(extractHash(passwordFromConfig));
            } else if (passwordFromConfig.hasPrefix(CONFIG_PREFIX_SHA512)) {
                authenticated = encoding:encodeHex(crypto:hashSha512(password.toByteArray(DEFAULT_CHARSET)))
                                    .equalsIgnoreCase(extractHash(passwordFromConfig));
            } else {
                authenticated = password == passwordFromConfig;
            }
        } else {
            authenticated = password == passwordFromConfig;
        }
        if (authenticated) {
            runtime:Principal principal = runtime:getInvocationContext().principal;
            principal.userId = username;
            principal.username = username;
            principal.scopes = getScopes(username);
        }
        return authenticated;
    }
};

# Reads the scope(s) for the user with the given username.
#
# + username - Username
# + return - Array of groups for the user denoted by the username
function getScopes(string username) returns string[] {
    // first read the user id from user->id mapping
    // reads the groups for the user-id
    return getArray(getConfigAuthValue(CONFIG_USER_SECTION + "." + username, "scopes"));
}

# Extract password hash from the configuration file.
#
# + configValue - Config value to extract the password from
# + return - Password hash extracted from the configuration field
function extractHash(string configValue) returns string {
    return configValue.substring(configValue.indexOf("{") + 1, configValue.lastIndexOf("}"));
}

# Reads the password hash for a user.
#
# + username - Username
# + return - Password hash read from userstore, or nil if not found
function readPassword(string username) returns string {
    // first read the user id from user->id mapping
    // read the hashed password from the userstore file, using the user id
    return getConfigAuthValue(CONFIG_USER_SECTION + "." + username, "password");
}

function getConfigAuthValue(string instanceId, string property) returns string {
    return config:getAsString(instanceId + "." + property, defaultValue = "");
}

# Construct an array of groups from the comma separed group string passed.
#
# + groupString - Comma separated string of groups
# + return - Array of groups, nil if the groups string is empty/nil
function getArray(string groupString) returns string[] {
    string[] groupsArr = [];
    if (groupString.length() == 0) {
        return groupsArr;
    }
    return groupString.split(",");
}
