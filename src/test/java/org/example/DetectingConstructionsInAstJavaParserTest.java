package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DetectingConstructionsInAstJavaParserTest {
    @Test
    void searchIfStmtWithThenStmtNotBlockStmt() {
        String input = """
                public class AST {
                    public int sum(int a, int b) {
                        System.out.println(a);
                        System.out.println(b);
                        if (a == b)
                            return 2 * a;
                        if (a == 2) {
                            return 2;
                        }
                        if (b == 5)
                            return 5 * b;
                        return a + b;
                    }
                }
                """;
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> parseResult = parser.parse(input);
        assertTrue(parseResult.isSuccessful(), "Problems of parser are: " + parseResult.getProblems());

        CompilationUnit cu = parseResult.getResult().orElseThrow();

        List<Statement> listThenNotBlock = new ArrayList<>();

        Class<IfStmt> a = IfStmt.class;
        Class<BlockStmt> b = BlockStmt.class;
        cu.walk(a,ifStmt ->  {
            Statement thenBlock = ifStmt.getThenStmt();
            if (!(b.isInstance(thenBlock))) {
                listThenNotBlock.add(thenBlock);
            }
        });

        assertEquals(2, listThenNotBlock.size());
    }
}
