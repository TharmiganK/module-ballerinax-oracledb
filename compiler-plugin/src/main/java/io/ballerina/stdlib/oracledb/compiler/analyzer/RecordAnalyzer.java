/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.stdlib.oracledb.compiler.analyzer;

import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.oracledb.compiler.Constants;
import io.ballerina.stdlib.oracledb.compiler.Utils;

import java.util.Optional;

import static io.ballerina.stdlib.oracledb.compiler.Constants.BALLERINAX;
import static io.ballerina.stdlib.oracledb.compiler.Constants.ORACLEDB;
import static io.ballerina.stdlib.oracledb.compiler.Utils.validateOptionConfig;

/**
 * Analyser for validation oracledb:Options.
 */
public class RecordAnalyzer implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        if (Utils.hasCompilationErrors(ctx)) {
            return;
        }

        Optional<Symbol> varSymOptional = ctx.semanticModel().symbol(ctx.node());
        if (varSymOptional.isPresent()) {
            TypeSymbol typeSymbol = ((VariableSymbol) varSymOptional.get()).typeDescriptor();

            if (isOracleDBOptionsRecord(typeSymbol, Constants.Options.NAME)) {
                Optional<MappingConstructorExpressionNode> recordNode = getRecordNode(ctx);
                if (recordNode.isEmpty()) {
                    return;
                }
                validateOptionConfig(ctx, recordNode.get());
            }
        }
    }

    private Optional<MappingConstructorExpressionNode> getRecordNode(SyntaxNodeAnalysisContext ctx) {
        // Initiated with a record
        Optional<ExpressionNode> optionalInitializer;
        if ((ctx.node() instanceof VariableDeclarationNode)) {
            // Function level variables
            optionalInitializer = ((VariableDeclarationNode) ctx.node()).initializer();
        } else {
            // Module level variables
            optionalInitializer = ((ModuleVariableDeclarationNode) ctx.node()).initializer();
        }
        if (optionalInitializer.isEmpty()) {
            return Optional.empty();
        }
        ExpressionNode initializer = optionalInitializer.get();
        if (!(initializer instanceof MappingConstructorExpressionNode)) {
            return Optional.empty();
        }
        return Optional.of((MappingConstructorExpressionNode) initializer);
    }

    private boolean isOracleDBOptionsRecord(TypeSymbol type, String recordName) {
        if (type.typeKind() == TypeDescKind.UNION) {
            return ((UnionTypeSymbol) type).memberTypeDescriptors().stream()
                    .filter(typeDescriptor -> typeDescriptor instanceof TypeReferenceTypeSymbol)
                    .map(typeReferenceTypeSymbol -> (TypeReferenceTypeSymbol) typeReferenceTypeSymbol)
                    .anyMatch(typeReferenceTypeSymbol ->
                            isOracleDBOptionsRecord(typeReferenceTypeSymbol, recordName));
        }
        if (type.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            return isOracleDBOptionsRecord((TypeReferenceTypeSymbol) type, recordName);
        }
        return false;
    }

    private boolean isOracleDBOptionsRecord(TypeReferenceTypeSymbol typeSymbol, String recordName) {
        if (typeSymbol.typeDescriptor().typeKind() == TypeDescKind.RECORD) {
            ModuleSymbol moduleSymbol = typeSymbol.getModule().get();
            return ORACLEDB.equals(moduleSymbol.getName().get()) && BALLERINAX.equals(moduleSymbol.id().orgName())
                    && typeSymbol.definition().getName().get().equals(recordName);
        }
        return false;
    }
}
