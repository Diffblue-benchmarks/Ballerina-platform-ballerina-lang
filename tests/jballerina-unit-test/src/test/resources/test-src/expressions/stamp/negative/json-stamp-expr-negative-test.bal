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

type EmployeeObj object {
    public int age = 10;
    public string name = "raj";

};


function stampJSONToXML() returns xml {

    json jsonValue = { name: "Raja", age: 25, salary: 20000 };

    xml xmlValue = xml.stamp(jsonValue);
    return xmlValue;
}

function stampJSONToObject() returns EmployeeObj {

    json employee = { name: "John", age: 23 };
    EmployeeObj employeeObj = EmployeeObj.stamp(employee);

    return employeeObj;
}

function stampJSONToTuple() returns [string, string] {

    json jsonValue = { name: "Raja", status: "single" };
    [string, string] tupleValue = [string, string].stamp(jsonValue);

    return tupleValue;
}
