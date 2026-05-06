package org.example.ebnfFormatter.match;

public record RawValue(Object value) implements BoundValue {
    @Override
    public Object legacyValue() {
        return value;
    }
}
