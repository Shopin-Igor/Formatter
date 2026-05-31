package org.example.ebnfFormatter.render;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class SourceCursor {
    private final JavaToken begin;
    private final JavaToken end;
    private JavaToken lastConsumed;

    private SourceCursor(TokenRange range) {
        this.begin = range.getBegin();
        this.end = range.getEnd();
    }

    static Optional<SourceCursor> create(Object value) {
        return tokenRange(value).map(SourceCursor::new);
    }

    void consumeLiteral(String text, RenderContext context) {
        String sourceText = removeWhitespace(text);
        if (sourceText.isEmpty()) {
            context.appendPendingWhitespace(text);
            return;
        }

        Optional<List<JavaToken>> tokens = nextSourceTokens(sourceText);
        if (tokens.isEmpty()) {
            context.flushPendingWhitespace();
            return;
        }

        List<JavaToken> matched = tokens.get();
        applyGapBefore(matched.getFirst(), context);
        lastConsumed = matched.getLast();
    }

    void consumeValue(Object value, RenderContext context) {
        Optional<TokenRange> range = tokenRange(value);
        if (range.isEmpty() || !contains(range.get().getBegin()) || !contains(range.get().getEnd())) {
            context.flushPendingWhitespace();
            return;
        }

        if (applyGapBefore(range.get().getBegin(), context)) {
            lastConsumed = range.get().getEnd();
            return;
        }

        context.flushPendingWhitespace();
    }

    private Optional<List<JavaToken>> nextSourceTokens(String text) {
        Optional<JavaToken> current = nextToken();
        while (current.isPresent() && current.get().getCategory().isWhitespaceOrComment()) {
            current = current.get().getNextToken();
        }

        StringBuilder matchedText = new StringBuilder();
        List<JavaToken> matchedTokens = new ArrayList<>();

        while (current.isPresent()) {
            JavaToken token = current.get();
            if (!contains(token) || token.getCategory().isWhitespaceOrComment()) {
                return Optional.empty();
            }

            matchedText.append(token.getText());
            if (!text.startsWith(matchedText.toString())) {
                return Optional.empty();
            }

            matchedTokens.add(token);
            if (matchedText.toString().equals(text)) {
                return Optional.of(matchedTokens);
            }

            current = token.getNextToken();
        }

        return Optional.empty();
    }

    private boolean applyGapBefore(JavaToken target, RenderContext context) {
        Optional<OriginalGap> gap = gapBefore(target);
        if (gap.isEmpty()) {
            return false;
        }

        if (gap.get().hasComment()) {
            context.replacePendingWhitespaceWithRaw(gap.get().text());
        } else {
            context.flushPendingWhitespace();
        }
        return true;
    }

    void finish(RenderContext context) {
        Optional<OriginalGap> gap = remainingGap();
        if (gap.isPresent() && gap.get().hasComment()) {
            context.replacePendingWhitespaceWithRaw(gap.get().text());
        }
    }

    private Optional<OriginalGap> gapBefore(JavaToken target) {
        Optional<JavaToken> current = lastConsumed == null
                ? Optional.of(begin)
                : lastConsumed.getNextToken();
        StringBuilder text = new StringBuilder();
        boolean hasComment = false;

        while (current.isPresent() && current.get() != target) {
            JavaToken token = current.get();
            if (!contains(token) || !token.getCategory().isWhitespaceOrComment()) {
                return Optional.empty();
            }
            hasComment = hasComment || token.getCategory().isComment();
            text.append(token.getText());
            current = token.getNextToken();
        }

        return current.isPresent() ? Optional.of(new OriginalGap(text.toString(), hasComment)) : Optional.empty();
    }

    private Optional<OriginalGap> remainingGap() {
        Optional<JavaToken> current = nextToken();
        StringBuilder text = new StringBuilder();
        boolean hasComment = false;

        while (current.isPresent()) {
            JavaToken token = current.get();
            if (!contains(token) || !token.getCategory().isWhitespaceOrComment()) {
                return Optional.empty();
            }
            hasComment = hasComment || token.getCategory().isComment();
            text.append(token.getText());
            if (token == end) {
                return Optional.of(new OriginalGap(text.toString(), hasComment));
            }
            current = token.getNextToken();
        }

        return Optional.empty();
    }

    private Optional<JavaToken> nextToken() {
        if (lastConsumed == null) {
            return Optional.of(begin);
        }
        if (lastConsumed == end) {
            return Optional.empty();
        }
        return lastConsumed.getNextToken();
    }

    private boolean contains(JavaToken token) {
        Optional<JavaToken> current = Optional.of(begin);
        while (current.isPresent()) {
            JavaToken currentToken = current.get();
            if (currentToken == token) {
                return true;
            }
            if (currentToken == end) {
                return false;
            }
            current = currentToken.getNextToken();
        }
        return false;
    }

    private static String removeWhitespace(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!Character.isWhitespace(ch)) {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static Optional<TokenRange> tokenRange(Object value) {
        if (value instanceof Node node) {
            return node.getTokenRange();
        }

        if (value instanceof Optional<?> optional) {
            return optional.flatMap(SourceCursor::tokenRange);
        }

        return Optional.empty();
    }
}
