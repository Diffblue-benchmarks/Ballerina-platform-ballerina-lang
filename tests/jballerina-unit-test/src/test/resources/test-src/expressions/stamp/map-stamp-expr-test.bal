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

type Employee record {
    string name;
    string status;
    string batch;
};

type Teacher record {
    string name;
    int age;
    string status;
    string batch;
    string school;
};

type IntRecord record{
    int a;
    int b;
};

type Person record {
    string name = "";
    int age = 0;
    Person? parent = ();
};

type Engineer record {
    string name = "";
    int age = 0;
    Engineer? parent = ();
};

//----------------------------Map Stamp -------------------------------------------------------------

function stampIntMapToRecord() returns IntRecord|error {
    map<int> m = { "a": 1, "b": 2 };
    IntRecord|error intRecord = IntRecord.stamp(m);

    return intRecord;
}

function stampIntMapToJSON() returns json|error {
    map<int> m = { "a": 1, "b": 2 };
    json|error jsonValue = json.stamp(m);

    return jsonValue;
}

function stampIntMapToAnydata() returns anydata {
    map<int> m = { "a": 1, "b": 2 };
    anydata anydataValue = anydata.stamp(m);

    return anydataValue;
}

function stampIntMapToIntMap() returns map<int> {
    map<int> m = { "a": 1, "b": 2 };
    map<int> mapValue = map<int>.stamp(m);

    return mapValue;
}

function stampIntMapToAnydataMap() returns map<anydata> {
    map<int> m = { "a": 1, "b": 2 };
    map<anydata> mapValue = map<anydata>.stamp(m);

    return mapValue;
}


function stampAnydataMapToIntMap() returns map<int>|error {
    map<anydata> m = { "a": 1, "b": 2 };
    map<int>|error mapValue = map<int>.stamp(m);

    return mapValue;
}


function stampAnydataMapToStringMap() returns map<string>|error {
    map<anydata> m = { firstName: "mohan", lastName: "raj" };
    map<string>|error mapValue = map<string>.stamp(m);

    return mapValue;
}


function stampAnydataMapToStringMapWithoutExplicitConstraintType() returns map<string>|error {
    map<anydata> m = { firstName: "mohan", lastName: "raj" };
    map<string>|error mapValue = map<string>.stamp(m);

    return mapValue;
}

function stampAnydataMapToRecord() returns Teacher|error {
    map<anydata> anydataMap = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };

    Teacher|error teacherValue = Teacher.stamp(anydataMap);

    return teacherValue;
}

function stampAnydataMapToJSON() returns json|error {
    map<anydata> anydataMap = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };

    json|error jsonValue = json.stamp(anydataMap);

    return jsonValue;
}

function stampAnydataMapToAnydata() returns anydata {
    map<anydata> anydataMap = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };

    anydata anydataValue = anydata.stamp(anydataMap);

    return anydataValue;
}

function stampAnydataMapToSimilarOpenRecordMap() returns map<Employee>|error {
    Teacher p1 = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };
    Teacher p2 = { name: "Mohan", age: 30, status: "single", batch: "LK2014", school: "Hindu College" };

    map<anydata> teacherMap = { "a": p1, "b": p2 };
    map<Employee>|error mapValue = map<Employee>.stamp(teacherMap);

    return mapValue;
}


function stampAnydataMapToRecordMap() returns map<Teacher>|error {
    Teacher p1 = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };
    Teacher p2 = { name: "Mohan", age: 30, status: "single", batch: "LK2014", school: "Hindu College" };

    map<anydata> teacherMap = { "a": p1, "b": p2 };
    map<Teacher>|error mapValue = map<Teacher>.stamp(teacherMap);

    return mapValue;
}

function stampAnydataMapToJSONMap() returns map<json>|error {
    Teacher p1 = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };
    Teacher p2 = { name: "Mohan", age: 30, status: "single", batch: "LK2014", school: "Hindu College" };

    map<anydata> teacherMap = { "a": p1, "b": p2 };
    map<json>|error jsonValue = map<json>.stamp(teacherMap);

    return jsonValue;
}


function stampRecordMapToAnydataMap() returns map<anydata> {
    Teacher p1 = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };
    Teacher p2 = { name: "Mohan", age: 30, status: "single", batch: "LK2014", school: "Hindu College" };

    map<Teacher> teacherMap = { "a": p1, "b": p2 };
    map<anydata> mapValue = map<anydata>.stamp(teacherMap);

    return mapValue;
}

function stampRecordMapToSimilarOpenRecordMap() returns map<Employee> {
    Teacher p1 = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };
    Teacher p2 = { name: "Mohan", age: 30, status: "single", batch: "LK2014", school: "Hindu College" };

    map<Teacher> teacherMap = { "a": p1, "b": p2 };
    map<Employee> mapValue = map<Employee>.stamp(teacherMap);

    return mapValue;
}

function stampRecordMapToJSONMap() returns map<json>|error {
    Teacher p1 = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };
    Teacher p2 = { name: "Mohan", age: 30, status: "single", batch: "LK2014", school: "Hindu College" };

    map<Teacher> teacherMap = { "a": p1, "b": p2 };
    map<json>|error mapValue = map<json>.stamp(teacherMap);

    return mapValue;

}

function stampJSONMapToRecordMap() returns map<Employee>|error {
    map<json> teacherMap = { "a": { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" },
        "b": { name: "Mohan", age: 30, status: "single", batch: "LK2014", school: "Hindu College" } };
    map<Employee>|error mapValue = map<Employee>.stamp(teacherMap);

    return mapValue;
}

function stampRecordTypeMultiDimensionMap() returns map<map<Employee>> {
    Teacher p1 = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };
    Teacher p2 = { name: "Mohan", age: 30, status: "single", batch: "LK2014", school: "Hindu College" };

    map<Teacher> teacherMap = { "a": p1, "b": p2 };
    map<map<Teacher>> multiMap = { "aa": teacherMap, "bb": teacherMap };

    map<map<Employee>> mapValue = map<map<Employee>>.stamp(multiMap);

    return mapValue;
}

function stampAnydataToIntMultiDimensionMap() returns map<map<map<int>>>|error {
    map<map<map<anydata>>> m = { "a": { "aa": { "aa": 11, "bb": 22 }, "bb": { "aa": 11, "bb": 22 } }, "b": { "aaa": { "aa":
    11, "bb": 22 }, "bbb": { "aa": 11, "bb": 22 } } };
    map<map<map<int>>>|error mapValue = map<map<map<int>>>.stamp(m);

    return mapValue;
}

function stampIntToAnydataMultiDimensionMap() returns map<map<map<anydata>>> {
    map<map<map<int>>> m = { "a": { "aa": { "aa": 11, "bb": 22 }, "bb": { "aa": 11, "bb": 22 } }, "b": { "aaa": { "aa":
    11, "bb": 22 }, "bbb": { "aa": 11, "bb": 22 } } };
    map<map<map<anydata>>> mapValue = map<map<map<anydata>>>.stamp(m);

    return mapValue;
}

function stampConstraintMapToAnydata() returns anydata {
    Teacher p1 = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };
    Teacher p2 = { name: "Mohan", age: 30, status: "single", batch: "LK2014", school: "Hindu College" };

    map<Teacher> teacherMap = { "a": p1, "b": p2 };
    anydata anydataValue = anydata.stamp(teacherMap);

    return anydataValue;
}

function stampConstraintMapToUnion() returns map<Teacher>|xml {
    Teacher p1 = { name: "Raja", age: 25, status: "single", batch: "LK2014", school: "Hindu College" };
    Teacher p2 = { name: "Mohan", age: 30, status: "single", batch: "LK2014", school: "Hindu College" };

    map<Teacher> teacherMap = { "a": p1, "b": p2 };
    map<Teacher>|xml anydataValue = map<Teacher>|xml.stamp(teacherMap);

    return anydataValue;
}

//---------------------------------- Negative Test cases -----------------------------------------------------------

type EmployeeClosedRecord record {|
    string name;
    string status;
    string batch;
|};

function stampMapToRecordNegative() returns EmployeeClosedRecord|error {
    map<string> m = { name: "Raja", status: "single", batch: "LK2014", school: "Hindu College" };
    EmployeeClosedRecord|error employee = EmployeeClosedRecord.stamp(m);

    return employee;
}

function testStampRecordToRecordWithCyclicValueReferences() returns Engineer|error {
    Person p = { name: "Waruna", age: 25, parent: () };
    Person p2 = { name: "Milinda", age: 25, parent:p };
    p.parent = p2;
    Engineer|error e =  trap Engineer.stamp(p); // Cyclic value will be check with isLikeType method.
    return e;
}

function testStampRecordToJsonWithCyclicValueReferences() returns json|error {
    Person p = { name: "Waruna", age: 25, parent: () };
    Person p2 = { name: "Milinda", age: 25, parent:p };
    p.parent = p2;
    json|error j =  trap json.stamp(p); // Cyclic value will be check with isLikeType method.
    return j;
}

function testStampRecordToMapWithCyclicValueReferences() returns map<anydata>|error {
    Person p = { name: "Waruna", age: 25, parent: () };
    Person p2 = { name: "Milinda", age: 25, parent:p };
    p.parent = p2;
    map<anydata>|error m =  trap map<anydata>.stamp(p.clone()); // Cyclic value will be check when stamping the value.
    return m;
}
