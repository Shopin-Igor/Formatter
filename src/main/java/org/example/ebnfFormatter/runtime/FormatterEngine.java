package org.example.ebnfFormatter.runtime;

import com.github.javaparser.ast.Node;
import org.example.ebnfFormatter.match.MatchResult;
import org.example.ebnfFormatter.match.PatternMatcher;
import org.example.ebnfFormatter.model.RuleDef;
import org.example.ebnfFormatter.render.TemplateRenderer;

import java.util.Optional;

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
        return tryRender(ruleName, node)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Node does not match any rule <" + ruleName + ">"
                ));
    }

    public Optional<String> tryRender(String ruleName, Object value) {
        for (RuleDef rule : ruleRegistry.requireAll(ruleName)) {
            MatchResult match = patternMatcher.match(rule, value);
            if (match.matched()) {
                return Optional.of(templateRenderer.render(
                        rule.format(),
                        match.bindings(),
                        this::tryRenderNested
                ));
            }
        }
        return Optional.empty();
    }

    private Optional<String> tryRenderNested(String ruleName, Object value) {
        if (!ruleRegistry.contains(ruleName)) {
            return Optional.empty();
        }
        return tryRender(ruleName, value);
    }
}
