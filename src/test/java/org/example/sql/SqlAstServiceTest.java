//package org.example.sql;
//
//import org.antlr.v4.runtime.CharStreams;
//import org.antlr.v4.runtime.CommonTokenStream;
//import org.antlr.v4.runtime.tree.ParseTree;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class SqlAstServiceTest {
//
//    @Test
//    void parsesSimpleSelect() {
//        String sql = "SELECT name, age FROM users WHERE age > 18";
//
//        SqlLexer lexer = new SqlLexer(CharStreams.fromString(sql));
//        CommonTokenStream tokens = new CommonTokenStream(lexer);
//        SqlParser parser = new SqlParser(tokens);
//
//        ParseTree tree = parser.selectStmt();
//
//        String treeStr = tree.toStringTree();
//        System.out.println(treeStr);
//
//
//
//        assertTrue(treeStr.contains("SELECT"), "Ожидали SELECT в дереве");
//        assertTrue(treeStr.contains("FROM"), "Ожидали FROM в дереве");
//        assertTrue(treeStr.contains("WHERE"), "Ожидали WHERE в дереве");
//        assertTrue(treeStr.contains("name"), "Ожидали поле 'name' в дереве");
//        assertTrue(treeStr.contains("age"), "Ожидали поле 'age' в дереве");
//        assertTrue(treeStr.contains("users"), "Ожидали таблицу 'users' в дереве");
//    }
//}
