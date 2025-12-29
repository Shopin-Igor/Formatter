package org.example;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SqlAstServiceTest {

    @Test
    void parsesSimpleSelect()
    {
        String sql = "SELECT name, age FROM users WHERE age > 18";

        TSqlLexer lexer = new TSqlLexer(CharStreams.fromString(sql));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TSqlParser parser = new TSqlParser(tokens);

        ParseTree tree = parser.tsql_file();

        String treeStr = tree.toStringTree();
//        System.out.println(treeStr);



        assertTrue(treeStr.contains("SELECT"), "SELECT expected be at tree");
        assertTrue(treeStr.contains("FROM"), "FROM expected be at tree");
        assertTrue(treeStr.contains("WHERE"), "WHERE  expected be at tree");
        assertTrue(treeStr.contains("name"), "field 'name' expected be at tree");
        assertTrue(treeStr.contains("age"), "field 'age' expected be at tree");
        assertTrue(treeStr.contains("users"), "table 'users' expected be at tree");
    }
}
