package org.example.ebnfFormatter.runtime;

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

import java.util.List;

public final class TestFormatterFactory {
    public static FormatterEngine createEngine() {
        RuleRegistry ruleRegistry = new RuleRegistry();
        ruleRegistry.registerAll(rulesFromDocumentation());

        TypeRegistry typeRegistry = new TypeRegistry();
        PatternMatcher patternMatcher = new PatternMatcher(typeRegistry);
        TemplateRenderer templateRenderer = new TemplateRenderer();

        return new FormatterEngine(ruleRegistry, patternMatcher, templateRenderer);
    }

    private static List<RuleDef> rulesFromDocumentation() {
        return List.of(
                ifStmtRule(),
                methodDeclarationRule(),
                forStmtRule(),
                blockStmtRule()
        );
    }

    private static RuleDef ifStmtRule() {
        return new RuleDef(
                "IfStmt",
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
        );
    }

    private static RuleDef methodDeclarationRule() {
        return new RuleDef(
                "MethodDeclaration",
                new NodePat(
                        "MethodDeclaration",
                        List.of(
                                new FieldPat("modifiers", true, new RuleRef("modifiers")),
                                new FieldPat("type", false, new RuleRef("type")),
                                new FieldPat("name", false, new RuleRef("name")),
                                new FieldPat("parameters", true, new RuleRef("parameters")),
                                new FieldPat("body", true, new RuleRef("body"))
                        )
                ),
                seq(
                        new FormatIfPresent(
                                "modifiers",
                                seq(
                                        new FormatJoin("modifiers", text(""))
                                )
                        ),
                        placeholder("type"),
                        sp(),
                        placeholder("name"),
                        text("("),
                        new FormatIfPresent(
                                "parameters",
                                new FormatJoin("parameters", text(", "))
                        ),
                        text(")"),
                        new FormatIfPresent(
                                "body",
                                seq(
                                        sp(),
                                        placeholder("body")
                                )
                        )
                )
        );
    }

    private static RuleDef forStmtRule() {
        return new RuleDef(
                "ForStmt",
                new NodePat(
                        "ForStmt",
                        List.of(
                                new FieldPat("initialization", true, new RuleRef("initialization")),
                                new FieldPat("compare", true, new RuleRef("compare")),
                                new FieldPat("update", true, new RuleRef("update")),
                                new FieldPat("body", false, new RuleRef("body"))
                        )
                ),
                seq(
                        text("for"),
                        sp(),
                        text("("),
                        new FormatIfPresent(
                                "initialization",
                                new FormatJoin("initialization", text(", "))
                        ),
                        text(";"),
                        sp(),
                        new FormatIfPresent("compare", placeholder("compare")),
                        text(";"),
                        sp(),
                        new FormatIfPresent(
                                "update",
                                new FormatJoin("update", text(", "))
                        ),
                        text(")"),
                        nl(),
                        indent(),
                        placeholder("body"),
                        dedent()
                )
        );
    }

    private static RuleDef blockStmtRule() {
        return new RuleDef(
                "BlockStmt",
                new NodePat(
                        "BlockStmt",
                        List.of(
                                new FieldPat("statements", true, new RuleRef("statements"))
                        )
                ),
                seq(
                        text("{"),
                        nl(),
                        indent(),
                        new FormatIfPresent(
                                "statements",
                                new FormatJoin("statements", nl())
                        ),
                        dedent(),
                        nl(),
                        text("}")
                )
        );
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