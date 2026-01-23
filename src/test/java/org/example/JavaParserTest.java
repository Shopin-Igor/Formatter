package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.IfStmt;
import org.junit.jupiter.api.Test;

import java.util.List;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaParserTest {

    @Test
    void isAnyWordsAtNodesAST() {

        String s = """
            public class AST {
                public int sum(int a, int b) {
                    System.out.println(a);
                    System.out.println(b);
                    if (a == b) {
                        return 2 * a;
                    }
                    if (a == 2) {
                        return 2;
                    }
                    if (b == 5) {
                        return 5 * b;
                    }
                    return a + b;
                }
            }
            """;
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> resultOfParse = parser.parse(s);
        assertTrue(resultOfParse.isSuccessful(), ()->"Problems of parsing are: " + resultOfParse.getProblems());
    
        CompilationUnit cu = resultOfParse.getResult().orElseThrow();

        assertTrue(cu.findFirst(IfStmt.class).isPresent(), "IfStmt not found");

        List<IfStmt> ifs = cu.findAll(IfStmt.class);
        System.out.println("ifs.size() = " + ifs.size());

        for (IfStmt iff : ifs) {
            System.out.println(iff);
        }


        cu.walk(IfStmt.class, ifStmt -> {
                assertEquals(Optional.empty(), ifStmt.getElseStmt());
        });

    }
}