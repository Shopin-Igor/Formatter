package org.example.ebnfFormatter.render;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

final class SourceRenderState {
    private final Deque<Optional<SourceCursor>> cursors = new ArrayDeque<>();

    void enter(Object sourceValue) {
        cursors.push(SourceCursor.create(sourceValue));
    }

    void exit(RenderContext context) {
        Optional<SourceCursor> cursor = cursors.pop();
        cursor.ifPresent(sourceCursor -> sourceCursor.finish(context));
    }

    void beforeLiteral(String text, RenderContext context) {
        Optional<SourceCursor> cursor = currentCursor();
        if (cursor.isEmpty()) {
            context.flushPendingWhitespace();
            return;
        }

        cursor.get().consumeLiteral(text, context);
    }

    void beforeValue(Object value, RenderContext context) {
        Optional<SourceCursor> cursor = currentCursor();
        if (cursor.isEmpty()) {
            context.flushPendingWhitespace();
            return;
        }

        cursor.get().consumeValue(value, context);
    }

    private Optional<SourceCursor> currentCursor() {
        if (cursors.isEmpty()) {
            return Optional.empty();
        }
        return cursors.peek();
    }
}
