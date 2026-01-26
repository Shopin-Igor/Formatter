package org.example.rules.ast;

public record RuleDef(String name, RuleAst body) implements RuleAst {
    @Override
    public String dump() {
        return "(rule <" + name + ">\n  "
                + RuleAst.indent(body.dump()) + "\n)";
    }
}
