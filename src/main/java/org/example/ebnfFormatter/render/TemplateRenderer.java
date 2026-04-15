package org.example.ebnfFormatter.render;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import org.example.ebnfFormatter.match.AppliedRuleValue;
import org.example.ebnfFormatter.match.Bindings;
import org.example.ebnfFormatter.match.BoundValue;
import org.example.ebnfFormatter.match.RawValue;
import org.example.ebnfFormatter.model.format.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class TemplateRenderer {

    public String render(FormatAst format, Bindings bindings) {
        return render(format, bindings, (ruleName, value) -> Optional.empty());
    }

    public String render(FormatAst format, Bindings bindings, NestedRuleRenderer nestedRuleRenderer) {
        RenderContext context = new RenderContext();
        renderInto(format, bindings, nestedRuleRenderer, context);
        return context.result();
    }

    private void renderInto(
            FormatAst format,
            Bindings bindings,
            NestedRuleRenderer nestedRuleRenderer,
            RenderContext context
    ) {
        switch (format) {
            case FormatText text -> context.appendText(text.text());

            case FormatPlaceholder placeholder ->
                    renderPlaceholder(placeholder.name(), bindings, nestedRuleRenderer, context);

            case FormatDirective directive -> renderDirective(directive, context);

            case FormatSeq seq -> {
                for (FormatAst item : seq.items()) {
                    renderInto(item, bindings, nestedRuleRenderer, context);
                }
            }

            case FormatGroup group -> renderInto(group.body(), bindings, nestedRuleRenderer, context);

            case FormatIfPresent ifPresent -> {
                if (!bindings.findValues(ifPresent.name()).isEmpty()) {
                    renderInto(ifPresent.body(), bindings, nestedRuleRenderer, context);
                }
            }

            case FormatJoin join -> renderJoin(join, bindings, nestedRuleRenderer, context);
        }
    }

    private void renderDirective(FormatDirective directive, RenderContext context) {
        switch (directive.kind()) {
            case SP -> context.space();
            case NL -> context.newline();
            case INDENT -> context.indent();
            case DEDENT -> context.dedent();
        }
    }

    private void renderPlaceholder(
            String placeholderName,
            Bindings bindings,
            NestedRuleRenderer nestedRuleRenderer,
            RenderContext context
    ) {
        List<BoundValue> values = bindings.getRequiredValues(placeholderName);
        for (BoundValue value : values) {
            renderBoundValue(placeholderName, value, nestedRuleRenderer, context);
        }
    }

    private void renderJoin(
            FormatJoin join,
            Bindings bindings,
            NestedRuleRenderer nestedRuleRenderer,
            RenderContext context
    ) {
        List<BoundValue> items = bindings.findValues(join.placeholderName());

        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                renderInto(join.separator(), bindings, nestedRuleRenderer, context);
            }
            renderBoundValue(join.placeholderName(), items.get(i), nestedRuleRenderer, context);
        }
    }

    private void renderBoundValue(
            String placeholderName,
            BoundValue boundValue,
            NestedRuleRenderer nestedRuleRenderer,
            RenderContext context
    ) {
        switch (boundValue) {
            case AppliedRuleValue appliedRuleValue ->
                    renderInto(
                            appliedRuleValue.appliedRule().rule().format(),
                            appliedRuleValue.appliedRule().bindings(),
                            nestedRuleRenderer,
                            context
                    );
            case RawValue rawValue -> renderRawValue(placeholderName, rawValue.value(), nestedRuleRenderer, context);
        }
    }

    private void renderRawValue(
            String placeholderName,
            Object value,
            NestedRuleRenderer nestedRuleRenderer,
            RenderContext context
    ) {
        if (value == null) {
            return;
        }

        Optional<String> nested = nestedRuleRenderer.tryRender(stripQuantifierSuffix(placeholderName), value);
        if (nested.isPresent()) {
            context.appendText(nested.get());
            return;
        }

        if (value instanceof Optional<?> optional) {
            optional.ifPresent(v -> renderRawValue(placeholderName, v, nestedRuleRenderer, context));
            return;
        }

        if (value instanceof Node node) {
            renderNode(node, context);
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            Iterator<?> it = iterable.iterator();
            while (it.hasNext()) {
                Object item = it.next();
                renderRawValue(placeholderName, item, nestedRuleRenderer, context);
            }
            return;
        }

        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                renderRawValue(placeholderName, Array.get(value, i), nestedRuleRenderer, context);
            }
            return;
        }

        context.appendText(String.valueOf(value));
    }

    private void renderNode(Node node, RenderContext context) {
        if (node instanceof BlockStmt blockStmt) {
            renderBlockStmt(blockStmt, context);
            return;
        }

        if (node instanceof IfStmt ifStmt) {
            renderIfStmt(ifStmt, context);
            return;
        }

        if (node instanceof ForStmt forStmt) {
            renderForStmt(forStmt, context);
            return;
        }

        if (node instanceof MethodDeclaration methodDeclaration) {
            renderMethodDeclaration(methodDeclaration, context);
            return;
        }

        if (node instanceof ReturnStmt returnStmt) {
            renderReturnStmt(returnStmt, context);
            return;
        }

        if (node instanceof ExpressionStmt expressionStmt) {
            renderExpressionStmt(expressionStmt, context);
            return;
        }


        context.appendText(node.toString());
    }

    private void renderReturnStmt(com.github.javaparser.ast.stmt.ReturnStmt stmt, RenderContext context) {
        context.appendText("return");
        context.space();
        context.appendText(stmt.getExpression().map(Node::toString).orElse(""));
        context.appendText(";");
    }

    private void renderExpressionStmt(com.github.javaparser.ast.stmt.ExpressionStmt stmt, RenderContext context) {
        context.appendText(stmt.getExpression().toString());
        context.appendText(";");
    }

    private void renderMethodDeclaration(MethodDeclaration method, RenderContext context) {
        if (!method.getModifiers().isEmpty()) {
            for (int i = 0; i < method.getModifiers().size(); i++) {
                if (i > 0) {
                    context.space();
                }
                context.appendText(method.getModifiers().get(i).getKeyword().asString());
            }
            context.space();
        }

        context.appendText(method.getType().toString());
        context.space();
        context.appendText(method.getNameAsString());
        context.appendText("(");

        for (int i = 0; i < method.getParameters().size(); i++) {
            if (i > 0) {
                context.appendText(", ");
            }
            context.appendText(method.getParameter(i).toString());
        }

        context.appendText(")");

        if (method.getBody().isPresent()) {
            context.space();
            renderBlockStmt(method.getBody().get(), context);
        } else {
            context.appendText(";");
        }
    }

    private void renderBlockStmt(BlockStmt block, RenderContext context) {
        context.appendText("{");
        context.newline();
        context.indent();

        List<Statement> statements = block.getStatements();
        for (int i = 0; i < statements.size(); i++) {
            if (i > 0) {
                context.newline();
            }
            renderNode(statements.get(i), context);
        }

        context.newline();
        context.dedent();
        context.appendText("}");
    }

    private void renderIfStmt(IfStmt stmt, RenderContext context) {
        renderIfHeaderAndBody(stmt, context);
    }

    private void renderIfHeaderAndBody(IfStmt stmt, RenderContext context) {
        context.appendText("if");
        context.space();
        context.appendText("(");
        context.appendText(stmt.getCondition().toString());
        context.appendText(")");

        renderControlBody(stmt.getThenStmt(), context);

        stmt.getElseStmt().ifPresent(elseStmt -> {
            if (elseStmt instanceof IfStmt nestedIf) {
                context.newline();
                context.appendText("else");
                context.space();
                renderIfHeaderAndBody(nestedIf, context);
            } else {
                context.newline();
                context.appendText("else");
                renderControlBody(elseStmt, context);
            }
        });
    }

    private void renderForStmt(ForStmt stmt, RenderContext context) {
        context.appendText("for");
        context.space();
        context.appendText("(");

        renderExpressionList(stmt.getInitialization(), context);

        context.appendText(";");

        stmt.getCompare().ifPresent(compare -> {
            context.space();
            context.appendText(compare.toString());
        });

        context.appendText(";");

        if (!stmt.getUpdate().isEmpty()) {
            context.space();
            renderExpressionList(stmt.getUpdate(), context);
        }

        context.appendText(")");

        renderControlBody(stmt.getBody(), context);
    }

    private void renderExpressionList(List<? extends Expression> expressions, RenderContext context) {
        for (int i = 0; i < expressions.size(); i++) {
            if (i > 0) {
                context.appendText(", ");
            }
            context.appendText(expressions.get(i).toString());
        }
    }

    private void renderControlBody(Statement body, RenderContext context) {
        if (body instanceof BlockStmt blockStmt) {
            context.space();
            renderBlockStmt(blockStmt, context);
            return;
        }

        context.newline();
        context.indent();
        renderNode(body, context);
        context.dedent();
    }

    private boolean isPresent(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof Optional<?> optional) {
            return optional.isPresent();
        }

        if (value instanceof CharSequence chars) {
            return !chars.isEmpty();
        }

        if (value instanceof Iterable<?> iterable) {
            return iterable.iterator().hasNext();
        }

        Class<?> type = value.getClass();
        if (type.isArray()) {
            return Array.getLength(value) > 0;
        }

        return true;
    }

    private String stripQuantifierSuffix(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        char last = name.charAt(name.length() - 1);
        return switch (last) {
            case '?', '*', '+' -> name.substring(0, name.length() - 1);
            default -> name;
        };
    }

    private List<?> toList(Object value) {
        if (value == null) {
            return List.of();
        }

        if (value instanceof List<?> list) {
            return list;
        }

        if (value instanceof Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            for (Object item : iterable) {
                result.add(item);
            }
            return result;
        }

        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            List<Object> result = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                result.add(Array.get(value, i));
            }
            return result;
        }

        return List.of(value);
    }
}
