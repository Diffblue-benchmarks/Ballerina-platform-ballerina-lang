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

type ErrorHandlerGenerator object {
    jvm:MethodVisitor mv;
    BalToJVMIndexMap indexMap;

    public function __init(jvm:MethodVisitor mv, BalToJVMIndexMap indexMap) {
        self.mv = mv;
        self.indexMap = indexMap;
    }

    function genPanic(bir:Panic panicTerm) {
        int errorIndex = self.getJVMIndexOfVarRef(panicTerm.errorOp.variableDcl);
        self.mv.visitVarInsn(ALOAD, errorIndex);
        self.mv.visitInsn(ATHROW);
    }

    function generateTryIns(jvm:Label endLabel, jvm:Label handlerLabel) {
        jvm:Label startLabel = new;
        self.mv.visitTryCatchBlock(startLabel, endLabel, handlerLabel, ERROR_VALUE);
        self.mv.visitLabel(startLabel);
    }

    function generateTryInsForTrap(bir:ErrorEntry currentEE, jvm:Label endLabel, jvm:Label handlerLabel,
                                   jvm:Label jumpLabel) {
        jvm:Label startLabel = new;
        self.mv.visitTryCatchBlock(startLabel, endLabel, handlerLabel, ERROR_VALUE);
        jvm:Label temp = new;
        self.mv.visitLabel(temp);
        int lhsIndex = self.getJVMIndexOfVarRef(<bir:VariableDcl>currentEE.errorOp.variableDcl);
        self.mv.visitVarInsn(ALOAD, lhsIndex);
        self.mv.visitJumpInsn(IFNONNULL, jumpLabel);
        self.mv.visitLabel(startLabel);
    }

    function generateCatchInsForTrap(bir:ErrorEntry currentEE, jvm:Label endLabel, jvm:Label handlerLabel,
                                     jvm:Label jumpLabel) {
        self.mv.visitLabel(endLabel);
        self.mv.visitJumpInsn(GOTO, jumpLabel);
        self.mv.visitLabel(handlerLabel);
        int lhsIndex = self.getJVMIndexOfVarRef(<bir:VariableDcl>currentEE.errorOp.variableDcl);
        self.mv.visitVarInsn(ASTORE, lhsIndex);
        self.mv.visitLabel(jumpLabel);
    }

    function printStackTraceFromFutureValue(jvm:MethodVisitor mv) {
        mv.visitInsn(DUP);
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, "strand", io:sprintf("L%s;", STRAND));
        mv.visitFieldInsn(GETFIELD, STRAND, "scheduler", io:sprintf("L%s;", SCHEDULER)); 
        mv.visitMethodInsn(INVOKEVIRTUAL, SCHEDULER, SCHEDULER_START_METHOD, "()V", false);
        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, PANIC_FIELD, io:sprintf("L%s;", THROWABLE));
        jvm:Label labelIf = new;
        mv.visitJumpInsn(IFNULL, labelIf);
        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, PANIC_FIELD, io:sprintf("L%s;", THROWABLE));
        mv.visitMethodInsn(INVOKEVIRTUAL, THROWABLE, PRINT_STACK_TRACE_METHOD, "()V", false);
        mv.visitInsn(RETURN);
        mv.visitLabel(labelIf);
    }

    function getJVMIndexOfVarRef(bir:VariableDcl varDcl) returns int {
        return self.indexMap.getIndex(varDcl);
    }
};
