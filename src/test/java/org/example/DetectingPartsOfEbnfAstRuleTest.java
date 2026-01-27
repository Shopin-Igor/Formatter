package org.example;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.example.rules.RuleAstBuilder;
import org.example.rules.ast.NodePat;
import org.example.rules.ast.RuleAst;
import org.example.rules.ast.RuleDef;
import org.example.rules.ast.RuleRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DetectingPartsOfEbnfAstRuleTest {

    @Test
    void shouldParseIfStmtRuleToRuleAst_oldStyle() {
        String input = """
                <ifStmt> ::= IfStmt(
                  condition = <expr>,
                  thenStmt  = BlockStmt(statements = [ <stmt>* ]),
                  elseStmt? = BlockStmt(statements = [ <stmt>* ])
                );
                """;


        var lexer = new ebnfLexer(CharStreams.fromString(input));
        var parser = new ebnfParser(new CommonTokenStream(lexer));
        var ruleCtx = parser.rule_();
        var builder = new RuleAstBuilder();
        RuleAst ast = builder.visit(ruleCtx);


        assertNotNull(ast);

        assertTrue(ast instanceof RuleDef);
        RuleDef rule = (RuleDef) ast;

        assertEquals("ifStmt", rule.name());
        assertNotNull(rule.body());

        assertTrue(rule.body() instanceof NodePat);
        NodePat ifPat = (NodePat) rule.body();

        assertEquals("IfStmt", ifPat.typeName());
        assertNotNull(ifPat.fields());
        assertEquals(3, ifPat.fields().size());

        var condition = ifPat.fields().get(0);
        assertEquals("condition", condition.name());
        assertFalse(condition.optional());

        assertTrue(condition.value() instanceof RuleRef);
        RuleRef exprRef = (RuleRef) condition.value();
        assertEquals("expr", exprRef.name());

        var thenStmt = ifPat.fields().get(1);
        assertEquals("thenStmt", thenStmt.name());
        assertFalse(thenStmt.optional());

        assertTrue(thenStmt.value() instanceof NodePat);
        NodePat thenBlock = (NodePat) thenStmt.value();
        assertEquals("BlockStmt", thenBlock.typeName());

        var elseStmt = ifPat.fields().get(2);
        assertEquals("elseStmt", elseStmt.name());
        assertTrue(elseStmt.optional());

        assertTrue(elseStmt.value() instanceof NodePat);
        NodePat elseBlock = (NodePat) elseStmt.value();
        assertEquals("BlockStmt", elseBlock.typeName());
    }
}
