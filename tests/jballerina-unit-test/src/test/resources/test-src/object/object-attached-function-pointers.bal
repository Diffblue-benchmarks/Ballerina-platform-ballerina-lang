type Person object {
    public int age = 3;
    public string name = "sample name";

    private int year = 5;
    private string month = "february";

    function attachedFn1(int a, float b) returns (int) {
        return 7 + a + <int>b;
    }

    function attachedFn2() returns (function (int, float) returns (int)) {
        var foo = function (int a, float b) returns (int) {
            return 7 + a + <int>b;
        };
        return foo;
    }

    function attachedFn3(int a, float b) returns (int);

    function attachedFn4() returns (function (int, float) returns (int));

    function attachedFn5(int a, float b) returns (function (float) returns ((function (boolean) returns (int)))) {
        var fooOut = function (float f) returns (function (boolean) returns (int)) {
            var fooIn = function (boolean boo) returns (int) {
                return 7 + a + <int>b + <int>f;
            };
            return fooIn;
        };
        return fooOut;
    }

    function attachedFn6(int a, float b) returns (int) {
        function (int a, float b) returns (int) foo = (x, y) => self.attachedFn3(x, y);
        return a + <int>b + foo.call(43, 10.2);
    }

    function attachedFn7(int a, float b) returns (int) {
        function (int a, float b) returns (int) foo = (x, y) => self.attachedFn3(x, y);
        return a + <int>b + foo.call(43, 10.2);
    }
};


function Person.attachedFn3(int a, float b) returns (int) {
    return a + <int>b;
}

function Person.attachedFn4() returns (function (int, float) returns (int)) {
    var foo = function (int a, float b) returns (int) {
        return 7 + a + <int>b;
    };
    return foo;
}

function test1() returns (int) {
    Person p = new;
    function (int a, float b) returns (int) foo = (x, y) => p.attachedFn1(x, y);
    return foo.call(43, 10.2);
}

function test2() returns (int) {
    Person p = new;
    function () returns (function (int, float) returns (int)) foo = () => p.attachedFn2();
    var bar = foo.call();
    return bar.call(43, 10.2);
}

function test3() returns (int) {
    Person p = new;
    function (int a, float b) returns (int) foo = (x, y) => p.attachedFn3(x, y);
    return foo.call(43, 10.2);
}

function test4() returns (int) {
    Person p = new;
    function () returns (function (int, float) returns (int)) foo = () => p.attachedFn4();
    var bar = foo.call();
    return bar.call(43, 10.2);
}

function test5() returns (int) {
    Person p = new;
    function (int a, float b) returns (function (float) returns ((function (boolean) returns (int)))) foo = (x, y) => p.attachedFn5(x, y);
    var bar = foo.call(43, 10.2);
    var baz = bar.call(5.3);
    return baz.call(true);
}

function test6() returns (int) {
    Person p = new;
    function (int a, float b) returns (int) foo = (x, y) => p.attachedFn6(x, y);
    return foo.call(43, 10.2);
}

function test7() returns (int) {
    Person p = new;
    function (int a, float b) returns (int) foo = (x, y) => p.attachedFn7(x, y);
    return foo.call(43, 10.2);
}

public type FooObj object {
    private (function(string[]) returns string) fp1;
    private (function(int[]) returns int) fp2;

    function __init ((function(string[]) returns string) fp1, (function(int[]) returns int) fp2){
        self.fp1 = fp1;
        self.fp2 = fp2;
        string[] s = ["abc", "afg"];
        int[] i = [1,2,3,4,5];
        string a = fp1.call(s);
        int b = fp2.call(i);
    }

    public function processStrArray(string[] vals) returns string{
        return self.fp1.call(vals);
    }

    public function processIntArray(int[] vals) returns int{
        return self.fp2.call(vals);
    }
};


function test8() returns [string, int] {
    string[] s = ["B", "A"];
    int[] i = [1,2,3,4,5];
    var foo = function (string[] v) returns string { return v[1];};
    var bar = function (int[] v) returns int { return v[1];};
    FooObj fooObj = new (foo, bar);
    _ = fooObj.processStrArray(s);
    (function (string[] vals) returns string) x = (vals) => fooObj.processStrArray(vals);
    string q = x.call(s);
    _ = fooObj.processIntArray(i);
    (function (int[] vals) returns int) y = (vals) => fooObj.processIntArray(vals);
    int r = y.call(i);

    return[q, r];
}

function test9() returns string {
    string[] vals = ["finally", "ballerina"];
    O1 o1 = new (function (string[] v) returns string { return vals[0]; });
    O2 o2 = new(x => o1.process(x));
    O3 o3 = new(x => o2.process(x));
    O4 o4 = new(x => o3.process(x));
    O5 o5 = new(x => o4.process(x));
    return o5.process(vals);
}

public type O1 object {
    private (function(string[]) returns string) fpO1;

    function __init ((function(string[]) returns string) fpO1) {
        self.fpO1 = fpO1;
    }

    public function process(string[] vals) returns string{
        return self.fpO1.call(vals);
    }
};

public type O2 object {
    private (function(string[]) returns string) fpO2;

    function __init ((function(string[]) returns string) fpO2) {
        self.fpO2 = fpO2;
    }

    public function process(string[] vals) returns string{
        return self.fpO2.call(vals);
    }
};

public type O3 object {
    private (function(string[]) returns string) fpO3;

    function __init ((function(string[]) returns string) fpO3) {
        self.fpO3 = fpO3;
    }

    public function process(string[] vals) returns string{
        return self.fpO3.call(vals);
    }
};

public type O4 object {
    private (function(string[]) returns string) fpO4;

    function __init ((function(string[]) returns string) fpO4) {
        self.fpO4 = fpO4;
    }

    public function process(string[] vals) returns string{
        return self.fpO4.call(vals);
    }
};

public type O5 object {
    private (function(string[]) returns string) fpO5;

    function __init ((function(string[]) returns string) fpO5) {
        self.fpO5 = fpO5;
    }

    public function process(string[] vals) returns string {
        return self.fpO5.call(vals);
    }
};
