package org.example.ebnfFormatter.dsl;

import org.example.ebnfFormatter.model.RuleDef;
import org.example.ebnfFormatter.model.format.DirectiveKind;
import org.example.ebnfFormatter.model.format.FormatAst;
import org.example.ebnfFormatter.model.format.FormatDirective;
import org.example.ebnfFormatter.model.format.FormatGroup;
import org.example.ebnfFormatter.model.format.FormatIfPresent;
import org.example.ebnfFormatter.model.format.FormatJoin;
import org.example.ebnfFormatter.model.format.FormatPlaceholder;
import org.example.ebnfFormatter.model.format.FormatSeq;
import org.example.ebnfFormatter.model.format.FormatText;
import org.example.ebnfFormatter.model.pattern.Alt;
import org.example.ebnfFormatter.model.pattern.FieldPat;
import org.example.ebnfFormatter.model.pattern.Lit;
import org.example.ebnfFormatter.model.pattern.ListPat;
import org.example.ebnfFormatter.model.pattern.NodePat;
import org.example.ebnfFormatter.model.pattern.PatternAst;
import org.example.ebnfFormatter.model.pattern.Quant;
import org.example.ebnfFormatter.model.pattern.QuantifierKind;
import org.example.ebnfFormatter.model.pattern.RuleRef;
import org.example.ebnfFormatter.model.pattern.Seq;
import org.example.ebnfParser;

import java.util.ArrayList;
import java.util.List;

public final class RuleAstBuilder {

    public List<RuleDef> buildRules(ebnfParser.RulelistContext ctx) {
        List<RuleDef> rules = new ArrayList<>();
        for (ebnfParser.Rule_Context ruleCtx : ctx.rule_()) {
            rules.add(buildRule(ruleCtx));
        }
        return rules;
    }

    public RuleDef buildRule(ebnfParser.Rule_Context ctx) {
        String name = parseRefName(ctx.ruleName().refName());
        PatternAst pattern = buildPattern(ctx.pattern());
        FormatAst format = buildFormatExpr(ctx.formatExpr());
        return new RuleDef(name, pattern, format);
    }

    public PatternAst buildPattern(ebnfParser.PatternContext ctx) {
        if (ctx.alternative().size() == 1) {
            return buildAlternative(ctx.alternative(0));
        }

        List<PatternAst> alternatives = new ArrayList<>();
        for (ebnfParser.AlternativeContext altCtx : ctx.alternative()) {
            alternatives.add(buildAlternative(altCtx));
        }
        return new Alt(alternatives);
    }

    private PatternAst buildAlternative(ebnfParser.AlternativeContext ctx) {
        if (ctx.sequence() == null) {
            return new Seq(List.of());
        }
        return buildSequence(ctx.sequence());
    }

    private PatternAst buildSequence(ebnfParser.SequenceContext ctx) {
        List<PatternAst> items = new ArrayList<>();
        for (ebnfParser.AtomContext atomCtx : ctx.atom()) {
            items.add(buildAtom(atomCtx));
        }

        if (items.size() == 1) {
            return items.getFirst();
        }
        return new Seq(items);
    }

    private PatternAst buildAtom(ebnfParser.AtomContext ctx) {
        PatternAst base = buildPrimary(ctx.primary());
        if (ctx.quantifier() == null) {
            return base;
        }
        return new Quant(base, parseQuantifier(ctx.quantifier()));
    }

    private PatternAst buildPrimary(ebnfParser.PrimaryContext ctx) {
        if (ctx.nodePattern() != null) {
            return buildNodePattern(ctx.nodePattern());
        }
        if (ctx.ruleRef() != null) {
            return buildRuleRef(ctx.ruleRef());
        }
        if (ctx.listPattern() != null) {
            return buildListPattern(ctx.listPattern());
        }
        if (ctx.group() != null) {
            return buildGroup(ctx.group());
        }
        if (ctx.literal() != null) {
            return buildLiteral(ctx.literal());
        }

        throw new IllegalStateException("Unsupported primary: " + ctx.getText());
    }

    private PatternAst buildRuleRef(ebnfParser.RuleRefContext ctx) {
        return new RuleRef(parseRefName(ctx.refName()));
    }

    private PatternAst buildListPattern(ebnfParser.ListPatternContext ctx) {
        List<PatternAst> items = new ArrayList<>();
        for (ebnfParser.PatternContext patternCtx : ctx.pattern()) {
            items.add(buildPattern(patternCtx));
        }
        return new ListPat(items);
    }

    private PatternAst buildNodePattern(ebnfParser.NodePatternContext ctx) {
        String typeName = ctx.typeName().getText();
        List<FieldPat> fields = new ArrayList<>();

        if (ctx.fieldAssignments() != null) {
            for (ebnfParser.FieldAssignmentContext fa : ctx.fieldAssignments().fieldAssignment()) {
                String fieldName = fa.fieldName().getText();
                boolean optional = fa.QMARK() != null;
                PatternAst value = buildPattern(fa.pattern());
                fields.add(new FieldPat(fieldName, optional, value));
            }
        }

        return new NodePat(typeName, fields);
    }

    private PatternAst buildGroup(ebnfParser.GroupContext ctx) {
        return buildPattern(ctx.pattern());
    }

    private PatternAst buildLiteral(ebnfParser.LiteralContext ctx) {
        if (ctx.STRING() != null) {
            return new Lit(unquote(ctx.STRING().getText()));
        }
        if (ctx.NUMBER() != null) {
            return new Lit(parseNumber(ctx.NUMBER().getText()));
        }
        if (ctx.TRUE() != null) {
            return new Lit(true);
        }
        if (ctx.FALSE() != null) {
            return new Lit(false);
        }
        if (ctx.NULL() != null) {
            return new Lit(null);
        }
        if (ctx.IDENT() != null) {
            return new Lit(ctx.IDENT().getText());
        }

        throw new IllegalStateException("Unsupported literal: " + ctx.getText());
    }

    public FormatAst buildFormatExpr(ebnfParser.FormatExprContext ctx) {
        List<FormatAst> items = new ArrayList<>();
        for (ebnfParser.FormatAtomContext atomCtx : ctx.formatAtom()) {
            items.add(buildFormatAtom(atomCtx));
        }

        if (items.size() == 1) {
            return items.get(0);
        }
        return new FormatSeq(items);
    }

    private FormatAst buildFormatAtom(ebnfParser.FormatAtomContext ctx) {
        if (ctx.textLiteral() != null) {
            return buildTextLiteral(ctx.textLiteral());
        }
        if (ctx.placeholder() != null) {
            return buildPlaceholder(ctx.placeholder());
        }
        if (ctx.formatDirective() != null) {
            return buildFormatDirective(ctx.formatDirective());
        }
        if (ctx.formatGroup() != null) {
            return buildFormatGroup(ctx.formatGroup());
        }
        if (ctx.conditionalFormat() != null) {
            return buildConditionalFormat(ctx.conditionalFormat());
        }
        if (ctx.joinFormat() != null) {
            return buildJoinFormat(ctx.joinFormat());
        }

        throw new IllegalStateException("Unsupported format atom: " + ctx.getText());
    }

    private FormatAst buildTextLiteral(ebnfParser.TextLiteralContext ctx) {
        return new FormatText(unquote(ctx.STRING().getText()));
    }

    private FormatAst buildPlaceholder(ebnfParser.PlaceholderContext ctx) {
        return new FormatPlaceholder(parseRefName(ctx.refName()));
    }

    private FormatAst buildFormatDirective(ebnfParser.FormatDirectiveContext ctx) {
        if (ctx.SP() != null) {
            return new FormatDirective(DirectiveKind.SP);
        }
        if (ctx.NL() != null) {
            return new FormatDirective(DirectiveKind.NL);
        }
        if (ctx.INDENT() != null) {
            return new FormatDirective(DirectiveKind.INDENT);
        }
        if (ctx.DEDENT() != null) {
            return new FormatDirective(DirectiveKind.DEDENT);
        }

        throw new IllegalStateException("Unknown format directive: " + ctx.getText());
    }

    private FormatAst buildFormatGroup(ebnfParser.FormatGroupContext ctx) {
        return new FormatGroup(buildFormatExpr(ctx.formatExpr()));
    }

    private FormatAst buildConditionalFormat(ebnfParser.ConditionalFormatContext ctx) {
        String name = parseRefName(ctx.refName());
        FormatAst body = buildFormatExpr(ctx.formatExpr());
        return new FormatIfPresent(name, body);
    }

    private FormatAst buildJoinFormat(ebnfParser.JoinFormatContext ctx) {
        String placeholderName = parseRefName(ctx.placeholder().refName());
        FormatAst separator = buildSeparatorExpr(ctx.separatorExpr());
        return new FormatJoin(placeholderName, separator);
    }

    private FormatAst buildSeparatorExpr(ebnfParser.SeparatorExprContext ctx) {
        List<FormatAst> items = new ArrayList<>();
        for (ebnfParser.SeparatorAtomContext atomCtx : ctx.separatorAtom()) {
            items.add(buildSeparatorAtom(atomCtx));
        }

        if (items.size() == 1) {
            return items.getFirst();
        }
        return new FormatSeq(items);
    }

    private FormatAst buildSeparatorAtom(ebnfParser.SeparatorAtomContext ctx) {
        if (ctx.textLiteral() != null) {
            return buildTextLiteral(ctx.textLiteral());
        }
        if (ctx.formatDirective() != null) {
            return buildFormatDirective(ctx.formatDirective());
        }
        if (ctx.formatGroup() != null) {
            return buildFormatGroup(ctx.formatGroup());
        }

        throw new IllegalStateException("Unsupported separator atom: " + ctx.getText());
    }

    private QuantifierKind parseQuantifier(ebnfParser.QuantifierContext ctx) {
        return switch (ctx.getText()) {
            case "?" -> QuantifierKind.OPTIONAL;
            case "*" -> QuantifierKind.ZERO_OR_MORE;
            case "+" -> QuantifierKind.ONE_OR_MORE;
            default -> throw new IllegalStateException("Unknown quantifier: " + ctx.getText());
        };
    }

    private String parseRefName(ebnfParser.RefNameContext ctx) {
        return ctx.getText();
    }

    private Object parseNumber(String text) {
        return text.contains(".") ? Double.parseDouble(text) : Long.parseLong(text);
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}