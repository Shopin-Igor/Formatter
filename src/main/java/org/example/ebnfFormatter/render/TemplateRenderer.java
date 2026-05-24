package org.example.ebnfFormatter.render;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.Stringable;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
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
        boolean hasEmptySeparator = isEmptyFormat(join.separator());

        for (int i = 0; i < items.size(); i++) {
            BoundValue item = items.get(i);
            if (i > 0) {
                if (hasEmptySeparator) {
                    appendOriginalGapBetween(items.get(i - 1), item, context);
                } else {
                    renderInto(join.separator(), bindings, nestedRuleRenderer, context);
                }
            }
            renderBoundValue(join.placeholderName(), item, nestedRuleRenderer, context);
        }

        if (hasEmptySeparator && !items.isEmpty()) {
            appendOriginalGapAfter(items.getLast(), context);
        }
    }

    private boolean isEmptyFormat(FormatAst format) {
        return switch (format) {
            case FormatText text -> text.text().isEmpty();
            case FormatSeq seq -> seq.items().stream().allMatch(this::isEmptyFormat);
            case FormatGroup group -> isEmptyFormat(group.body());
            default -> false;
        };
    }

    private void appendOriginalGapBetween(BoundValue left, BoundValue right, RenderContext context) {
        originalGapBetween(left.legacyValue(), right.legacyValue()).ifPresent(context::appendText);
    }

    private void appendOriginalGapAfter(BoundValue value, RenderContext context) {
        originalGapAfter(value.legacyValue()).ifPresent(context::appendText);
    }

    private Optional<String> originalGapBetween(Object left, Object right) {
        Optional<TokenRange> leftRange = tokenRange(left);
        Optional<TokenRange> rightRange = tokenRange(right);
        if (leftRange.isEmpty() || rightRange.isEmpty()) {
            return Optional.empty();
        }

        JavaToken end = rightRange.get().getBegin();
        Optional<JavaToken> current = leftRange.get().getEnd().getNextToken();
        StringBuilder text = new StringBuilder();

        while (current.isPresent() && current.get() != end) {
            JavaToken token = current.get();
            if (!token.getCategory().isWhitespaceOrComment()) {
                return Optional.empty();
            }
            text.append(token.getText());
            current = token.getNextToken();
        }

        return current.isPresent() ? Optional.of(text.toString()) : Optional.empty();
    }

    private Optional<String> originalGapAfter(Object value) {
        Optional<TokenRange> range = tokenRange(value);
        if (range.isEmpty()) {
            return Optional.empty();
        }

        Optional<JavaToken> current = range.get().getEnd().getNextToken();
        StringBuilder text = new StringBuilder();

        while (current.isPresent() && current.get().getCategory().isWhitespaceOrComment()) {
            JavaToken token = current.get();
            text.append(token.getText());
            current = token.getNextToken();
        }

        return Optional.of(text.toString());
    }

    private Optional<TokenRange> tokenRange(Object value) {
        if (value instanceof Node node) {
            return node.getTokenRange();
        }

        if (value instanceof Optional<?> optional) {
            return optional.flatMap(this::tokenRange);
        }

        return Optional.empty();
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

        context.appendText(valueToSource(value));
    }

    private String valueToSource(Object value) {
//        if (value instanceof BinaryExpr.Operator operator) {
//            return operator.asString();
//        }
//
//        if (value instanceof AssignExpr.Operator operator) {
//            return operator.asString();
//        }
//
//        if (value instanceof UnaryExpr.Operator operator) {
//            return operator.asString();
//        }
//
//        if (value instanceof PrimitiveType.Primitive primitive) {
//            return primitive.asString();
//        }

        if (value instanceof Stringable stringable) {
            return stringable.asString();
        }


        if (value instanceof Modifier.Keyword keyword) {
            return keyword.asString();
        }
//                DEFAULT("default"),
//                PUBLIC("public"),
//                PROTECTED("protected"),
//                PRIVATE("private"),
//                ABSTRACT("abstract"),
//                STATIC("static"),
//                FINAL("final"),
//                TRANSIENT("transient"),
//                VOLATILE("volatile"),
//                SYNCHRONIZED("synchronized"),
//                NATIVE("native"),
//                STRICTFP("strictfp"),
//                TRANSITIVE("transitive"),
//                SEALED("sealed"),
//                NON_SEALED("non-sealed");

        return String.valueOf(value);
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

        context.appendText(sourceText(node));
    }

    private String sourceText(Node node) {
        if (LexicalPreservingPrinter.isAvailableOn(node)) {  // может быть полезно для маштабирования
            return LexicalPreservingPrinter.print(node);
        }
        return node.getTokenRange()
                .map(TokenRange::toString)
                .orElseGet(() -> LexicalPreservingPrinter.print(node));
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
