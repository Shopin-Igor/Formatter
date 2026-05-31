package org.example.ebnfFormatter.render;

public final class RenderContext {
    private static final String INDENT_UNIT = "    ";

    private final StringBuilder out = new StringBuilder();
    private final StringBuilder pendingWhitespace = new StringBuilder();
    private int indentLevel = 0;
    private boolean lineStart = true;
    private int column = 0;

    public void appendText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        flushPendingWhitespace();
        appendIndentedText(text);
    }

    public void appendRawText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        flushPendingWhitespace();
        appendRawCommitted(text);
    }

    void appendSourceWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        flushPendingWhitespace();
        appendWhitespace(text);
    }

    void appendSourceTokenText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        flushPendingWhitespace();
        if (lineStart) {
            appendIndentIfNeeded();
        }
        appendRawCommitted(text);
    }

    public void replacePendingWhitespaceWithRaw(String text) {
        if (startsOnPendingLine(text)) {
            flushPendingWhitespace();
            appendIndentedText(text);
            return;
        }

        pendingWhitespace.setLength(0);
        appendRawCommitted(text);
    }

    public void appendPendingWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        pendingWhitespace.append(text);
    }

    public void flushPendingWhitespace() {
        if (pendingWhitespace.isEmpty()) {
            return;
        }

        appendWhitespace(pendingWhitespace.toString());
        pendingWhitespace.setLength(0);
    }

    public void space() {
        appendPendingWhitespace(" ");
    }

    public void newline() {
        appendPendingWhitespace("\n");
    }

    public void indent() {
        indentLevel++;
    }

    public void dedent() {
        if (indentLevel == 0) {
            return;
        }
        indentLevel--;
    }

    public boolean isLineStart() {
        if (pendingWhitespace.isEmpty()) {
            return lineStart;
        }

        char last = pendingWhitespace.charAt(pendingWhitespace.length() - 1);
        return last == '\n' || last == '\r';
    }

    int sourceStartColumn() {
        flushPendingWhitespace();
        if (lineStart) {
            return currentIndentColumns();
        }
        return column;
    }

    int currentIndentColumns() {
        return indentLevel * INDENT_UNIT.length();
    }

    public String result() {
        flushPendingWhitespace();
        return out.toString();
    }

    private void appendIndentedText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (lineStart) {
                appendIndentIfNeeded();
            }

            out.append(ch);

            if (ch == '\n') {
                lineStart = true;
                column = 0;
            } else {
                lineStart = false;
                column++;
            }
        }
    }

    private void appendWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n' || ch == '\r') {
                out.append(ch);
                lineStart = true;
                column = 0;
                continue;
            }

            if (lineStart) {
                appendIndentIfNeeded();
            }
            out.append(ch);
            lineStart = false;
            column++;
        }
    }

    private void appendRawCommitted(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            out.append(ch);
            if (ch == '\n' || ch == '\r') {
                lineStart = true;
                column = 0;
            } else {
                lineStart = false;
                column++;
            }
        }
    }

    private boolean startsOnPendingLine(String text) {
        return !pendingWhitespace.isEmpty()
                && isLineStart()
                && startsWithoutIndent(text);
    }

    private boolean startsWithoutIndent(String text) {
        return text != null
                && !text.isEmpty()
                && text.charAt(0) != ' '
                && text.charAt(0) != '\t'
                && text.charAt(0) != '\n'
                && text.charAt(0) != '\r';
    }

    private void appendIndentIfNeeded() {
        for (int i = 0; i < indentLevel; i++) {
            out.append(INDENT_UNIT);
            column += INDENT_UNIT.length();
        }
        lineStart = false;
    }
}
