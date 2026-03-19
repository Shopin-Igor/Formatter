package org.example.ebnfFormatter.runtime;

import org.example.ebnfFormatter.model.RuleDef;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class RuleRegistry {
    private final Map<String, RuleDef> rulesByName = new HashMap<>();

    public void register(RuleDef rule) {
        rulesByName.put(rule.name(), rule);
    }

    public void registerAll(Collection<RuleDef> rules) {
        for (RuleDef rule : rules) {
            register(rule);
        }
    }

    public RuleDef require(String ruleName) {
        RuleDef rule = rulesByName.get(ruleName);
        if (rule == null) {
            throw new IllegalArgumentException("Unknown rule: " + ruleName);
        }
        return rule;
    }

    public RuleDef find(String ruleName) {
        return rulesByName.get(ruleName);
    }

    public boolean contains(String ruleName) {
        return rulesByName.containsKey(ruleName);
    }
}