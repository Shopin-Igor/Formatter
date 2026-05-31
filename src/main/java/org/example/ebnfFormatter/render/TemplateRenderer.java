package org.example.ebnfFormatter.render;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.Stringable;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.example.ebnfFormatter.match.AppliedRule;
import org.example.ebnfFormatter.match.AppliedRuleValue;
import org.example.ebnfFormatter.match.Bindings;
import org.example.ebnfFormatter.match.BoundValue;
import org.example.ebnfFormatter.match.RawValue;
import org.example.ebnfFormatter.model.format.*;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class TemplateRenderer {

    public String render(FormatAst format, Bindings bindings) {
        return render(format, bindings, (ruleName, value) -> Optional.empty());
    }

    public String render(FormatAst format, Bindings bindings, NestedRuleRenderer nestedRuleRenderer) {
        RenderContext context = new RenderContext();
        SourceRenderState sourceState = new SourceRenderState();
        renderInto(format, bindings, nestedRuleRenderer, context, sourceState);
        return context.result();
    }

    public String render(AppliedRule appliedRule, NestedRuleRenderer nestedRuleRenderer) {
        RenderContext context = new RenderContext();
        SourceRenderState sourceState = new SourceRenderState();
        renderAppliedRule(appliedRule, nestedRuleRenderer, context, sourceState);
        return context.result();
    }

    private void renderAppliedRule(
            AppliedRule appliedRule,
            NestedRuleRenderer nestedRuleRenderer,
            RenderContext context,
            SourceRenderState sourceState
    ) {
        sourceState.enter(appliedRule.sourceValue());
        try {
            renderInto(
                    appliedRule.rule().format(),
                    appliedRule.bindings(),
                    nestedRuleRenderer,
                    context,
                    sourceState
            );
        } finally {
            sourceState.exit(context);
        }
    }

    private void renderInto(
            FormatAst format,
            Bindings bindings,
            NestedRuleRenderer nestedRuleRenderer,
            RenderContext context,
            SourceRenderState sourceState
    ) {
        switch (format) {
            case FormatText text -> renderText(text.text(), context, sourceState);

            case FormatPlaceholder placeholder ->
                    renderPlaceholder(placeholder.name(), bindings, nestedRuleRenderer, context, sourceState);

            case FormatDirective directive -> renderDirective(directive, context);

            case FormatSeq seq -> {
                for (FormatAst item : seq.items()) {
                    renderInto(item, bindings, nestedRuleRenderer, context, sourceState);
                }
            }

            case FormatGroup group -> renderInto(group.body(), bindings, nestedRuleRenderer, context, sourceState);

            case FormatIfPresent ifPresent -> {
                if (!bindings.findValues(ifPresent.name()).isEmpty()) {
                    renderInto(ifPresent.body(), bindings, nestedRuleRenderer, context, sourceState);
                }
            }

            case FormatJoin join -> renderJoin(join, bindings, nestedRuleRenderer, context, sourceState);
        }
    }

    private void renderText(String text, RenderContext context, SourceRenderState sourceState) {
        int index = 0;
        while (index < text.length()) {
            boolean whitespace = Character.isWhitespace(text.charAt(index));
            int start = index;
            while (index < text.length() && Character.isWhitespace(text.charAt(index)) == whitespace) {
                index++;
            }

            String part = text.substring(start, index);
            if (whitespace) {
                context.appendPendingWhitespace(part);
            } else {
                sourceState.beforeLiteral(part, context);
                context.appendText(part);
            }
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
            RenderContext context,
            SourceRenderState sourceState
    ) {
        List<BoundValue> values = bindings.getRequiredValues(placeholderName);
        for (BoundValue value : values) {
            renderBoundValue(placeholderName, value, nestedRuleRenderer, context, sourceState);
        }
    }

    private void renderJoin(
            FormatJoin join,
            Bindings bindings,
            NestedRuleRenderer nestedRuleRenderer,
            RenderContext context,
            SourceRenderState sourceState
    ) {
        List<BoundValue> items = bindings.findValues(join.placeholderName());
        boolean hasEmptySeparator = isEmptyFormat(join.separator());

        for (int i = 0; i < items.size(); i++) {
            BoundValue item = items.get(i);
            if (i > 0) {
                appendJoinSeparator(
                        join,
                        bindings,
                        nestedRuleRenderer,
                        context,
                        sourceState,
                        items.get(i - 1),
                        item,
                        hasEmptySeparator
                );
            }
            renderBoundValue(join.placeholderName(), item, nestedRuleRenderer, context, sourceState);
        }

        if (hasEmptySeparator && !items.isEmpty()) {
            appendEmptyJoinGapAfter(items.getLast(), context);
        }
    }

    private void appendJoinSeparator(
            FormatJoin join,
            Bindings bindings,
            NestedRuleRenderer nestedRuleRenderer,
            RenderContext context,
            SourceRenderState sourceState,
            BoundValue left,
            BoundValue right,
            boolean hasEmptySeparator
    ) {
        if (hasEmptySeparator) {
            appendEmptyJoinGapBetween(left, right, context);
            return;
        }

        renderInto(join.separator(), bindings, nestedRuleRenderer, context, sourceState);
    }

    private void appendEmptyJoinGapBetween(BoundValue left, BoundValue right, RenderContext context) {
        Optional<OriginalGap> originalGap = OriginalGaps.between(left.legacyValue(), right.legacyValue());
        if (originalGap.isPresent() && !originalGap.get().hasComment()) {
            context.appendRawText(originalGap.get().text());
        }
    }

    private void appendEmptyJoinGapAfter(BoundValue item, RenderContext context) {
        Optional<OriginalGap> originalGap = OriginalGaps.after(item.legacyValue());
        if (originalGap.isPresent() && !originalGap.get().hasComment()) {
            context.appendRawText(originalGap.get().text());
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

    private void renderBoundValue(
            String placeholderName,
            BoundValue boundValue,
            NestedRuleRenderer nestedRuleRenderer,
            RenderContext context,
            SourceRenderState sourceState
    ) {
        sourceState.beforeValue(boundValue.legacyValue(), context);

        switch (boundValue) {
            case AppliedRuleValue appliedRuleValue ->
                    renderAppliedRule(appliedRuleValue.appliedRule(), nestedRuleRenderer, context, sourceState);
            case RawValue rawValue -> renderRawValue(placeholderName, rawValue.value(), nestedRuleRenderer, context, sourceState);
        }
    }

    private void renderRawValue(
            String placeholderName,
            Object value,
            NestedRuleRenderer nestedRuleRenderer,
            RenderContext context,
            SourceRenderState sourceState
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
            optional.ifPresent(v -> renderRawValue(placeholderName, v, nestedRuleRenderer, context, sourceState));
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
                sourceState.beforeValue(item, context);
                renderRawValue(placeholderName, item, nestedRuleRenderer, context, sourceState);
            }
            return;
        }

        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object item = Array.get(value, i);
                sourceState.beforeValue(item, context);
                renderRawValue(placeholderName, item, nestedRuleRenderer, context, sourceState);
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

        appendSourceNode(node, context);
    }

    private void appendSourceNode(Node node, RenderContext context) {
        Optional<TokenRange> tokenRange = node.getTokenRange();
        if (tokenRange.isEmpty()) {
            context.appendText(sourceText(node));
            return;
        }

        TokenRange range = tokenRange.get();
        int sourceIndent = sourceIndent(range.getBegin());
        int renderedSourceIndent = context.sourceStartColumn();
        int renderedInlineIndent = Math.max(0, renderedSourceIndent - context.currentIndentColumns());
        int sourceIndentToStrip = Math.max(0, sourceIndent - renderedInlineIndent);
        int[] indentToStrip = {0};
        JavaToken token = range.getBegin();

        while (true) {
            appendSourceToken(token, sourceIndentToStrip, indentToStrip, context);
            if (token == range.getEnd()) {
                return;
            }

            Optional<JavaToken> nextToken = token.getNextToken();
            if (nextToken.isEmpty()) {
                return;
            }
            token = nextToken.get();
        }
    }

    private void appendSourceToken(
            JavaToken token,
            int sourceIndentToStrip,
            int[] indentToStrip,
            RenderContext context
    ) {
        if (token.getCategory().isWhitespace()) {
            context.appendSourceWhitespace(stripSourceIndent(token.getText(), sourceIndentToStrip, indentToStrip));
            return;
        }

        indentToStrip[0] = 0;
        context.appendSourceTokenText(token.getText());
    }

    private int sourceIndent(JavaToken token) {
        return token.getRange()
                .map(range -> Math.max(0, range.begin.column - 1))
                .orElse(0);
    }

    private String stripSourceIndent(String text, int sourceIndentToStrip, int[] indentToStrip) {
        if (sourceIndentToStrip <= 0 || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder(text.length());
        int index = 0;
        while (index < text.length()) {
            char ch = text.charAt(index);

            if (indentToStrip[0] > 0) {
                if (ch == ' ') {
                    indentToStrip[0]--;
                    index++;
                    continue;
                }
                if (ch == '\t') {
                    indentToStrip[0] = Math.max(0, indentToStrip[0] - 4);
                    index++;
                    continue;
                }
                indentToStrip[0] = 0;
            }

            if (ch == '\r') {
                result.append(ch);
                index++;
                if (index < text.length() && text.charAt(index) == '\n') {
                    result.append('\n');
                    index++;
                }
                indentToStrip[0] = sourceIndentToStrip;
                continue;
            }

            if (ch == '\n') {
                result.append(ch);
                index++;
                indentToStrip[0] = sourceIndentToStrip;
                continue;
            }

            result.append(ch);
            index++;
        }

        return result.toString();
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

}
