// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public type Person object {
    private int salary;

    public function __init(int salary) {
        self.salary = salary;
    }

    private function decrementAndUpdateSalary(int amount) returns int;

    private function incrementSalary(int amount) {
        self.salary += amount;
    }

    public function incrementAndGetSalary(int amount) returns int {
        self.incrementSalary(amount);
        return self.salary;
    }

    public function decrementAndGetSalary(int amount) returns int {
        return self.decrementAndUpdateSalary(amount);
    }

    function decrementAndGetSalaryOuterMethod(int amount) returns int;
};

private function Person.decrementAndUpdateSalary(int amount) returns int {
    self.salary -= amount;
    return self.salary;
}

function Person.decrementAndGetSalaryOuterMethod(int amount) returns int {
    return self.decrementAndUpdateSalary(amount);
}

function testPrivateMethodAccess() returns [int, int, int] {
    Person person = new(10000);
    var result1 = person.incrementAndGetSalary(5000);
    var result2 = person.decrementAndGetSalary(2500);
    var result3 = person.decrementAndGetSalaryOuterMethod(1000);
    return [result1, result2, result3];
}
