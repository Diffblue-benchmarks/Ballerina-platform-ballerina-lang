/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.langserver.signature;

import org.ballerinalang.langserver.common.CommonKeys;
import org.ballerinalang.langserver.common.LSNodeVisitor;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.compiler.LSServiceOperationContext;
import org.ballerinalang.langserver.completions.SymbolInfo;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.tree.TopLevelNode;
import org.eclipse.lsp4j.Position;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolResolver;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangService;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangCompoundAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangForeach;
import org.wso2.ballerinalang.compiler.tree.statements.BLangIf;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTransaction;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWhile;
import org.wso2.ballerinalang.compiler.tree.types.BLangObjectTypeNode;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tree visitor to traverse through the ballerina node tree and find the scope of a given cursor position.
 */
public class SignatureTreeVisitor extends LSNodeVisitor {
    private SymbolEnv symbolEnv;
    private SymbolResolver symbolResolver;
    private boolean terminateVisitor = false;
    private SymbolTable symTable;
    private LSServiceOperationContext lsContext;
    private Deque<DiagnosticPos> blockPositionStack;

    /**
     * Public constructor.
     * @param textDocumentServiceContext    Document service context for the signature operation
     */
    public SignatureTreeVisitor(LSServiceOperationContext textDocumentServiceContext) {
        blockPositionStack = new ArrayDeque<>();
        this.lsContext = textDocumentServiceContext;
        init(lsContext.get(DocumentServiceKeys.COMPILER_CONTEXT_KEY));
    }

    private void init(CompilerContext compilerContext) {
        symTable = SymbolTable.getInstance(compilerContext);
        symbolResolver = SymbolResolver.getInstance(compilerContext);
    }

    @Override
    public void visit(BLangPackage pkgNode) {
        SymbolEnv pkgEnv = symTable.pkgEnvMap.get(pkgNode.symbol);
        List<TopLevelNode> topLevelNodes = CommonUtil.getCurrentFileTopLevelNodes(pkgNode, lsContext);

        topLevelNodes.stream()
                .filter(CommonUtil.checkInvalidTypesDefs())
                .forEach(topLevelNode -> acceptNode((BLangNode) topLevelNode, pkgEnv));
    }

    @Override
    public void visit(BLangTypeDefinition typeDefinition) {
        acceptNode(typeDefinition.typeNode, symbolEnv);
    }

    @Override
    public void visit(BLangObjectTypeNode objectTypeNode) {
        objectTypeNode.functions.stream()
                .filter(bLangFunction -> !bLangFunction.flagSet.contains(Flag.INTERFACE))
                .forEach(bLangFunction -> acceptNode(bLangFunction, symbolEnv));
    }

    @Override
    public void visit(BLangFunction funcNode) {
        BSymbol funcSymbol = funcNode.symbol;
        SymbolEnv funcEnv = SymbolEnv.createFunctionEnv(funcNode, funcSymbol.scope, symbolEnv);
        blockPositionStack.push(funcNode.pos);
        this.acceptNode(funcNode.body, funcEnv);
        blockPositionStack.pop();
        // Process workers
        if (terminateVisitor && !funcNode.workers.isEmpty()) {
            terminateVisitor = false;
        }
        funcNode.workers.forEach(e -> this.acceptNode(e, funcEnv));
    }

    @Override
    public void visit(BLangService serviceNode) {
        BLangObjectTypeNode serviceType = (BLangObjectTypeNode) serviceNode.serviceTypeDefinition.typeNode;
        List<BLangNode> serviceContent = new ArrayList<>();
        SymbolEnv serviceEnv = SymbolEnv.createPkgLevelSymbolEnv(serviceNode, serviceType.symbol.scope, symbolEnv);
        List<BLangFunction> serviceFunctions = ((BLangObjectTypeNode) serviceNode.serviceTypeDefinition.typeNode)
                .getFunctions();
        List<BLangSimpleVariable> serviceFields = serviceType.getFields().stream()
                .map(simpleVar -> (BLangSimpleVariable) simpleVar)
                .collect(Collectors.toList());
        serviceContent.addAll(serviceFunctions);
        serviceContent.addAll(serviceFields);
        serviceContent.sort(new CommonUtil.BLangNodeComparator());
        serviceContent.forEach(serviceField -> this.acceptNode(serviceField, serviceEnv));
    }

    @Override
    public void visit(BLangBlockStmt blockNode) {
        SymbolEnv blockEnv = SymbolEnv.createBlockEnv(blockNode, symbolEnv);
        blockNode.stmts.forEach(stmt -> this.acceptNode(stmt, blockEnv));
        if (!terminateVisitor && this.isCursorWithinBlock()) {
            Map<Name, Scope.ScopeEntry> visibleSymbolEntries = symbolResolver.getAllVisibleInScopeSymbols(blockEnv);
            this.populateSymbols(visibleSymbolEntries);
        }
    }

    @Override
    public void visit(BLangSimpleVariableDef varDefNode) {
        this.acceptNode(varDefNode.var, symbolEnv);
    }

    @Override
    public void visit(BLangIf ifNode) {
        this.blockPositionStack.push(ifNode.pos);
        this.acceptNode(ifNode.body, symbolEnv);
        this.blockPositionStack.pop();

        if (ifNode.elseStmt != null) {
            this.blockPositionStack.push(ifNode.elseStmt.pos);
            acceptNode(ifNode.elseStmt, symbolEnv);
            this.blockPositionStack.pop();
        }
    }

    @Override
    public void visit(BLangForeach foreach) {
        this.blockPositionStack.push(foreach.pos);
        this.acceptNode(foreach.body, symbolEnv);
        this.blockPositionStack.pop();
    }

    @Override
    public void visit(BLangWhile whileNode) {
        this.blockPositionStack.push(whileNode.pos);
        this.acceptNode(whileNode.body, symbolEnv);
        this.blockPositionStack.pop();
    }

    @Override
    public void visit(BLangTransaction transactionNode) {
        this.blockPositionStack.push(transactionNode.transactionBody.pos);
        this.acceptNode(transactionNode.transactionBody, symbolEnv);
        this.blockPositionStack.pop();

        if (transactionNode.onRetryBody != null) {
            this.blockPositionStack.push(transactionNode.onRetryBody.pos);
            this.acceptNode(transactionNode.onRetryBody, symbolEnv);
            this.blockPositionStack.pop();
        }
    }

    @Override
    public void visit(BLangCompoundAssignment assignment) {
        this.blockPositionStack.push(assignment.pos);
        this.acceptNode(assignment.varRef, symbolEnv);
        this.blockPositionStack.pop();

        if (assignment.expr != null) {
            this.blockPositionStack.push(assignment.expr.pos);
            acceptNode(assignment.expr, symbolEnv);
            this.blockPositionStack.pop();
        }
    }

    // Private Methods
    private void acceptNode(BLangNode node, SymbolEnv env) {
        if (this.terminateVisitor) {
            return;
        }

        SymbolEnv prevEnv = this.symbolEnv;
        this.symbolEnv = env;
        node.accept(this);
        this.symbolEnv = prevEnv;
    }

    private boolean isCursorWithinBlock() {
        Position cursorPosition = this.lsContext.get(DocumentServiceKeys.POSITION_KEY).getPosition();
        DiagnosticPos blockPosition = CommonUtil.toZeroBasedPosition(blockPositionStack.peek());
        int cursorLine = cursorPosition.getLine();
        int cursorColumn = cursorPosition.getCharacter();
        int nodeStrtLine = blockPosition.getStartLine();
        int nodeEndLine = blockPosition.getEndLine();
        int nodeStrtColumn = blockPosition.getStartColumn();
        int nodeEndColumn = blockPosition.getEndColumn();
        
        /*
          node Start ->{ <cursor_position> }<- node End.
          If the cursor is within the scope of the node, then the cursor should be located after the node start
          and before the node end
         */
        int isAfterBlockStart = nodeStrtLine == cursorLine ? cursorColumn - nodeStrtColumn : cursorLine - nodeStrtLine;
        int isBeforeBlockEnd = nodeEndLine == cursorLine ? nodeEndColumn - cursorColumn : nodeEndLine - cursorLine;

        return isAfterBlockStart > 0 && isBeforeBlockEnd > 0;
    }

    /**
     * Populate the symbols.
     * @param symbolEntries symbol entries
     */
    private void populateSymbols(Map<Name, Scope.ScopeEntry> symbolEntries) {
        this.terminateVisitor = true;
        List<SymbolInfo> visibleSymbols = new ArrayList<>();

        symbolEntries.forEach((k, v) -> visibleSymbols.add(new SymbolInfo(k.getValue(), v)));
        lsContext.put(CommonKeys.VISIBLE_SYMBOLS_KEY, visibleSymbols);
    }
}
