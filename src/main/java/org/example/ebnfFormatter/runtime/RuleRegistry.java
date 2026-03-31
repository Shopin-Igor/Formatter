package org.example.ebnfFormatter.runtime;

import org.example.ebnfFormatter.model.RuleDef;

import java.util.*;

public final class RuleRegistry {
    private final Map<String, List<RuleDef>> rulesByName = new HashMap<>();

    public void register(RuleDef rule) {
        rulesByName.computeIfAbsent(rule.name(), k -> new ArrayList<>()).add(rule);
    }

    public void registerAll(Collection<RuleDef> rules) {
        for (RuleDef rule : rules) {
            register(rule);
        }
    }

    public List<RuleDef> requireAll(String ruleName) {
        List<RuleDef> rules = rulesByName.get(ruleName);
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("Unknown rule: " + ruleName);
        }
        return rules;
    }

    public List<RuleDef> findAll(String ruleName) {
        return rulesByName.getOrDefault(ruleName, List.of());
    }

    public boolean contains(String ruleName) {
        return rulesByName.containsKey(ruleName);
    }
}