package org.example.sqlLikeRequest;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import org.example.SqlLikeRequestParser;

import java.util.List;

public final class QueryExecutor {

    private final TypeRegistry typeRegistry;

    public QueryExecutor(TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    public List<Match> execute(SqlLikeRequestParser.QueryContext queryTree, CompilationUnit cu) {
        SqlLikeRequestParser.SelectStmtContext select = queryTree.selectStmt();

        String selectTypeName = select.selectTarget().getText();
        Class<? extends Node> selectType = typeRegistry.resolve(selectTypeName);

        List<? extends Node> candidates = cu.findAll(selectType);

        if (select.expr() == null) {
            return candidates.stream().map(Match::from).toList();
        }

        ExprEvaluator evaluator = new ExprEvaluator(typeRegistry, selectTypeName);

        return candidates.stream()
                .filter(n -> evaluator.eval(select.expr(), n))
                .map(Match::from)
                .toList();
    }
}
