package org.example.ebnfFormatter.match;

import org.example.ebnfFormatter.model.RuleDef;

public record AppliedRule(
        String logicalName,
        RuleDef rule,
        Object sourceValue,
        Bindings bindings
) {}