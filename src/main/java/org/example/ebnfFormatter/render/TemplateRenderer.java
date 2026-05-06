package org.example.ebnfFormatter.render;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.metamodel.PropertyMetaModel;
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
            renderNode(node, nestedRuleRenderer, context);
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

    private void renderNode(Node node, NestedRuleRenderer nestedRuleRenderer, RenderContext context) {
        Optional<String> renderedByJavaParserType = nestedRuleRenderer.tryRender(
                node.getClass().getSimpleName(),
                node
        );
        if (renderedByJavaParserType.isPresent()) {
            context.appendText(renderedByJavaParserType.get());
            return;
        }

        context.appendText(node.toString());
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
