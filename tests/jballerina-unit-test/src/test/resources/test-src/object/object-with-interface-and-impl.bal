public function testObjectWithInterface () returns [int, string] {
    Person p = new Person();
    return [p.attachInterface(7), p.month];
}

type Person object {
    public int age = 10;

    string month = "february";

    function attachInterface(int add) returns int;

    public function attachInterface2(int add) returns int;
};

function Person.attachInterface(int add) returns int {
    int count = self.age + add;
    return count;
}

public function Person.attachInterface2(int add) returns int {
    int count = self.age + add;
    return count;
}
