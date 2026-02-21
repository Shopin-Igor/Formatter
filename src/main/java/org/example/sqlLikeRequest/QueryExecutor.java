package org.example.sqlLikeRequest;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.example.SqlLikeRequestParserParser;

import java.util.List;

public final class QueryExecutor {

    private final TypeRegistry typeRegistry;

    public QueryExecutor(TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    public List<Match> execute(SqlLikeRequestParserParser.QueryContext queryTree, CompilationUnit cu) {
        SqlLikeRequestParserParser.FormatStmtContext request = queryTree.formatStmt();
        SqlLikeRequestParserParser.SelectStmtContext selectPart = request.selectStmt();

        String selectTypeName = selectPart.selectTarget().typeName().getText();

        TerminalNode aliasNode = selectPart.selectTarget().REPLACEMENT();
        String rootRef = (aliasNode != null) ? aliasNode.getText() : selectTypeName;


        Class<? extends Node> selectType = typeRegistry.resolve(selectTypeName);
        List<? extends Node> candidates = cu.findAll(selectType);


        ExprEvaluator evaluator = new ExprEvaluator(typeRegistry, rootRef);

        boolean hasFormat = request.FORMAT() != null && request.formatString() != null;
        OutputRenderer renderer = hasFormat ? new OutputRenderer(rootRef) : null;

        return candidates.stream()
                .filter(n -> selectPart.expr() == null || evaluator.eval(selectPart.expr(), n))
                .map(n -> {
                    if (!hasFormat) return Match.from(n);
                    String formatted = renderer.render(request.formatString(), n);
                    return Match.from(n, formatted);
                })
                .toList();
    }
}