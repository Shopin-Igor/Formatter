package org.example.ebnfFormatter.match;

public record MatchResult(boolean matched, Bindings bindings) {
    public static MatchResult success(Bindings bindings) {
        return new MatchResult(true, bindings);
    }

    public static MatchResult failure() {
        return new MatchResult(false, new Bindings());
    }
}