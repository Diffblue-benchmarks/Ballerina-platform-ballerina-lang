public type InvalidNameError error<string, record { string companyName; }>;

function getQuote(string name) returns (float|InvalidNameError) {

    if (name == "FOO") {
        return 10.5;
    } else if (name == "BAR") {
        return 11.5;
    }

    InvalidNameError err = error("invalid name", {companyName: name });
    return err;
}

function testReturnError() returns [string, string, string, string]|error {

    string a;
    string b;
    string c;
    string d;

    float quoteValue;
    // Special identifier "=?" will be used to ignore values.

    quoteValue = check getQuote("FOO");
    a = "FOO:" + quoteValue;

    // Ignore error.
    var r = getQuote("QUX");

    if (r is float) {
        b = "QUX:" + r;
    } else {
        b = "QUX:ERROR";
    }

    // testing for errors.
    // error occurred. Recover from the error by assigning 0.
    var q = getQuote("BAZ");

    if (q is float) {
        c = "BAZ:" + quoteValue;
    } else {
        quoteValue = 0.0;
        c = "BAZ:" + quoteValue;
    }

    var p = getQuote("BAR");

    if (p is float) {
        d = "BAR:" + p;
    } else {
        quoteValue = 0.0;
        d = "BAR:ERROR";
    }

    return [a,b,c,d];
}

function testReturnAndThrowError() returns (string){
    error? e = trap checkAndThrow();

    if (e is error) {
        return e.reason();
    }

    return "OK";
}

function checkAndThrow(){
    float qVal;
    var p = getQuote("BAZ");

    if (p is float) {
        qVal = p;
    } else {
        panic p;
    }
}
