package org.example.ebnfFormatter.runtime;

import java.util.Map;

public record TypeSpec(
        String dslName,
        Class<?> javaType,
        Map<String, PropertySpec> properties
) {
    public PropertySpec property(String name) {
        PropertySpec spec = properties.get(name);
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Unknown property '%s' for DSL type '%s'".formatted(name, dslName)
            );
        }
        return spec;
    }
}