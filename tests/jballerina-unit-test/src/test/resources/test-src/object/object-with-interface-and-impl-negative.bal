public function testObjectWithInterface () returns [int, string] {
    Person p = new Person();
    return [p.attachInterface(7), p.month];
}

type Person object {
    public int age = 10;
    string month = "february";

    function attachInterface(int add) returns int;
};

function Person.attachInterface(int add) returns int {
    int count = age + add;
    return count;
}

function Person.attachInterface(int add) returns int {
    return add;
}

type Employee object {
    public int age = 10;
    private string month = "february";
};

function Employee.attachInterfaceFunc(int add) returns int {
    int count = age + add;
    return count;
}
