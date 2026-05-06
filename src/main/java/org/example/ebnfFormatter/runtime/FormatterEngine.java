package org.example.ebnfFormatter.runtime;

import com.github.javaparser.ast.Node;
import org.example.ebnfFormatter.match.MatchResult;
import org.example.ebnfFormatter.match.PatternMatcher;
import org.example.ebnfFormatter.model.RuleDef;
import org.example.ebnfFormatter.render.TemplateRenderer;

public final class FormatterEngine {
    private final RuleRegistry ruleRegistry;
    private final PatternMatcher patternMatcher;
    private final TemplateRenderer templateRenderer;

    public FormatterEngine(
            RuleRegistry ruleRegistry,
            PatternMatcher patternMatcher,
            TemplateRenderer templateRenderer
    ) {
        this.ruleRegistry = ruleRegistry;
        this.patternMatcher = patternMatcher;
        this.templateRenderer = templateRenderer;
    }

    public String format(Node node, String ruleName) {
        RuleDef rule = ruleRegistry.require(ruleName);
        MatchResult match = patternMatcher.match(rule.pattern(), node);

        if (!match.matched()) {
            throw new IllegalArgumentException(
                    "Node does not match rule <" + ruleName + ">"
            );
        }

        return templateRenderer.render(rule.format(), match.bindings());
    }
}