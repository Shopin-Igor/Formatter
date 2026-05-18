package org.example.ebnfFormatter.match;

public record AppliedRuleValue(AppliedRule appliedRule) implements BoundValue {
    @Override
    public Object legacyValue() {
        return appliedRule.sourceValue();
    }
}