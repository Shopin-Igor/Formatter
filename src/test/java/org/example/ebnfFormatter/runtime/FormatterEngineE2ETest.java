package org.example.ebnfFormatter.runtime;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import org.example.ebnfFormatter.match.PatternMatcher;
import org.example.ebnfFormatter.model.RuleDef;
import org.example.ebnfFormatter.model.format.DirectiveKind;
import org.example.ebnfFormatter.model.format.FormatAst;
import org.example.ebnfFormatter.model.format.FormatDirective;
import org.example.ebnfFormatter.model.format.FormatIfPresent;
import org.example.ebnfFormatter.model.format.FormatJoin;
import org.example.ebnfFormatter.model.format.FormatPlaceholder;
import org.example.ebnfFormatter.model.format.FormatSeq;
import org.example.ebnfFormatter.model.format.FormatText;
import org.example.ebnfFormatter.model.pattern.FieldPat;
import org.example.ebnfFormatter.model.pattern.NodePat;
import org.example.ebnfFormatter.model.pattern.RuleRef;
import org.example.ebnfFormatter.render.TemplateRenderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FormatterEngineE2ETest {

    @Test
    void formats_if_without_else() {
        FormatterEngine engine = engineWithRules(
                new RuleDef(
                        "ifRule",
                        new NodePat(
                                "IfStmt",
                                List.of(
                                        new FieldPat("condition", false, new RuleRef("condition")),
                                        new FieldPat("thenStmt", false, new RuleRef("thenStmt")),
                                        new FieldPat("elseStmt", true, new RuleRef("elseStmt"))
                                )
                        ),
                        seq(
                                text("if"),
                                sp(),
                                text("("),
                                placeholder("condition"),
                                text(")"),
                                sp(),
                                indent(),
                                placeholder("thenStmt"),
                                dedent(),
                                new FormatIfPresent(
                                        "elseStmt",
                                        seq(
                                                sp(),
                                                text("else"),
                                                sp(),
                                                nl(),
                                                indent(),
                                                placeholder("elseStmt"),
                                                dedent()
                                        )
                                )
                        )
                )
        );

        Node node = StaticJavaParser.parseStatement("if(a>b)return a;");

        String actual = engine.format(node, "ifRule");

        assertThat(actual).isEqualTo("if (a > b) return a;");
    }

    @Test
    void formats_if_with_else() {
        FormatterEngine engine = engineWithRules(
                new RuleDef(
                        "ifRule",
                        new NodePat(
                                "IfStmt",
                                List.of(
                                        new FieldPat("condition", false, new RuleRef("condition")),
                                        new FieldPat("thenStmt", false, new RuleRef("thenStmt")),
                                        new FieldPat("elseStmt", true, new RuleRef("elseStmt"))
                                )
                        ),
                        seq(
                                text("if"),
                                sp(),
                                text("("),
                                placeholder("condition"),
                                text(")"),
                                nl(),
                                indent(),
                                placeholder("thenStmt"),
                                dedent(),
                                new FormatIfPresent(
                                        "elseStmt",
                                        seq(
                                                nl(),
                                                text("else"),
                                                nl(),
                                                indent(),
                                                placeholder("elseStmt"),
                                                dedent()
                                        )
                                )
                        )
                )
        );

        Node node = StaticJavaParser.parseStatement("if (a > b) return a; else return b;");

        String actual = engine.format(node, "ifRule");

        assertThat(actual).isEqualTo("""
                if (a > b) 
                    return a; 
                else
                    return b;
                """.trim());
    }

    @Test
    void formats_block_with_indentation_and_join() {
        FormatterEngine engine = engineWithRules(
                new RuleDef(
                        "blockRule",
                        new NodePat(
                                "BlockStmt",
                                List.of(
                                        new FieldPat("statements", false, new RuleRef("stmts"))
                                )
                        ),
                        seq(
                                text("{"),
                                nl(),
                                indent(),
                                new FormatJoin("stmts", nl()),
                                nl(),
                                dedent(),
                                text("}")
                        )
                )
        );

        Node node = StaticJavaParser.parseStatement("""
                {
                    int x = 1;
                    return x;
                }
                """);

        String actual = engine.format(node, "blockRule");

        assertThat(actual).isEqualTo("""
                {
                    int x = 1;
                    return x;
                }""");
    }

    @Test
    void formats_for_stmt_with_compare() {
        FormatterEngine engine = engineWithRules(
                new RuleDef(
                        "forRule",
                        new NodePat(
                                "ForStmt",
                                List.of(
                                        new FieldPat("initialization", false, new RuleRef("init")),
                                        new FieldPat("compare", true, new RuleRef("compare")),
                                        new FieldPat("update", false, new RuleRef("update")),
                                        new FieldPat("body", false, new RuleRef("body"))
                                )
                        ),
                        seq(
                                text("for"),
                                sp(),
                                text("("),
                                new FormatJoin("init", text(", ")),
                                text(";"),
                                new FormatIfPresent(
                                        "compare",
                                        seq(
                                                sp(),
                                                placeholder("compare")
                                        )
                                ),
                                text(";"),
                                new FormatIfPresent(
                                        "update",
                                        seq(
                                                sp(),
                                                new FormatJoin("update", text(", "))
                                        )
                                ),
                                text(")"),
                                sp(),
                                placeholder("body")
                        )
                )
        );

        Node node = StaticJavaParser.parseStatement(
                "for (i = 0, j = 1; i < 10; i++, j++) x++;"
        );

        String actual = engine.format(node, "forRule");

        assertThat(actual).isEqualTo("for (i = 0, j = 1; i < 10; i++, j++) x++;");
    }

    @Test
    void formats_for_stmt_without_compare() {
        FormatterEngine engine = engineWithRules(
                new RuleDef(
                        "forRule",
                        new NodePat(
                                "ForStmt",
                                List.of(
                                        new FieldPat("initialization", false, new RuleRef("init")),
                                        new FieldPat("compare", true, new RuleRef("compare")),
                                        new FieldPat("update", false, new RuleRef("update")),
                                        new FieldPat("body", false, new RuleRef("body"))
                                )
                        ),
                        seq(
                                text("for"),
                                sp(),
                                text("("),
                                new FormatJoin("init", text(", ")),
                                text(";"),
                                new FormatIfPresent(
                                        "compare",
                                        seq(
                                                sp(),
                                                placeholder("compare")
                                        )
                                ),
                                text(";"),
                                new FormatIfPresent(
                                        "update",
                                        seq(
                                                sp(),
                                                new FormatJoin("update", text(", "))
                                        )
                                ),
                                text(")"),
                                sp(),
                                placeholder("body")
                        )
                )
        );

        Node node = StaticJavaParser.parseStatement(
                "for (i = 0, j = 1; ; i++, j++) x++;"
        );

        String actual = engine.format(node, "forRule");

        assertThat(actual).isEqualTo("for (i = 0, j = 1;; i++, j++) x++;");
    }

    private static FormatterEngine engineWithRules(RuleDef... rules) {
        RuleRegistry ruleRegistry = new RuleRegistry();
        ruleRegistry.registerAll(List.of(rules));

        TypeRegistry typeRegistry = new TypeRegistry();
        PatternMatcher patternMatcher = new PatternMatcher(typeRegistry);
        TemplateRenderer templateRenderer = new TemplateRenderer();

        return new FormatterEngine(ruleRegistry, patternMatcher, templateRenderer);
    }

    private static FormatSeq seq(FormatAst... items) {
        return new FormatSeq(List.of(items));
    }

    private static FormatText text(String value) {
        return new FormatText(value);
    }

    private static FormatPlaceholder placeholder(String name) {
        return new FormatPlaceholder(name);
    }

    private static FormatDirective sp() {
        return new FormatDirective(DirectiveKind.SP);
    }

    private static FormatDirective nl() {
        return new FormatDirective(DirectiveKind.NL);
    }

    private static FormatDirective indent() {
        return new FormatDirective(DirectiveKind.INDENT);
    }

    private static FormatDirective dedent() {
        return new FormatDirective(DirectiveKind.DEDENT);
    }
}