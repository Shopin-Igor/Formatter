package org.example.ebnfFormatter.render;

public final class RenderContext {
    private static final String INDENT_UNIT = "    ";

    private final StringBuilder out = new StringBuilder();
    private int indentLevel = 0;
    private boolean lineStart = true;

    public void appendText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (lineStart) {
                appendIndentIfNeeded();
            }

            out.append(ch);

            if (ch == '\n') {
                lineStart = true;
            } else {
                lineStart = false;
            }
        }
    }

    public void space() {
        if (lineStart) {
            appendIndentIfNeeded();
        }
        out.append(' ');
        lineStart = false;
    }

    public void newline() {
        out.append('\n');
        lineStart = true;
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

    public String result() {
        return out.toString();
    }

    private void appendIndentIfNeeded() {
        for (int i = 0; i < indentLevel; i++) {
            out.append(INDENT_UNIT);
        }
        lineStart = false;
    }
}