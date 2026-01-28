package org.example;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.example.rules.RuleAstBuilder;
import org.example.rules.ast.RuleAst;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EbnfRuleToAstTest {

    @Test
    void rule_to_ast_dump() {
        String input = """
                <ifStmt> ::= IfStmt(
                  condition = <expr>,
                  thenStmt  = BlockStmt(statements = [ <stmt>* ]),
                  elseStmt? = BlockStmt(statements = [ <stmt>* ])
                );
                """;

        ParseTree tree = parseSingleRule(input);

        RuleAst ast = new RuleAstBuilder().visit(tree);
        String actual = normalize(ast.dump());


        String expected = normalize("""
                (rule <ifStmt>
                  (node IfStmt
                    (field condition
                      (ref <expr>)
                    )
                    (field thenStmt
                      (node BlockStmt
                        (field statements
                          (list
                            (quant ZERO_OR_MORE
                              (ref <stmt>)
                            )
                          )
                        )
                      )
                    )
                    (field elseStmt?
                      (node BlockStmt
                        (field statements
                          (list
                            (quant ZERO_OR_MORE
                              (ref <stmt>)
                            )
                          )
                        )
                      )
                    )
                  )
                )
                """);

        assertEquals(expected, actual);
    }

    private static ParseTree parseSingleRule(String input) {
        ebnfLexer lexer = new ebnfLexer(CharStreams.fromString(input));
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);

        ebnfParser parser = new ebnfParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);

        return parser.rule_();
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").trim();
    }

    private static final class ThrowingErrorListener extends BaseErrorListener {
        static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer, Object offendingSymbol,
                int line, int charPositionInLine,
                String msg, RecognitionException e
        ) {
            throw new RuntimeException("Syntax error at " + line + ":" + charPositionInLine + " - " + msg, e);
        }
    }
}
