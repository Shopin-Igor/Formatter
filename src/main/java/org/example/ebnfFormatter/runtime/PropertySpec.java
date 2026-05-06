package org.example.ebnfFormatter.runtime;

import java.util.function.Function;

public record PropertySpec(
        String name,
        Function<Object, Object> getter,
        boolean optional,
        boolean list
) {
    public Object get(Object target) {
        return getter.apply(target);
    }
}