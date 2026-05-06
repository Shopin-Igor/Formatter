package org.example.ebnfFormatter.match;

import com.github.javaparser.ast.Node;
import org.example.ebnfFormatter.model.RuleDef;
import org.example.ebnfFormatter.model.pattern.Alt;
import org.example.ebnfFormatter.model.pattern.FieldPat;
import org.example.ebnfFormatter.model.pattern.ListPat;
import org.example.ebnfFormatter.model.pattern.Lit;
import org.example.ebnfFormatter.model.pattern.NodePat;
import org.example.ebnfFormatter.model.pattern.PatternAst;
import org.example.ebnfFormatter.model.pattern.Quant;
import org.example.ebnfFormatter.model.pattern.RuleRef;
import org.example.ebnfFormatter.model.pattern.Seq;
import org.example.ebnfFormatter.runtime.RuleRegistry;
import org.example.ebnfFormatter.runtime.TypeRegistryUniversal;
import org.example.ebnfFormatter.runtime.TypeSpec;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PatternMatcher {

    private final TypeRegistryUniversal typeRegistry;
    private final RuleRegistry ruleRegistry;

    public PatternMatcher(TypeRegistryUniversal typeRegistry, RuleRegistry ruleRegistry) {
        this.typeRegistry = typeRegistry;
        this.ruleRegistry = ruleRegistry;
    }

    public MatchResult match(RuleDef rule, Object value) {
        AppliedRule appliedRule = matchRuleApplication(rule.name(), rule, value);
        if (appliedRule == null) {
            return MatchResult.failure();
        }
        return MatchResult.success(appliedRule);
    }

    private AppliedRule matchRuleApplication(String logicalName, RuleDef rule, Object value) {
        Bindings bindings = new Bindings();
        if (!matchFork(rule.pattern(), value, bindings)) {
            return null;
        }

        Bindings scopedBindings = bindings.copy();
        attachSelfBindings(scopedBindings, logicalName, rule.pattern(), value);
        return new AppliedRule(logicalName, rule, value, scopedBindings);
    }

    private boolean matchFork(PatternAst pattern, Object value, Bindings bindings) {
        return switch (pattern) {
            case Lit lit -> matchLit(lit, value);
            case RuleRef ref -> matchRuleRef(ref, value, bindings);
            case NodePat nodePat -> matchNodePat(nodePat, value, bindings);
            case Seq seq -> matchSeq(seq, value, bindings);
            case Alt alt -> matchAlt(alt, value, bindings);
            case ListPat listPat -> matchListPat(listPat, value, bindings);
            case Quant quant -> matchQuant(quant, value, bindings);
            default -> false;
        };
    }

    private boolean matchLit(Lit pat, Object value) {
        return Objects.equals(pat.value(), value);
    }

    private boolean matchRuleRef(RuleRef ref, Object value, Bindings bindings) {
        List<RuleDef> rules = ruleRegistry.findAll(ref.name());

        if (!rules.isEmpty()) {
            for (RuleDef rule : rules) {
                AppliedRule appliedRule = matchRuleApplication(ref.name(), rule, value);
                if (appliedRule != null) {
                    return bindings.bind(ref.name(), new AppliedRuleValue(appliedRule));
                }
            }
            return bindRawValueIfMatchesDslType(ref.name(), value, bindings);
        }

        if (value == null) {
            return bindings.bind(ref.name(), new RawValue(null));
        }

        try {
            TypeSpec spec = typeRegistry.requireByDslName(ref.name());
            if (!spec.javaType().isInstance(value)) {
                return false;
            }
        } catch (IllegalArgumentException e) {
            return bindRawValue(ref.name(), value, bindings);
        }

        return bindRawValue(ref.name(), value, bindings);
    }

    private boolean bindRawValueIfMatchesDslType(String refName, Object value, Bindings bindings) {
        if (value == null) {
            return false;
        }

        try {
            TypeSpec spec = typeRegistry.requireByDslName(refName);
            if (!spec.javaType().isInstance(value)) {
                return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }

        return bindRawValue(refName, value, bindings);
    }

    private boolean matchNodePat(NodePat pat, Object value, Bindings bindings) {
        if (!(value instanceof Node node)) {
            return false;
        }

        TypeSpec spec = typeRegistry.requireByDslName(pat.typeName());
        if (!spec.javaType().isInstance(node)) {
            return false;
        }

        Bindings copy = bindings.copy();

        for (FieldPat field : pat.fields()) {
            Object fieldValue = typeRegistry.readProperty(node, field.name());

            if (field.optional() && fieldValue == null) {
                continue;
            }

            if (!matchFork(field.value(), fieldValue, copy)) {
                return false;
            }
        }

        bindings.replaceWith(copy);
        return true;
    }

    private boolean matchSeq(Seq seq, Object value, Bindings bindings) {
        List<?> values = toList(value);
        return matchSequence(seq.items(), values, 0, 0, bindings) == values.size();
    }

    private int matchSequence(List<PatternAst> patterns, List<?> values, int patternIndex, int valueIndex, Bindings bindings) {
        if (patternIndex == patterns.size()) {
            return valueIndex;
        }

        PatternAst currentPattern = patterns.get(patternIndex);

        if (currentPattern instanceof Quant quant) {
            return matchQuantified(patterns, values, patternIndex, valueIndex, quant, bindings);
        }

        if (valueIndex >= values.size()) {
            return -1;
        }

        Bindings trial = bindings.copy();
        if (!matchFork(currentPattern, values.get(valueIndex), trial)) {
            return -1;
        }

        int nextIndex = matchSequence(patterns, values, patternIndex + 1, valueIndex + 1, trial);
        if (nextIndex == -1) {
            return -1;
        }

        bindings.replaceWith(trial);
        return nextIndex;
    }

    private int matchQuantified(
            List<PatternAst> patterns,
            List<?> values,
            int patternIndex,
            int valueIndex,
            Quant quant,
            Bindings bindings
    ) {
        return switch (quant.quantifier()) {
            case OPTIONAL -> matchOptional(patterns, values, patternIndex, valueIndex, quant.pattern(), bindings);
            case ZERO_OR_MORE -> matchZeroOrMore(patterns, values, patternIndex, valueIndex, quant.pattern(), bindings);
            case ONE_OR_MORE -> matchOneOrMore(patterns, values, patternIndex, valueIndex, quant.pattern(), bindings);
        };
    }

    private int matchOptional(
            List<PatternAst> patterns,
            List<?> values,
            int patternIndex,
            int valueIndex,
            PatternAst inner,
            Bindings bindings
    ) {
        Bindings skipBindings = bindings.copy();
        int skipResult = matchSequence(patterns, values, patternIndex + 1, valueIndex, skipBindings);
        if (skipResult != -1) {
            bindings.replaceWith(skipBindings);
            return skipResult;
        }

        if (valueIndex >= values.size()) {
            return -1;
        }

        Bindings takeBindings = bindings.copy();
        Bindings iterationBindings = matchQuantifiedIteration(inner, values.get(valueIndex));
        if (iterationBindings == null) {
            return -1;
        }
        takeBindings.appendAll(iterationBindings);

        int takeResult = matchSequence(patterns, values, patternIndex + 1, valueIndex + 1, takeBindings);
        if (takeResult == -1) {
            return -1;
        }

        bindings.replaceWith(takeBindings);
        return takeResult;
    }

    private int matchZeroOrMore(
            List<PatternAst> patterns,
            List<?> values,
            int patternIndex,
            int valueIndex,
            PatternAst inner,
            Bindings bindings
    ) {
        List<Bindings> snapshots = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();

        Bindings currentBindings = bindings.copy();
        int currentIndex = valueIndex;

        snapshots.add(currentBindings.copy());
        indexes.add(currentIndex);

        while (currentIndex < values.size()) {
            Bindings iterationBindings = matchQuantifiedIteration(inner, values.get(currentIndex));
            if (iterationBindings == null) {
                break;
            }

            currentBindings.appendAll(iterationBindings);
            currentIndex++;

            snapshots.add(currentBindings.copy());
            indexes.add(currentIndex);
        }

        for (int i = snapshots.size() - 1; i >= 0; i--) {
            Bindings candidate = snapshots.get(i).copy();
            int candidateIndex = indexes.get(i);

            int result = matchSequence(patterns, values, patternIndex + 1, candidateIndex, candidate);
            if (result != -1) {
                bindings.replaceWith(candidate);
                return result;
            }
        }

        return -1;
    }

    private int matchOneOrMore(
            List<PatternAst> patterns,
            List<?> values,
            int patternIndex,
            int valueIndex,
            PatternAst inner,
            Bindings bindings
    ) {
        if (valueIndex >= values.size()) {
            return -1;
        }

        Bindings firstIteration = matchQuantifiedIteration(inner, values.get(valueIndex));
        if (firstIteration == null) {
            return -1;
        }

        Bindings firstBindings = bindings.copy();
        firstBindings.appendAll(firstIteration);

        int result = matchZeroOrMoreAfterFirst(patterns, values, patternIndex, valueIndex + 1, inner, firstBindings);
        if (result != -1) {
            bindings.replaceWith(firstBindings);
        }
        return result;
    }

    private int matchZeroOrMoreAfterFirst(
            List<PatternAst> patterns,
            List<?> values,
            int patternIndex,
            int valueIndex,
            PatternAst inner,
            Bindings bindings
    ) {
        List<Bindings> snapshots = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();

        Bindings currentBindings = bindings.copy();
        int currentIndex = valueIndex;

        snapshots.add(currentBindings.copy());
        indexes.add(currentIndex);

        while (currentIndex < values.size()) {
            Bindings iterationBindings = matchQuantifiedIteration(inner, values.get(currentIndex));
            if (iterationBindings == null) {
                break;
            }

            currentBindings.appendAll(iterationBindings);
            currentIndex++;

            snapshots.add(currentBindings.copy());
            indexes.add(currentIndex);
        }

        for (int i = snapshots.size() - 1; i >= 0; i--) {
            Bindings candidate = snapshots.get(i).copy();
            int candidateIndex = indexes.get(i);

            int result = matchSequence(patterns, values, patternIndex + 1, candidateIndex, candidate);
            if (result != -1) {
                bindings.replaceWith(candidate);
                return result;
            }
        }

        return -1;
    }

    private boolean matchAlt(Alt pat, Object value, Bindings bindings) {
        for (PatternAst option : pat.options()) {
            Bindings copy = bindings.copy();
            if (matchFork(option, value, copy)) {
                bindings.replaceWith(copy);
                return true;
            }
        }
        return false;
    }

    private boolean matchListPat(ListPat pat, Object value, Bindings bindings) {
        List<?> values = toList(value);
        return matchSequence(pat.items(), values, 0, 0, bindings) == values.size();
    }

    private boolean matchQuant(Quant quant, Object value, Bindings bindings) {
        return switch (quant.quantifier()) {
            case OPTIONAL -> {
                if (value == null) {
                    yield true;
                }

                Bindings iterationBindings = matchQuantifiedIteration(quant.pattern(), value);
                if (iterationBindings == null) {
                    yield false;
                }

                Bindings copy = bindings.copy();
                copy.appendAll(iterationBindings);
                bindings.replaceWith(copy);
                yield true;
            }
            case ZERO_OR_MORE -> {
                if (value == null) {
                    yield true;
                }

                Bindings copy = bindings.copy();
                for (Object item : toList(value)) {
                    Bindings iterationBindings = matchQuantifiedIteration(quant.pattern(), item);
                    if (iterationBindings == null) {
                        yield false;
                    }
                    copy.appendAll(iterationBindings);
                }

                bindings.replaceWith(copy);
                yield true;
            }
            case ONE_OR_MORE -> {
                if (value == null) {
                    yield false;
                }

                List<?> values = toList(value);
                if (values.isEmpty()) {
                    yield false;
                }

                Bindings copy = bindings.copy();
                for (Object item : values) {
                    Bindings iterationBindings = matchQuantifiedIteration(quant.pattern(), item);
                    if (iterationBindings == null) {
                        yield false;
                    }
                    copy.appendAll(iterationBindings);
                }

                bindings.replaceWith(copy);
                yield true;
            }
        };
    }

    private Bindings matchQuantifiedIteration(PatternAst inner, Object value) {
        Bindings iterationBindings = new Bindings();
        if (!matchFork(inner, value, iterationBindings)) {
            return null;
        }
        return iterationBindings;
    }

    private void attachSelfBindings(Bindings bindings, String logicalName, PatternAst pattern, Object value) {
        if (pattern instanceof NodePat nodePat) {
            bindIfAbsent(bindings, nodePat.typeName(), new RawValue(value));
        }
    }

    private void bindIfAbsent(Bindings bindings, String name, BoundValue value) {
        if (!bindings.hasBinding(name)) {
            bindings.bind(name, value);
        }
    }

    private boolean bindRawValue(String name, Object value, Bindings bindings) {
        if (value == null) {
            return bindings.bind(name, new RawValue(null));
        }

        if (value instanceof Iterable<?> iterable) {
            List<BoundValue> items = new ArrayList<>();
            for (Object item : iterable) {
                items.add(new RawValue(item));
            }
            return bindings.bindAll(name, items);
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<BoundValue> items = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                items.add(new RawValue(Array.get(value, i)));
            }
            return bindings.bindAll(name, items);
        }

        return bindings.bind(name, new RawValue(value));
    }

    private List<?> toList(Object value) {
        if (value == null) {
            return List.of();
        }

        if (value instanceof List<?> list) {
            return list;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> result = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                result.add(Array.get(value, i));
            }
            return result;
        }

        return List.of(value);
    }
}
