package org.example.ebnfFormatter.model.pattern;

import java.util.List;

public record NodePat(String typeName, List<FieldPat> fields) implements PatternAst {
    @Override
    public String dump() {
        StringBuilder sb = new StringBuilder("(node ").append(typeName);
        for (FieldPat f : fields) sb.append("\n  ").append(PatternAst.indent(f.dump()));
        sb.append("\n)");
        return sb.toString();
    }
}
