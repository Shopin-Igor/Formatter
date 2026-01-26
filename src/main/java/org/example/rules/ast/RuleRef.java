package org.example.rules.ast;

public record RuleRef(String name) implements RuleAst {
    @Override public String dump() {
        return "(ref <" + name + ">)";
    }
}
