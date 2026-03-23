package org.example.ebnfFormatter.match;

import com.github.javaparser.ast.Node;
import org.example.ebnfFormatter.model.pattern.*;
import org.example.ebnfFormatter.runtime.TypeRegistry;
import org.example.ebnfFormatter.runtime.TypeRegistryUniversal;
import org.example.ebnfFormatter.runtime.TypeSpec;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PatternMatcher {

    private final TypeRegistryUniversal typeRegistry;

    public PatternMatcher(TypeRegistryUniversal typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    private boolean matchFork(PatternAst pattern, Object value, Bindings b) {
        return switch (pattern) {
            case Lit lit -> matchLit(lit, value);
            case RuleRef ref -> matchRuleRef(ref, value, b);
            case NodePat nodePat -> matchNodePat(nodePat, value, b);
            case Seq seq -> matchSeq(seq, value, b);
            case Alt alt -> matchAlt(alt, value, b);
            case ListPat listPat -> matchListPat(listPat, value, b);
            case Quant quant -> matchQuant(quant, value, b);
            default -> false;
        };
    }

    private boolean matchLit(Lit pat, Object value) {
        return Objects.equals(pat.value(), value);
    }

    private boolean matchRuleRef(RuleRef ref, Object value, Bindings bindings) {
        if (value == null) {
            return bindings.bind(ref.name(), null);
        }

        TypeSpec spec;
        try {
            spec = typeRegistry.requireByDslName(ref.name());
        } catch (IllegalArgumentException e) {
            return bindings.bind(ref.name(), value);
        }

        if (value instanceof Node node) {
            if (!spec.javaType().isInstance(node)) {
                return false;
            }
            return bindings.bind(ref.name(), value);
        }

        if (!spec.javaType().isInstance(value)) {
            return false;
        }

        return bindings.bind(ref.name(), value);
    }

    private boolean matchNodePat(NodePat pat, Object value, Bindings b) {
        if (!(value instanceof Node node)) {
            return false;
        }

        TypeSpec spec = typeRegistry.requireByDslName(pat.typeName());
        if (spec == null) {
            return false;
        }

        if (!spec.javaType().isInstance(node)) {
            return false;
        }

        Bindings copy = b.copy();

        for (FieldPat field : pat.fields()) {
            Object fieldValue = typeRegistry.readProperty(node, field.name());

            if (field.optional() && fieldValue == null) {
                continue;
            }

            if (!matchFork(field.value(), fieldValue, copy)) {
                return false;
            }
        }

        b.replaceWith(copy);
        return true;
    }

    private boolean matchSeq(Seq seq, Object value, Bindings b) {
        List<?> values = toList(value);
        return matchSequence(seq.items(), values, 0, 0, b) == values.size();
    }

    private int matchSequence(List<PatternAst> pats, List<?> values, int pi, int vi, Bindings bindings) {
        if (pi == pats.size()) {
            return vi;
        }

        PatternAst currentPattern = pats.get(pi);

        if (currentPattern instanceof Quant quant) {
            return matchQuantified(pats, values, pi, vi, quant, bindings);
        }

        if (vi >= values.size()) {
            return -1;
        }

        Bindings trial = bindings.copy();
        boolean matched = matchFork(currentPattern, values.get(vi), trial);

        if (!matched) {
            return -1;
        }

        int nextIndex = matchSequence(pats, values, pi + 1, vi + 1, trial);
        if (nextIndex == -1) {
            return -1;
        }

        bindings.replaceWith(trial);
        return nextIndex;
    }

    private int matchQuantified(
            List<PatternAst> pats,
            List<?> values,
            int pi,
            int vi,
            Quant quant,
            Bindings bindings
    ) {
        PatternAst inner = quant.pattern();

        return switch (quant.quantifier()) {
            case OPTIONAL -> matchOptional(pats, values, pi, vi, inner, bindings);
            case ZERO_OR_MORE -> matchZeroOrMore(pats, values, pi, vi, inner, bindings);
            case ONE_OR_MORE -> matchOneOrMore(pats, values, pi, vi, inner, bindings);
        };
    }

    private int matchOptional(
            List<PatternAst> pats,
            List<?> values,
            int pi,
            int vi,
            PatternAst inner,
            Bindings bindings) {
        Bindings skipBindings = bindings.copy();
        int skipResult = matchSequence(pats, values, pi + 1, vi, skipBindings);
        if (skipResult != -1) {
            bindings.replaceWith(skipBindings);
            return skipResult;
        }

        if (vi >= values.size()) {
            return -1;
        }

        Bindings takeBindings = bindings.copy();
        if (!matchFork(inner, values.get(vi), takeBindings)) {
            return -1;
        }

        int takeResult = matchSequence(pats, values, pi + 1, vi + 1, takeBindings);
        if (takeResult == -1) {
            return -1;
        }

        bindings.replaceWith(takeBindings);
        return takeResult;
    }

    private int matchZeroOrMore(
            List<PatternAst> pats,
            List<?> values,
            int pi,
            int vi,
            PatternAst inner,
            Bindings bindings) {
        List<Bindings> snapshots = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();

        Bindings currentBindings = bindings.copy();
        int currentIndex = vi;

        snapshots.add(currentBindings.copy());
        indexes.add(currentIndex);

        while (currentIndex < values.size()) {
            Bindings nextBindings = currentBindings.copy();
            if (!matchFork(inner, values.get(currentIndex), nextBindings)) {
                break;
            }

            currentIndex++;
            currentBindings = nextBindings;

            snapshots.add(currentBindings.copy());
            indexes.add(currentIndex);
        }

        for (int i = snapshots.size() - 1; i >= 0; i--) {
            Bindings candidate = snapshots.get(i).copy();
            int candidateIndex = indexes.get(i);

            int result = matchSequence(pats, values, pi + 1, candidateIndex, candidate);
            if (result != -1) {
                bindings.replaceWith(candidate);
                return result;
            }
        }

        return -1;
    }

    private int matchOneOrMore(
            List<PatternAst> pats,
            List<?> values,
            int pi,
            int vi,
            PatternAst inner,
            Bindings bindings
    ) {
        if (vi >= values.size()) {
            return -1;
        }

        Bindings firstBindings = bindings.copy();
        if (!matchFork(inner, values.get(vi), firstBindings)) {
            return -1;
        }

        return matchZeroOrMoreAfterFirst(pats, values, pi, vi + 1, inner, firstBindings);
    }

    private int matchZeroOrMoreAfterFirst(
            List<PatternAst> pats,
            List<?> values,
            int pi,
            int vi,
            PatternAst inner,
            Bindings bindings
    ) {
        List<Bindings> snapshots = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();

        Bindings currentBindings = bindings.copy();
        int currentIndex = vi;

        snapshots.add(currentBindings.copy());
        indexes.add(currentIndex);

        while (currentIndex < values.size()) {
            Bindings nextBindings = currentBindings.copy();
            if (!matchFork(inner, values.get(currentIndex), nextBindings)) {
                break;
            }

            currentIndex++;
            currentBindings = nextBindings;

            snapshots.add(currentBindings.copy());
            indexes.add(currentIndex);
        }

        for (int i = snapshots.size() - 1; i >= 0; i--) {
            Bindings candidate = snapshots.get(i).copy();
            int candidateIndex = indexes.get(i);

            int result = matchSequence(pats, values, pi + 1, candidateIndex, candidate);
            if (result != -1) {
                bindings.replaceWith(candidate);
                return result;
            }
        }

        return -1;
    }

    private boolean matchAlt(Alt pat, Object value, Bindings b) {
        for (PatternAst option : pat.options()) {
            Bindings copy = b.copy();
            if (matchFork(option, value, copy)) {
                b.replaceWith(copy);
                return true;
            }
        }
        return false;
    }

    private boolean matchListPat(ListPat pat, Object value, Bindings b) {
        List<?> values = toList(value);
        return matchSequence(pat.items(), values, 0, 0, b) == values.size();
    }

    private boolean matchQuant(Quant quant, Object value, Bindings b) {
        return switch (quant.quantifier()) {
            case OPTIONAL -> {
                if (value == null) {
                    yield true;
                }
                Bindings copy = b.copy();
                if (!matchFork(quant.pattern(), value, copy)) {
                    yield false;
                }
                b.replaceWith(copy);
                yield true;
            }
            case ZERO_OR_MORE -> {
                if (value == null) {
                    yield true;
                }

                List<?> values = toList(value);
                Bindings copy = b.copy();

                boolean ok = true;
                for (Object item : values) {
                    if (!matchFork(quant.pattern(), item, copy)) {
                        ok = false;
                        break;
                    }
                }

                if (!ok) {
                    yield false;
                }

                b.replaceWith(copy);
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

                Bindings copy = b.copy();

                boolean ok = true;
                for (Object item : values) {
                    if (!matchFork(quant.pattern(), item, copy)) {
                        ok = false;
                        break;
                    }
                }

                if (!ok) {
                    yield false;
                }

                b.replaceWith(copy);
                yield true;
            }
        };
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

    public MatchResult match(PatternAst pattern, Node node) {
        Bindings bindings = new Bindings();
        boolean matched = matchFork(pattern, node, bindings);

        if (!matched) {
            return MatchResult.failure();
        }

        return MatchResult.success(bindings);
    }
}