package org.example.rules;

import org.example.ebnfBaseVisitor;
import org.example.ebnfParser;
import org.example.rules.ast.*;

import java.util.ArrayList;
import java.util.List;

public class RuleAstBuilder extends ebnfBaseVisitor<RuleAst> {

    @Override
    public RuleAst visitRule_(ebnfParser.Rule_Context ctx) {
        String name = ctx.ruleName().IDENT().getText();
        RuleAst body = visit(ctx.pattern());
        return new RuleDef(name, body);
    }

    @Override
    public RuleAst visitPattern(ebnfParser.PatternContext ctx) {
        if (ctx.alternative().size() == 1)
            return visit(ctx.alternative(0));
        List<RuleAst> opts = new ArrayList<>();
        for (var a : ctx.alternative())
            opts.add(visit(a));
        return new Alt(opts);
    }

    @Override
    public RuleAst visitAlternative(ebnfParser.AlternativeContext ctx) {
        if (ctx.sequence() == null)
            return new Seq(List.of());
        return visit(ctx.sequence());
    }

    @Override
    public RuleAst visitSequence(ebnfParser.SequenceContext ctx) {
        List<RuleAst> items = new ArrayList<>();
        for (var atom : ctx.atom())
            items.add(visit(atom));
        if (items.size() == 1)
            return items.get(0);
        return new Seq(items);
    }

    @Override
    public RuleAst visitAtom(ebnfParser.AtomContext ctx) {
        RuleAst base = visit(ctx.primary());
        if (ctx.quantifier() == null)
            return base;

        String q = ctx.quantifier().getText();
        return switch (q) {
            case "?" -> new Quant(Q.OPTIONAL, base);
            case "*" -> new Quant(Q.ZERO_OR_MORE, base);
            case "+" -> new Quant(Q.ONE_OR_MORE, base);
            default -> throw new IllegalStateException("Unknown quantifier: " + q);
        };
    }

    @Override
    public RuleAst visitRuleRef(ebnfParser.RuleRefContext ctx) {
        return new RuleRef(ctx.IDENT().getText());
    }

    @Override
    public RuleAst visitListPattern(ebnfParser.ListPatternContext ctx) {
        List<RuleAst> items = new ArrayList<>();
        if (ctx.pattern() != null) {
            for (var p : ctx.pattern())
                items.add(visit(p));
        }
        return new ListPat(items);
    }

    @Override
    public RuleAst visitNodePattern(ebnfParser.NodePatternContext ctx) {
        String typeName = ctx.typeName().getText();
        List<FieldPat> fields = new ArrayList<>();

        if (ctx.fieldAssignments() != null) {
            for (var fa : ctx.fieldAssignments().fieldAssignment()) {
                String fieldName = fa.fieldName().getText();
                boolean optional = (fa.QMARK() != null);
                RuleAst value = visit(fa.pattern());
                fields.add(new FieldPat(fieldName, optional, value));
            }
        }
        return new NodePat(typeName, fields);
    }

    @Override
    public RuleAst visitGroup(ebnfParser.GroupContext ctx) {
        return visit(ctx.pattern());
    }

    @Override
    public RuleAst visitLiteral(ebnfParser.LiteralContext ctx) {
        if (ctx.STRING() != null)
            return new Lit(unquote(ctx.STRING().getText()));
        if (ctx.NUMBER() != null)
            return new Lit(ctx.NUMBER().getText());
        if (ctx.TRUE() != null)
            return new Lit(true);
        if (ctx.FALSE() != null)
            return new Lit(false);
        return new Lit(null);
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1);
        return s;
    }
}
