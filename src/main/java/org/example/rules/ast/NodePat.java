package org.example.rules.ast;

import java.util.List;

public record NodePat(String typeName, List<FieldPat> fields) implements RuleAst {
    @Override
    public String dump() {
        StringBuilder sb = new StringBuilder("(node ").append(typeName);
        for (FieldPat f : fields) sb.append("\n  ").append(RuleAst.indent(f.dump()));
        sb.append("\n)");
        return sb.toString();
    }
}
