package org.example.ebnfFormatter.render;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.Node;

import java.util.Optional;

final class OriginalGaps {
    private OriginalGaps() {
    }

    static Optional<OriginalGap> between(Object left, Object right) {
        Optional<TokenRange> leftRange = tokenRange(left);
        Optional<TokenRange> rightRange = tokenRange(right);
        if (leftRange.isEmpty() || rightRange.isEmpty()) {
            return Optional.empty();
        }

        JavaToken end = rightRange.get().getBegin();
        Optional<JavaToken> current = leftRange.get().getEnd().getNextToken();
        StringBuilder text = new StringBuilder();
        boolean hasComment = false;

        while (current.isPresent() && current.get() != end) {
            JavaToken token = current.get();
            if (!token.getCategory().isWhitespaceOrComment()) {
                return Optional.empty();
            }
            hasComment = hasComment || token.getCategory().isComment();
            text.append(token.getText());
            current = token.getNextToken();
        }

        return current.isPresent() ? Optional.of(new OriginalGap(text.toString(), hasComment)) : Optional.empty();
    }

    static Optional<OriginalGap> after(Object value) {
        Optional<TokenRange> range = tokenRange(value);
        if (range.isEmpty()) {
            return Optional.empty();
        }

        Optional<JavaToken> current = range.get().getEnd().getNextToken();
        StringBuilder text = new StringBuilder();
        boolean hasComment = false;

        while (current.isPresent() && current.get().getCategory().isWhitespaceOrComment()) {
            JavaToken token = current.get();
            hasComment = hasComment || token.getCategory().isComment();
            text.append(token.getText());
            current = token.getNextToken();
        }

        return Optional.of(new OriginalGap(text.toString(), hasComment));
    }

    private static Optional<TokenRange> tokenRange(Object value) {
        if (value instanceof Node node) {
            return node.getTokenRange();
        }

        if (value instanceof Optional<?> optional) {
            return optional.flatMap(OriginalGaps::tokenRange);
        }

        return Optional.empty();
    }
}
