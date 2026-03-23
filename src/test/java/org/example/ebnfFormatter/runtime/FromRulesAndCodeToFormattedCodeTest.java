package org.example.ebnfFormatter.runtime;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.stmt.ForStmt;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.example.ebnfFormatter.dsl.RuleAstBuilder;
import org.example.ebnfFormatter.match.PatternMatcher;
import org.example.ebnfFormatter.model.RuleDef;
import org.example.ebnfFormatter.render.TemplateRenderer;
import org.example.ebnfLexer;
import org.example.ebnfParser;
import org.junit.jupiter.api.Test;

import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;

public class FromRulesAndCodeToFormattedCodeTest {
    @Test
    void endToEndForStmt() {
        String rules = """
            <ForStmt> ::= ForStmt
                => "for";
            """;

        String code = """
            public class AST {
                void m() {
                    for (;;) make();
                }
            }
            """;

        List<RuleDef> parsed = parseRules(rules);

        RuleRegistry ruleRegistry = new RuleRegistry();
        ruleRegistry.registerAll(parsed);

        TypeRegistryUniversal typeRegistry = new TypeRegistryUniversal();
        PatternMatcher patternMatcher = new PatternMatcher(typeRegistry);
        TemplateRenderer templateRenderer = new TemplateRenderer();
        FormatterEngine engine = new FormatterEngine(ruleRegistry, patternMatcher, templateRenderer);

        ForStmt node = StaticJavaParser.parse(code)
                .findFirst(ForStmt.class)
                .orElseThrow(() -> new AssertionError("ForStmt not found"));

        String formatted = engine.format(node, "ForStmt");

        assertThat(formatted).isEqualTo("for");
    }

    private List<RuleDef> parseRules(String text) {
        ebnfLexer lexer = new ebnfLexer(CharStreams.fromString(text));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ebnfParser parser = new ebnfParser(tokens);

        ebnfParser.RulelistContext ctx = parser.rulelist();

        RuleAstBuilder builder = new RuleAstBuilder();
        return builder.buildRules(ctx);
    }


}
