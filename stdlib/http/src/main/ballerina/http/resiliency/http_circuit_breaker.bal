// Copyright (c) 2018 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// //www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/log;
import ballerina/time;
import ballerina/io;

# A finite type for modeling the states of the Circuit Breaker. The Circuit Breaker starts in the `CLOSED` state.
# If any failure thresholds are exceeded during execution, the circuit trips and goes to the `OPEN` state. After
# the specified timeout period expires, the circuit goes to the `HALF_OPEN` state. If the trial request sent while
# in the `HALF_OPEN` state succeeds, the circuit goes back to the `CLOSED` state.
public type CircuitState CB_OPEN_STATE|CB_HALF_OPEN_STATE|CB_CLOSED_STATE;

# Represents the open state of the circuit. When the Circuit Breaker is in `OPEN` state, requests will fail
# immediately.
public const CB_OPEN_STATE = "OPEN";

# Represents the half-open state of the circuit. When the Circuit Breaker is in `HALF_OPEN` state, a trial request
# will be sent to the upstream service. If it fails, the circuit will trip again and move to the `OPEN` state. If not,
# it will move to the `CLOSED` state.
public const CB_HALF_OPEN_STATE = "HALF_OPEN";

# Represents the closed state of the circuit. When the Circuit Breaker is in `CLOSED` state, all requests will be
# allowed to go through to the upstream service. If the failures exceed the configured threhold values, the circuit
# will trip and move to the `OPEN` state.
public const CB_CLOSED_STATE = "CLOSED";

# Maintains the health of the Circuit Breaker.
#
# + lastRequestSuccess - Whether last request is success or not
# + totalRequestCount - Total request count received within the `RollingWindow`
# + lastUsedBucketId - ID of the last bucket used in Circuit Breaker calculations
# + startTime - Circuit Breaker start time
# + lastRequestTime - The time that the last request received
# + lastErrorTime - The time that the last error occurred
# + lastForcedOpenTime - The time that circuit forcefully opened at last
# + totalBuckets - The discrete time buckets into which the time window is divided
public type CircuitHealth record {|
    boolean lastRequestSuccess = false;
    int totalRequestCount = 0;
    int lastUsedBucketId = 0;
    time:Time startTime = time:currentTime();
    time:Time lastRequestTime?;
    time:Time lastErrorTime?;
    time:Time lastForcedOpenTime?;
    Bucket?[] totalBuckets = [];
|};

# Provides a set of configurations for controlling the behaviour of the Circuit Breaker.
#
# + rollingWindow - `RollingWindow` options of the `CircuitBreaker`
# + failureThreshold - The threshold for request failures. When this threshold exceeds, the circuit trips.
#                      The threshold should be a value between 0 and 1.
# + resetTimeMillis - The time period(in milliseconds) to wait before attempting to make another request to
#                     the upstream service
# + statusCodes - Array of HTTP response status codes which are considered as failures
public type CircuitBreakerConfig record {|
    RollingWindow rollingWindow = {};
    float failureThreshold = 0.0;
    int resetTimeMillis = 0;
    int[] statusCodes = [];
|};

# Represents a rolling window in the Circuit Breaker.
#
# + requestVolumeThreshold - Minimum number of requests in a `RollingWindow` that will trip the circuit.
# + timeWindowMillis - Time period in milliseconds for which the failure threshold is calculated
# + bucketSizeMillis - The granularity at which the time window slides. This is measured in milliseconds.
public type RollingWindow record {|
    int requestVolumeThreshold = 10;
    int timeWindowMillis = 60000;
    int bucketSizeMillis = 10000;
|};

# Represents a discrete sub-part of the time window (Bucket).
#
# + totalCount - Total number of requests received during the sub-window time frame
# + failureCount - Number of failed requests during the sub-window time frame
# + rejectedCount - Number of rejected requests during the sub-window time frame
# + lastUpdatedTime - The time that the `Bucket` is last updated.
public type Bucket record {|
    int totalCount = 0;
    int failureCount = 0;
    int rejectedCount = 0;
    time:Time lastUpdatedTime?;
|};

# Derived set of configurations from the `CircuitBreakerConfig`.
#
# + failureThreshold - The threshold for request failures. When this threshold exceeds, the circuit trips.
#                      The threshold should be a value between 0 and 1
# + resetTimeMillis - The time period(in milliseconds) to wait before attempting to make another request to
#                     the upstream service
# + statusCodes - Array of HTTP response status codes which are considered as failures
# + noOfBuckets - Number of buckets derived from the `RollingWindow`
# + rollingWindow - `RollingWindow` options provided in the `CircuitBreakerConfig`
public type CircuitBreakerInferredConfig record {|
    float failureThreshold = 0.0;
    int resetTimeMillis = 0;
    boolean[] statusCodes = [];
    int noOfBuckets = 0;
    RollingWindow rollingWindow = {};
|};

# A Circuit Breaker implementation which can be used to gracefully handle network failures.
#
# + url - The URL of the target service
# + config - The configurations of the client endpoint associated with this `CircuitBreaker` instance
# + circuitBreakerInferredConfig - Configurations derived from `CircuitBreakerConfig`
# + httpClient - The underlying `HttpActions` instance which will be making the actual network calls
# + circuitHealth - The circuit health monitor
# + currentCircuitState - The current state the cicuit is in
public type CircuitBreakerClient object {

    public string url;
    public ClientEndpointConfig config;
    public CircuitBreakerInferredConfig circuitBreakerInferredConfig;
    public Client httpClient;
    public CircuitHealth circuitHealth;
    public CircuitState currentCircuitState = CB_CLOSED_STATE;

    # A Circuit Breaker implementation which can be used to gracefully handle network failures.
    #
    # + url - The URL of the target service
    # + config - The configurations of the client endpoint associated with this `CircuitBreaker` instance
    # + circuitBreakerInferredConfig - Configurations derived from `CircuitBreakerConfig`
    # + httpClient - The underlying `HttpActions` instance which will be making the actual network calls
    # + circuitHealth - The circuit health monitor
    public function __init(string url, ClientEndpointConfig config, CircuitBreakerInferredConfig
                                        circuitBreakerInferredConfig, Client httpClient, CircuitHealth circuitHealth) {
        self.url = url;
        self.config = config;
        self.circuitBreakerInferredConfig = circuitBreakerInferredConfig;
        self.httpClient = httpClient;
        self.circuitHealth = circuitHealth;
    }

    # The POST remote function implementation of the Circuit Breaker. This wraps the `post()` function of the underlying
    # HTTP remote functions provider.
    #
    # + path - Resource path
    # + message - A Request or any payload of type `string`, `xml`, `json`, `byte[]`, `io:ReadableByteChannel`
    #             or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream
    #            server
    public function post(string path, RequestMessage message) returns Response|error;

    # The HEAD remote function implementation of the Circuit Breaker. This wraps the `head()` function of the underlying
    # HTTP remote functions provider.
    #
    # + path - Resource path
    # + message - A Request or any payload of type `string`, `xml`, `json`, `byte[]`, `io:ReadableByteChannel`
    #             or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream
    #            server
    public function head(string path, RequestMessage message = ()) returns Response|error;

    # The PUT remote function implementation of the Circuit Breaker. This wraps the `put()` function of the underlying
    # HTTP remote functions provider.
    #
    # + path - Resource path
    # + message - A Request or any payload of type `string`, `xml`, `json`, `byte[]`, `io:ReadableByteChannel`
    #             or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream
    #            server
    public function put(string path, RequestMessage message) returns Response|error;

    # This wraps the `post()` function of the underlying HTTP remote functions provider. The `execute()` function can
    # be used to invoke an HTTP call with the given HTTP verb.
    #
    # + httpVerb - HTTP verb to be used for the request
    # + path - Resource path
    # + message - A Request or any payload of type `string`, `xml`, `json`, `byte[]`, `io:ReadableByteChannel` or
    #             `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream
    #            server
    public function execute(string httpVerb, string path, RequestMessage message) returns Response|error;

    # The PATCH remote function implementation of the Circuit Breaker. This wraps the `patch()` function of the
    # underlying HTTP remote functions provider.
    #
    # + path - Resource path
    # + message - A Request or any payload of type `string`, `xml`, `json`, `byte[]`, `io:ReadableByteChannel` or
    #             `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream
    #            server
    public function patch(string path, RequestMessage message) returns Response|error;

    # The DELETE remote function implementation of the Circuit Breaker. This wraps the `delete()` function of the
    # underlying HTTP remote functions provider.
    #
    # + path - Resource path
    # + message - A Request or any payload of type `string`, `xml`, `json`, `byte[]`, `io:ReadableByteChannel` or
    #             `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream
    #            server
    public function delete(string path, RequestMessage message) returns Response|error;

    # The GET remote function implementation of the Circuit Breaker. This wraps the `get()` function of the underlying
    # HTTP remote functions provider.
    #
    # + path - Resource path
    # + message - An optional HTTP request or any payload of type `string`, `xml`, `json`, `byte[]`,
    #            `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream
    #            server
    public function get(string path, RequestMessage message = ()) returns Response|error;

    # The OPTIONS remote function implementation of the Circuit Breaker. This wraps the `options()` function of the
    # underlying HTTP remote functions provider.
    #
    # + path - Resource path
    # + message - An optional HTTP Request or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The response for the request or an `error` if failed to establish communication with the upstream
    #            server
    public function options(string path, RequestMessage message = ()) returns Response|error;

    # This wraps the `forward()` function of the underlying HTTP remote functions provider. The Forward remote function
    # can be used to forward an incoming request to an upstream service as it is.
    #
    # + path - Resource path
    # + request - A Request struct
    # + return - The response for the request or an `error` if failed to establish communication with the upstream
    #            server
    public function forward(string path, Request request) returns Response|error;

    # Submits an HTTP request to a service with the specified HTTP verb.
    # The `submit()` function does not give out a `Response` as the result,
    # rather it returns an `HttpFuture` which can be used to do further interactions with the endpoint.
    #
    # + httpVerb - The HTTP verb value
    # + path - The resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - An `HttpFuture` that represents an asynchronous service invocation, or an `error` if the submission
    #            fails
    public function submit(string httpVerb, string path, RequestMessage message) returns HttpFuture|error;

    # Retrieves the `Response` for a previously submitted request.
    #
    # + httpFuture - The `HttpFuture` related to a previous asynchronous invocation
    # + return - An HTTP response message, or an `error` if the invocation fails
    public function getResponse(HttpFuture httpFuture) returns Response|error;

    # Circuit breaking not supported. Defaults to the `hasPromise()` function of the underlying HTTP remote functions
    # provider.
    #
    # + httpFuture - The `HttpFuture` relates to a previous asynchronous invocation
    # + return - A `boolean` that represents whether a `PushPromise` exists
    public function hasPromise(HttpFuture httpFuture) returns (boolean);

    # Retrieves the next available `PushPromise` for a previously submitted request.
    #
    # + httpFuture - The `HttpFuture` relates to a previous asynchronous invocation
    # + return - An HTTP `PushPromise` message, or an `error` if the invocation fails
    public function getNextPromise(HttpFuture httpFuture) returns PushPromise|error;

    # Retrieves the promised server push `Response` message.
    #
    # + promise - The related `PushPromise`
    # + return - A promised HTTP `Response` message, or an `error` if the invocation fails
    public function getPromisedResponse(PushPromise promise) returns Response|error;

    # Circuit breaking not supported. Defaults to the `rejectPromise()` function of the underlying HTTP
    # remote functions provider.
    #
    # + promise - The `PushPromise` to be rejected
    public function rejectPromise(PushPromise promise);

    # Force the circuit into a closed state in which it will allow requests regardless of the error percentage
    # until the failure threshold exceeds.
    public function forceClose();

    # Force the circuit into a open state in which it will suspend all requests
    # until `resetTimeMillis` interval exceeds.
    public function forceOpen();

    # Provides `CircuitState` of the circuit breaker.
    #
    # + return - The current `CircuitState` of circuit breaker
    public function getCurrentState() returns CircuitState;
};

public function CircuitBreakerClient.post(string path, RequestMessage message) returns Response|error {
    CircuitBreakerInferredConfig cbic = self.circuitBreakerInferredConfig;
    self.currentCircuitState = updateCircuitState(self.circuitHealth, self.currentCircuitState, cbic);

    if (self.currentCircuitState == CB_OPEN_STATE) {
        // TODO: Allow the user to handle this scenario. Maybe through a user provided function
        return handleOpenCircuit(self.circuitHealth, cbic);
    } else {
        var serviceResponse = self.httpClient->post(path, <Request>message);
        return updateCircuitHealthAndRespond(serviceResponse, self.circuitHealth, cbic);
    }
}

public function CircuitBreakerClient.head(string path, RequestMessage message = ()) returns Response|error {
    CircuitBreakerInferredConfig cbic = self.circuitBreakerInferredConfig;
    self.currentCircuitState = updateCircuitState(self.circuitHealth, self.currentCircuitState, cbic);

    if (self.currentCircuitState == CB_OPEN_STATE) {
        // TODO: Allow the user to handle this scenario. Maybe through a user provided function
        return handleOpenCircuit(self.circuitHealth, cbic);
    } else {
        var serviceResponse = self.httpClient->head(path, message = <Request>message);
        return updateCircuitHealthAndRespond(serviceResponse, self.circuitHealth, cbic);
    }
}

public function CircuitBreakerClient.put(string path, RequestMessage message) returns Response|error {
    CircuitBreakerInferredConfig cbic = self.circuitBreakerInferredConfig;
    self.currentCircuitState = updateCircuitState(self.circuitHealth, self.currentCircuitState, cbic);

    if (self.currentCircuitState == CB_OPEN_STATE) {
        // TODO: Allow the user to handle this scenario. Maybe through a user provided function
        return handleOpenCircuit(self.circuitHealth, cbic);
    } else {
        var serviceResponse = self.httpClient->put(path, <Request>message);
        return updateCircuitHealthAndRespond(serviceResponse, self.circuitHealth, cbic);
    }
}

public function CircuitBreakerClient.execute(string httpVerb, string path, RequestMessage message)
                                                                                    returns Response|error {

    CircuitBreakerInferredConfig cbic = self.circuitBreakerInferredConfig;
    self.currentCircuitState = updateCircuitState(self.circuitHealth, self.currentCircuitState, cbic);

    if (self.currentCircuitState == CB_OPEN_STATE) {
        // TODO: Allow the user to handle this scenario. Maybe through a user provided function
        return handleOpenCircuit(self.circuitHealth, cbic);
    } else {
        var serviceResponse = self.httpClient->execute(httpVerb, path, <Request>message);
        return updateCircuitHealthAndRespond(serviceResponse, self.circuitHealth, cbic);
    }
}

public function CircuitBreakerClient.patch(string path, RequestMessage message) returns Response|error {
    CircuitBreakerInferredConfig cbic = self.circuitBreakerInferredConfig;
    self.currentCircuitState = updateCircuitState(self.circuitHealth, self.currentCircuitState, cbic);

    if (self.currentCircuitState == CB_OPEN_STATE) {
        // TODO: Allow the user to handle this scenario. Maybe through a user provided function
        return handleOpenCircuit(self.circuitHealth, cbic);
    } else {
        var serviceResponse = self.httpClient->patch(path, <Request>message);
        return updateCircuitHealthAndRespond(serviceResponse, self.circuitHealth, cbic);
    }
}

public function CircuitBreakerClient.delete(string path, RequestMessage message) returns Response|error {
    CircuitBreakerInferredConfig cbic = self.circuitBreakerInferredConfig;
    self.currentCircuitState = updateCircuitState(self.circuitHealth, self.currentCircuitState, cbic);

    if (self.currentCircuitState == CB_OPEN_STATE) {
        // TODO: Allow the user to handle this scenario. Maybe through a user provided function
        return handleOpenCircuit(self.circuitHealth, cbic);
    } else {
        var serviceResponse = self.httpClient->delete(path, <Request>message);
        return updateCircuitHealthAndRespond(serviceResponse, self.circuitHealth, cbic);
    }
}

public function CircuitBreakerClient.get(string path, RequestMessage message = ()) returns Response|error {
    CircuitBreakerInferredConfig cbic = self.circuitBreakerInferredConfig;
    self.currentCircuitState = updateCircuitState(self.circuitHealth, self.currentCircuitState, cbic);

    if (self.currentCircuitState == CB_OPEN_STATE) {
        // TODO: Allow the user to handle this scenario. Maybe through a user provided function
        return handleOpenCircuit(self.circuitHealth, cbic);
    } else {
        var serviceResponse = self.httpClient->get(path, message = <Request>message);
        return updateCircuitHealthAndRespond(serviceResponse, self.circuitHealth, cbic);
    }
}

public function CircuitBreakerClient.options(string path, RequestMessage message = ()) returns Response|error {
    CircuitBreakerInferredConfig cbic = self.circuitBreakerInferredConfig;
    self.currentCircuitState = updateCircuitState(self.circuitHealth, self.currentCircuitState, cbic);

    if (self.currentCircuitState == CB_OPEN_STATE) {
        // TODO: Allow the user to handle this scenario. Maybe through a user provided function
        return handleOpenCircuit(self.circuitHealth, cbic);
    } else {
        var serviceResponse = self.httpClient->options(path, message = <Request>message);
        return updateCircuitHealthAndRespond(serviceResponse, self.circuitHealth, cbic);
    }
}

public function CircuitBreakerClient.forward(string path, Request request) returns Response|error {
    CircuitBreakerInferredConfig cbic = self.circuitBreakerInferredConfig;
    self.currentCircuitState = updateCircuitState(self.circuitHealth, self.currentCircuitState, cbic);

    if (self.currentCircuitState == CB_OPEN_STATE) {
        // TODO: Allow the user to handle this scenario. Maybe through a user provided function
        return handleOpenCircuit(self.circuitHealth, cbic);
    } else {
        var serviceResponse = self.httpClient->forward(path, request);
        return updateCircuitHealthAndRespond(serviceResponse, self.circuitHealth, cbic);
    }
}

public function CircuitBreakerClient.submit(string httpVerb, string path, RequestMessage message)
                                                                            returns HttpFuture|error {

    CircuitBreakerInferredConfig cbic = self.circuitBreakerInferredConfig;
    self.currentCircuitState = updateCircuitState(self.circuitHealth, self.currentCircuitState, cbic);

    if (self.currentCircuitState == CB_OPEN_STATE) {
        // TODO: Allow the user to handle this scenario. Maybe through a user provided function
        return handleOpenCircuit(self.circuitHealth, cbic);
    } else {
        var serviceFuture = self.httpClient->submit(httpVerb, path, <Request>message);
        if (serviceFuture is HttpFuture) {
            var serviceResponse = self.httpClient->getResponse(serviceFuture);
            var result = updateCircuitHealthAndRespond(serviceResponse, self.circuitHealth, cbic);
        } else {
            updateCircuitHealthFailure(self.circuitHealth, cbic);
        }
        return serviceFuture;
    }
}

public function CircuitBreakerClient.getResponse(HttpFuture httpFuture) returns Response|error {
    // No need to check for response as we already check for the response in submit method
    return self.httpClient->getResponse(httpFuture);
}

public function CircuitBreakerClient.hasPromise(HttpFuture httpFuture) returns boolean {
    return self.httpClient->hasPromise(httpFuture);
}

public function CircuitBreakerClient.getNextPromise(HttpFuture httpFuture) returns PushPromise|error {
    return self.httpClient->getNextPromise(httpFuture);
}

public function CircuitBreakerClient.getPromisedResponse(PushPromise promise) returns Response|error {
    return self.httpClient->getPromisedResponse(promise);
}

public function CircuitBreakerClient.rejectPromise(PushPromise promise) {
    return self.httpClient->rejectPromise(promise);
}

public function CircuitBreakerClient.forceClose() {
    self.currentCircuitState = CB_CLOSED_STATE;
}

public function CircuitBreakerClient.forceOpen() {
    self.currentCircuitState = CB_OPEN_STATE;
    self.circuitHealth.lastForcedOpenTime = time:currentTime();
}

public function CircuitBreakerClient.getCurrentState() returns CircuitState {
    return self.currentCircuitState;
}

# Update circuit state.
#
# + circuitHealth - Circuit Breaker health status
# + currentStateValue - Circuit Breaker current state value
# + circuitBreakerInferredConfig - Configurations derived from `CircuitBreakerConfig`
# + return - State of the circuit
function updateCircuitState(CircuitHealth circuitHealth, CircuitState currentStateValue,
                            CircuitBreakerInferredConfig circuitBreakerInferredConfig) returns (CircuitState) {
    lock {
        CircuitState currentState = currentStateValue;
        prepareRollingWindow(circuitHealth, circuitBreakerInferredConfig);
        int currentBucketId = getCurrentBucketId(circuitHealth, circuitBreakerInferredConfig);
        updateLastUsedBucketId(currentBucketId, circuitHealth);
        circuitHealth.lastRequestTime = time:currentTime();
        int totalRequestsCount = getTotalRequestsCount(circuitHealth);
        circuitHealth.totalRequestCount = totalRequestsCount;
        if (totalRequestsCount >= circuitBreakerInferredConfig.rollingWindow.requestVolumeThreshold) {
            if (currentState == CB_OPEN_STATE) {
                currentState = switchCircuitStateOpenToHalfOpenOnResetTime(circuitBreakerInferredConfig,
                                                                                    circuitHealth, currentState);
            } else if (currentState == CB_HALF_OPEN_STATE) {
                if (!circuitHealth.lastRequestSuccess) {
                    // If the trial run has failed, trip the circuit again
                    currentState = CB_OPEN_STATE;
                    log:printInfo("CircuitBreaker trial run has failed. Circuit switched from HALF_OPEN to OPEN state.")
                    ;
                } else {
                    // If the trial run was successful reset the circuit
                    currentState = CB_CLOSED_STATE;
                    log:printInfo(
                        "CircuitBreaker trial run  was successful. Circuit switched from HALF_OPEN to CLOSE state.");
                }
            } else {
                float currentFailureRate = getCurrentFailureRatio(circuitHealth);

                if (currentFailureRate > circuitBreakerInferredConfig.failureThreshold) {
                    currentState = CB_OPEN_STATE;
                    log:printInfo("CircuitBreaker failure threshold exceeded. Circuit tripped from CLOSE to OPEN state."
                    );
                }
            }
        } else {
            currentState = switchCircuitStateOpenToHalfOpenOnResetTime(circuitBreakerInferredConfig,
                                                                                    circuitHealth, currentState);
        }
        Bucket bucket = <Bucket> circuitHealth.totalBuckets[currentBucketId];
        bucket.totalCount += 1;
        return currentState;
    }
}

function updateCircuitHealthAndRespond(Response|error serviceResponse, CircuitHealth circuitHealth,
                                   CircuitBreakerInferredConfig circuitBreakerInferredConfig) returns Response|error {
    if (serviceResponse is Response) {
        if (circuitBreakerInferredConfig.statusCodes[serviceResponse.statusCode]) {
            updateCircuitHealthFailure(circuitHealth, circuitBreakerInferredConfig);
        } else {
            updateCircuitHealthSuccess(circuitHealth, circuitBreakerInferredConfig);
        }
    } else {
        updateCircuitHealthFailure(circuitHealth, circuitBreakerInferredConfig);
    }
    return serviceResponse;
}

function updateCircuitHealthFailure(CircuitHealth circuitHealth,
                                    CircuitBreakerInferredConfig circuitBreakerInferredConfig) {
    lock {
        int currentBucketId = getCurrentBucketId(circuitHealth, circuitBreakerInferredConfig);
        circuitHealth.lastRequestSuccess = false;
        updateLastUsedBucketId(currentBucketId, circuitHealth);
        Bucket bucket = <Bucket> circuitHealth.totalBuckets[currentBucketId];
        bucket.failureCount += 1;
        time:Time lastUpdated = time:currentTime();
        circuitHealth.lastErrorTime = lastUpdated;
        circuitHealth.totalBuckets[currentBucketId].lastUpdatedTime = lastUpdated;
    }
}

function updateCircuitHealthSuccess(CircuitHealth circuitHealth,
                                    CircuitBreakerInferredConfig circuitBreakerInferredConfig) {
    lock {
        int currentBucketId = getCurrentBucketId(circuitHealth, circuitBreakerInferredConfig);
        time:Time lastUpdated = time:currentTime();
        updateLastUsedBucketId(currentBucketId, circuitHealth);
        circuitHealth.lastRequestSuccess = true;
        circuitHealth.totalBuckets[currentBucketId].lastUpdatedTime = lastUpdated;
    }
}

// Handles open circuit state.
function handleOpenCircuit(CircuitHealth circuitHealth, CircuitBreakerInferredConfig circuitBreakerInferredConfig)
             returns (error) {
    updateRejectedRequestCount(circuitHealth, circuitBreakerInferredConfig);
    time:Time effectiveErrorTime = getEffectiveErrorTime(circuitHealth);
    int timeDif = time:currentTime().time - effectiveErrorTime.time;
    int timeRemaining = circuitBreakerInferredConfig.resetTimeMillis - timeDif;
    string errorMessage = "Upstream service unavailable. Requests to upstream service will be suspended for "
        + timeRemaining + " milliseconds.";
    map<anydata|error> errorDetail = { message : errorMessage };
    error httpConnectorErr = error(HTTP_ERROR_CODE, errorDetail);
    return httpConnectorErr;
}

// Validates the struct configurations passed to create circuit breaker.
function validateCircuitBreakerConfiguration(CircuitBreakerConfig circuitBreakerConfig) {
    float failureThreshold = circuitBreakerConfig.failureThreshold;
    if (failureThreshold < 0 || failureThreshold > 1) {
        string errorMessage = "Invalid failure threshold. Failure threshold value"
            + " should between 0 to 1, found " + failureThreshold;
        map<anydata|error> errorDetail = { message : errorMessage };
        error circuitBreakerConfigError = error(HTTP_ERROR_CODE, errorDetail);
        panic circuitBreakerConfigError;
    }
}

# Calculate Failure at a given point.
#
# + circuitHealth - Circuit Breaker health status
# + return - Current failure ratio
function getCurrentFailureRatio(CircuitHealth circuitHealth) returns float {
    int totalCount = 0;
    int totalFailures = 0;

    foreach var optBucket in circuitHealth.totalBuckets {
        var bucket = <Bucket>optBucket;
        totalCount =  totalCount + bucket.failureCount +
                                        (bucket.totalCount - (bucket.failureCount + bucket.rejectedCount));
        totalFailures = totalFailures + bucket.failureCount;
    }
    float ratio = 0.0;
    if (totalCount > 0) {
        ratio = <float> totalFailures / totalCount;
    }
    return ratio;
}

# Calculate total requests count within `RollingWindow`.
#
# + circuitHealth - Circuit Breaker health status
# + return - Total requests count
function getTotalRequestsCount(CircuitHealth circuitHealth) returns int {
    int totalCount = 0;

    foreach var bucket in circuitHealth.totalBuckets {
        Bucket temp = <Bucket>bucket;
        totalCount  =  totalCount + temp.totalCount;
    }
    return totalCount;
}

# Calculate the current bucket Id.
#
# + circuitHealth - Circuit Breaker health status
# + circuitBreakerInferredConfig - Configurations derived from `CircuitBreakerConfig`
# + return - Current bucket id
function getCurrentBucketId(CircuitHealth circuitHealth, CircuitBreakerInferredConfig circuitBreakerInferredConfig)
             returns int {
    int elapsedTime = (time:currentTime().time - circuitHealth.startTime.time) % circuitBreakerInferredConfig.
        rollingWindow.timeWindowMillis;
    int currentBucketId = ((elapsedTime / circuitBreakerInferredConfig.rollingWindow.bucketSizeMillis) + 1)
        % circuitBreakerInferredConfig.noOfBuckets;
    return currentBucketId;
}

# Update rejected requests count.
#
# + circuitHealth - Circuit Breaker health status
# + circuitBreakerInferredConfig - Configurations derived from `CircuitBreakerConfig`
function updateRejectedRequestCount(CircuitHealth circuitHealth,
                                                CircuitBreakerInferredConfig circuitBreakerInferredConfig) {

    int currentBucketId = getCurrentBucketId(circuitHealth, circuitBreakerInferredConfig);
    updateLastUsedBucketId(currentBucketId, circuitHealth);
    Bucket bucket = <Bucket>circuitHealth.totalBuckets[currentBucketId];
    bucket.rejectedCount += 1;
}

# Reset the bucket values to default ones.
#
# + circuitHealth - - Circuit Breaker health status.
# + bucketId - - Id of the bucket should reset.
function resetBucketStats(CircuitHealth circuitHealth, int bucketId) {
    circuitHealth.totalBuckets[bucketId] = {};
}

function getEffectiveErrorTime(CircuitHealth circuitHealth) returns time:Time {
    return (circuitHealth.lastErrorTime.time > circuitHealth.lastForcedOpenTime.time)
    ? circuitHealth.lastErrorTime : circuitHealth.lastForcedOpenTime;
}

# Populate the `RollingWindow` statistics to handle circuit breaking within the `RollingWindow` time frame.
#
# + circuitHealth - Circuit Breaker health status
# + circuitBreakerInferredConfig - Configurations derived from `CircuitBreakerConfig`
function prepareRollingWindow(CircuitHealth circuitHealth, CircuitBreakerInferredConfig circuitBreakerInferredConfig) {

    int currentTime = time:currentTime().time;
    int idleTime = currentTime - circuitHealth.lastRequestTime.time;
    RollingWindow rollingWindow = circuitBreakerInferredConfig.rollingWindow;
    // If the time duration between two requests greater than timeWindowMillis values, reset the buckets to default.
    if (idleTime > rollingWindow.timeWindowMillis) {
        reInitializeBuckets(circuitHealth);
    } else {
        int currentBucketId = getCurrentBucketId(circuitHealth, circuitBreakerInferredConfig);
        int lastUsedBucketId = circuitHealth.lastUsedBucketId;
        // Check whether subsequent requests received within same bucket(sub time window). If the idle time is greater
        // than bucketSizeMillis means subsequent calls are received time exceeding the rolling window. if we need to
        // reset the buckets to default.
        if (currentBucketId == circuitHealth.lastUsedBucketId && idleTime > rollingWindow.bucketSizeMillis) {
            reInitializeBuckets(circuitHealth);
        // If the current bucket (sub time window) is less than last updated bucket. Stats of the current bucket to
        // zeroth bucket and Last bucket to last used bucket needs to be reset to default.
        } else if (currentBucketId < lastUsedBucketId) {
            int index = currentBucketId;
            while (index >= 0) {
                resetBucketStats(circuitHealth, index);
                index -= 1;
            }
            int lastIndex = (circuitHealth.totalBuckets.length()) - 1;
            while (lastIndex > currentBucketId) {
                resetBucketStats(circuitHealth, lastIndex);
                lastIndex -= 1;
            }
        } else {
            // If the current bucket (sub time window) is greater than last updated bucket. Stats of current bucket to
            // last used bucket needs to be reset without resetting last used bucket stat.
            while (currentBucketId > lastUsedBucketId && idleTime > rollingWindow.bucketSizeMillis) {
                resetBucketStats(circuitHealth, currentBucketId);
                currentBucketId -= 1;
            }
        }
    }
}

# Reinitialize the Buckets to default state.
#
# + circuitHealth - Circuit Breaker health status
function reInitializeBuckets(CircuitHealth circuitHealth) {
    Bucket?[] bucketArray = [];
    int bucketIndex = 0;
    while (bucketIndex < circuitHealth.totalBuckets.length()) {
        bucketArray[bucketIndex] = {};
        bucketIndex += 1;
    }
    circuitHealth.totalBuckets = bucketArray;
}

# Updates the `lastUsedBucketId` in `CircuitHealth`.
#
# + bucketId - Possition of the currrently used bucket
# + circuitHealth - Circuit Breaker health status
function updateLastUsedBucketId(int bucketId, CircuitHealth circuitHealth) {
    if (bucketId != circuitHealth.lastUsedBucketId) {
        resetBucketStats(circuitHealth, bucketId);
        circuitHealth.lastUsedBucketId = bucketId;
    }
}

# Switches circuit state from open to half open state when reset time exceeded.
#
# + circuitBreakerInferredConfig -  Configurations derived from `CircuitBreakerConfig`
# + circuitHealth - Circuit Breaker health status
# + currentState - current state of the circuit
# + return - Calculated state value of the circuit
function switchCircuitStateOpenToHalfOpenOnResetTime(CircuitBreakerInferredConfig circuitBreakerInferredConfig,
                                        CircuitHealth circuitHealth, CircuitState currentState) returns CircuitState {
    CircuitState currentCircuitState = currentState;
    if (currentState == CB_OPEN_STATE) {
        time:Time effectiveErrorTime = getEffectiveErrorTime(circuitHealth);
        int elapsedTime = time:currentTime().time - effectiveErrorTime.time;
        if (elapsedTime > circuitBreakerInferredConfig.resetTimeMillis) {
            currentCircuitState = CB_HALF_OPEN_STATE;
            log:printInfo("CircuitBreaker reset timeout reached. Circuit switched from OPEN to HALF_OPEN state.");
        }
    }
    return currentCircuitState;
}
