package org.example;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.example.HardCodeIfVisitor.IfVisitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class IfVisitorHardCodedTest {

    @Test
    void testIfVisitorFormatting() {
        String input = "if(d==true){a=5;}";
        String expected = "if (d==true) \n{a=5;}";

        OldEbnfLexer lexer = new OldEbnfLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OldEbnfParser parser = new OldEbnfParser(tokens);

        OldEbnfParser.RulelistContext tree = parser.rulelist();
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
        new IfVisitor(rewriter).visit(tree);



        String output = rewriter.getText();
        assertEquals(expected, output);
    }
}