package org.example.sqlLikeRequest;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.antlr.v4.runtime.*;
import org.example.SqlLikeRequestLexer;
import org.example.SqlLikeRequestParserParser;

import java.util.List;

public final class QueryRunner {

    private final TypeRegistry types = new TypeRegistry();

    public List<Match> run(String dsl, String javaSource) {
        SqlLikeRequestParserParser.QueryContext query = parseDsl(dsl);
        CompilationUnit cu = parseJava(javaSource);
        return new QueryExecutor(types).execute(query, cu);
    }

    private SqlLikeRequestParserParser.QueryContext parseDsl(String dsl) {
        CharStream input = CharStreams.fromString(dsl);
        SqlLikeRequestLexer lexer = new SqlLikeRequestLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        SqlLikeRequestParserParser parser = new SqlLikeRequestParserParser(tokens);

        parser.setErrorHandler(new BailErrorStrategy());
        return parser.query();
    }

    private CompilationUnit parseJava(String code) {
        JavaParser jp = new JavaParser();
        ParseResult<CompilationUnit> pr = jp.parse(code);
        if (!pr.isSuccessful() || pr.getResult().isEmpty()) {
            throw new IllegalArgumentException("Java parse problems: " + pr.getProblems());
        }
        return pr.getResult().get(); // не .orElseThrow тк результат уже точно будет
    }
}
