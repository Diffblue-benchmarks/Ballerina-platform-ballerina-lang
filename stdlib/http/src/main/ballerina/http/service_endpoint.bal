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

import ballerina/crypto;
import ballerina/log;
import ballerina/system;

/////////////////////////////
/// HTTP Listener Endpoint ///
/////////////////////////////
# This is used for creating HTTP server endpoints. An HTTP server endpoint is capable of responding to
# remote callers. The `Listener` is responsible for initializing the endpoint using the provided configurations.
public type Listener object {

    *AbstractListener;

    private int port = 0;
    private ServiceEndpointConfiguration config = {};
    private string instanceId;

    public function __start() returns error? {
        return self.start();
    }

    public function __stop() returns error? {
        return self.stop();
    }

    public function __attach(service s, string? name = ()) returns error? {
        return self.register(s, name);
    }

    public function __init(int port, ServiceEndpointConfiguration? config = ()) {
        self.instanceId = system:uuid();
        self.config = config ?: {};
        self.port = port;
        self.init(self.config);
    }

    # Gets invoked during module initialization to initialize the endpoint.
    #
    # + c - Configurations for HTTP service endpoints
    public function init(ServiceEndpointConfiguration c);

    public function initEndpoint() returns error? = external;

    # Gets invoked when attaching a service to the endpoint.
    #
    # + s - The service that needs to be attached
    # + name - Name of the service
    # + return - An `error` if there is any error occurred during the service attachment process or else nil
    function register(service s, string? name) returns error? = external;

    # Starts the registered service.
    function start() = external;

    # Stops the registered service.
    function stop() = external;
};

public function Listener.init(ServiceEndpointConfiguration c) {
    self.config = c;
    var auth = self.config["auth"];
    if (auth is ListenerAuth) {
        var authnHandlers = auth.authnHandlers;
        if (authnHandlers is AuthnHandler?[]) {
            if (authnHandlers.length() > 0) {
                initListener(self.config);
            }
        } else {
            if (authnHandlers[0].length() > 0) {
                initListener(self.config);
            }
        }
    }
    var err = self.initEndpoint();
    if (err is error) {
        panic err;
    }
}

function initListener(ServiceEndpointConfiguration config) {
    var secureSocket = config.secureSocket;
    if (secureSocket is ServiceSecureSocket) {
        addAuthFiltersForSecureListener(config);
    } else {
        error err = error("Secure sockets have not been cofigured in order to enable auth providers.");
        panic err;
    }
}

# Presents a read-only view of the remote address.
#
# + host - The remote host name/IP
# + port - The remote port
public type Remote record {|
    string host = "";
    int port = 0;
|};

# Presents a read-only view of the local address.
#
# + host - The local host name/IP
# + port - The local port
public type Local record {|
    string host = "";
    int port = 0;
|};

# Configures limits for requests. If these limits are violated, the request is rejected.
#
# + maxUriLength - Maximum allowed length for a URI. Exceeding this limit will result in a
#                  `414 - URI Too Long` response.
# + maxHeaderSize - Maximum allowed size for headers. Exceeding this limit will result in a
#                   `413 - Payload Too Large` response.
# + maxEntityBodySize - Maximum allowed size for the entity body. Exceeding this limit will result in a
#                       `413 - Payload Too Large` response.
public type RequestLimits record {|
    int maxUriLength = -1;
    int maxHeaderSize = -1;
    int maxEntityBodySize = -1;
|};

# Provides a set of configurations for HTTP service endpoints.
#
# + host - The host name/IP of the endpoint
# + keepAlive - Can be set to either `KEEPALIVE_AUTO`, which respects the `connection` header, or `KEEPALIVE_ALWAYS`,
#               which always keeps the connection alive, or `KEEPALIVE_NEVER`, which always closes the connection
# + secureSocket - The SSL configurations for the service endpoint. This needs to be configured in order to
#                  communicate through HTTPS.
# + httpVersion - Highest HTTP version supported by the endpoint
# + requestLimits - Configures the parameters for request validation
# + filters - If any pre-processing needs to be done to the request before dispatching the request to the
#             resource, filters can applied
# + timeoutMillis - Period of time in milliseconds that a connection waits for a read/write operation. Use value 0 to
#                   disable timeout
# + maxPipelinedRequests - Defines the maximum number of requests that can be processed at a given time on a single
#                          connection. By default 10 requests can be pipelined on a single cinnection and user can
#                          change this limit appropriately. This will be applicable only for HTTP 1.1
# + auth - Listener authenticaton configurations
public type ServiceEndpointConfiguration record {|
    string host = "0.0.0.0";
    KeepAlive keepAlive = KEEPALIVE_AUTO;
    ServiceSecureSocket? secureSocket = ();
    string httpVersion = "1.1";
    RequestLimits? requestLimits = ();
    //TODO: update as a optional field
    Filter[] filters = [];
    int timeoutMillis = DEFAULT_LISTENER_TIMEOUT;
    int maxPipelinedRequests = MAX_PIPELINED_REQUESTS;
    ListenerAuth auth?;
|};

# Authentication configurations for the listener.
#
# + authnHandlers - Array of authentication handlers or Array of arrays of authentication handlers. Array is used to
# say at least one of the authenticaion handlers should successfully authenticated. Array of arrays is used to say
# at least one authentication handler from the sub arrays should successfully authenticated.
# + scopes - Array of scopes or Array of arrays of scopes. Array is used to say at least one of the scopes should
# successfully authorized. Array of arrays is used to say at least one scope from the sub arrays should successfully
# authorized.
# + positiveAuthzCache - Caching configurations for positive authorizations
# + negativeAuthzCache - Caching configurations for negative authorizations
public type ListenerAuth record {|
    (AuthnHandler?)[]|(AuthnHandler?)[][] authnHandlers;
    string[]|string[][] scopes?;
    AuthCacheConfig positiveAuthzCache = {};
    AuthCacheConfig negativeAuthzCache = {};
|};

# Configures the SSL/TLS options to be used for HTTP service.
#
# + trustStore - Configures the trust store to be used
# + keyStore - Configures the key store to be used
# + certFile - A file containing the certificate of the server
# + keyFile - A file containing the private key of the server
# + keyPassword - Password of the private key if it is encrypted
# + trustedCertFile - A file containing a list of certificates or a single certificate that the server trusts
# + protocol - SSL/TLS protocol related options
# + certValidation - Certificate validation against CRL or OCSP related options
# + ciphers - List of ciphers to be used (e.g.: TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
#             TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA)
# + sslVerifyClient - The type of client certificate verification
# + shareSession - Enable/disable new SSL session creation
# + handshakeTimeout - SSL handshake time out
# + sessionTimeout - SSL session time out
# + ocspStapling - Enable/disable OCSP stapling
public type ServiceSecureSocket record {|
    crypto:TrustStore? trustStore = ();
    crypto:KeyStore? keyStore = ();
    string certFile = "";
    string keyFile = "";
    string keyPassword = "";
    string trustedCertFile = "";
    Protocols? protocol = ();
    ValidateCert? certValidation = ();
    string[] ciphers = ["TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"];
    string sslVerifyClient = "";
    boolean shareSession = true;
    int? handshakeTimeout = ();
    int? sessionTimeout = ();
    ServiceOcspStapling? ocspStapling = ();
|};

# Provides a set of configurations for controlling the authorization caching behaviour of the endpoint.
#
# + enabled - Specifies whether authorization caching is enabled. Caching is enabled by default.
# + capacity - The capacity of the cache
# + expiryTimeMillis - The number of milliseconds to keep an entry in the cache
# + evictionFactor - The fraction of entries to be removed when the cache is full. The value should be
#                    between 0 (exclusive) and 1 (inclusive).
public type AuthCacheConfig record {|
    boolean enabled = true;
    int capacity = 100;
    int expiryTimeMillis = 5 * 1000; // 5 seconds;
    float evictionFactor = 1;
|};

# Defines the possible values for the keep-alive configuration in service and client endpoints.
public type KeepAlive KEEPALIVE_AUTO|KEEPALIVE_ALWAYS|KEEPALIVE_NEVER;

# Decides to keep the connection alive or not based on the `connection` header of the client request }
public const KEEPALIVE_AUTO = "AUTO";
# Keeps the connection alive irrespective of the `connection` header value }
public const KEEPALIVE_ALWAYS = "ALWAYS";
# Closes the connection irrespective of the `connection` header value }
public const KEEPALIVE_NEVER = "NEVER";

# Add authn and authz filters
#
# + config - `ServiceEndpointConfiguration` instance
function addAuthFiltersForSecureListener(ServiceEndpointConfiguration config) {
    // add authentication and authorization filters as the first two filters.
    // if there are any other filters specified, those should be added after the authn and authz filters.
    Filter[] authFilters = [];

    var auth = config["auth"];
    if (auth is ListenerAuth) {
        AuthnHandler?[]|AuthnHandler?[][] authnHandlers = auth.authnHandlers;
        AuthnFilter authnFilter = new(authnHandlers);
        authFilters[0] = authnFilter;

        var scopes = auth["scopes"];
        cache:Cache positiveAuthzCache = new(expiryTimeMillis = auth.positiveAuthzCache.expiryTimeMillis,
                                            capacity = auth.positiveAuthzCache.capacity,
                                            evictionFactor = auth.positiveAuthzCache.evictionFactor);
        cache:Cache negativeAuthzCache = new(expiryTimeMillis = auth.negativeAuthzCache.expiryTimeMillis,
                                            capacity = auth.negativeAuthzCache.capacity,
                                            evictionFactor = auth.negativeAuthzCache.evictionFactor);
        AuthzHandler authzHandler = new(positiveAuthzCache, negativeAuthzCache);
        AuthzFilter authzFilter = new(authzHandler, scopes);
        authFilters[1] = authzFilter;

        if (config.filters.length() == 0) {
            // can add authn and authz filters directly
            config.filters = authFilters;
        } else {
            Filter[] newFilters = authFilters;
            // add existing filters next
            int i = 0;
            while (i < config.filters.length()) {
                newFilters[i + (newFilters.length())] = config.filters[i];
                i = i + 1;
            }
            config.filters = newFilters;
        }
    }
    // No need to validate else part since the function is called if and only if the `auth is ListenerAuth`
}

//////////////////////////////////
/// WebSocket Service Endpoint ///
//////////////////////////////////
# Represents a WebSocket service endpoint.
// public type WebSocketListener Listener;
public type WebSocketListener object {

    *AbstractListener;

    private Listener httpEndpoint;

    public function __start() returns error? {
        return self.httpEndpoint.start();
    }

    public function __stop() returns error? {
        return self.httpEndpoint.stop();
    }

    public function __attach(service s, string? name = ()) returns error? {
        return self.httpEndpoint.register(s, name);
    }


    # Gets invoked during module initialization to initialize the endpoint.
    #
    # + port - The port of the endpoint
    # + config - The `ServiceEndpointConfiguration` of the endpoint
    public function __init(int port, ServiceEndpointConfiguration? config = ()) {
        self.httpEndpoint = new(port, config = config);
    }

};
