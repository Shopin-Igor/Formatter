package org.example.ebnfFormatter.render;

import java.util.Optional;

@FunctionalInterface
public interface NestedRuleRenderer {
    Optional<String> tryRender(String ruleName, Object value);
}
