package org.example.ebnfFormatter.match;

public record MatchResult(boolean matched, AppliedRule appliedRule) {
    public static MatchResult success(AppliedRule appliedRule) {
        return new MatchResult(true, appliedRule);
    }

    public static MatchResult failure() {
        return new MatchResult(false, null);
    }

    public Bindings bindings() {
        if (!matched || appliedRule == null) {
            return new Bindings();
        }
        return appliedRule.bindings();
    }
}
