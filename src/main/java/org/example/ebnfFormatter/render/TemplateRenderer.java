package org.example.ebnfFormatter.render;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.example.ebnfFormatter.match.Bindings;
import org.example.ebnfFormatter.model.format.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class TemplateRenderer {

    public String render(FormatAst format, Bindings bindings) {
        RenderContext context = new RenderContext();
        renderInto(format, bindings, context);
        return context.result();
    }

    private void renderInto(FormatAst format, Bindings bindings, RenderContext context) {
        switch (format) {
            case FormatText text -> context.appendText(text.text());

            case FormatPlaceholder placeholder -> {
                Object value = bindings.getRequired(placeholder.name());
                renderValue(value, context);
            }

            case FormatDirective directive -> renderDirective(directive, context);

            case FormatSeq seq -> {
                for (FormatAst item : seq.items()) {
                    renderInto(item, bindings, context);
                }
            }

            case FormatGroup group -> renderInto(group.body(), bindings, context);

            case FormatIfPresent ifPresent -> {
                Object value = bindings.find(ifPresent.name());
                if (isPresent(value)) {
                    renderInto(ifPresent.body(), bindings, context);
                }
            }

            case FormatJoin join -> renderJoin(join, bindings, context);
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

    private void renderJoin(FormatJoin join, Bindings bindings, RenderContext context) {
        Object raw = bindings.find(join.placeholderName());
        List<?> items = toList(raw);

        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                renderInto(join.separator(), bindings, context);
            }
            renderValue(items.get(i), context);
        }
    }

    private void renderValue(Object value, RenderContext context) {
        if (value == null) {
            return;
        }

        if (value instanceof Optional<?> optional) {
            optional.ifPresent(v -> renderValue(v, context));
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
                renderValue(item, context);
            }
            return;
        }

        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                renderValue(Array.get(value, i), context);
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

        context.appendText(node.toString());
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