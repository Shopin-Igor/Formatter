package org.example.ebnfFormatter.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Bindings {

    private final Map<String, List<BoundValue>> bindingsByName = new LinkedHashMap<>();

    public boolean bind(String name, Object value) {
        return bind(name, new RawValue(value));
    }

    public boolean bind(String name, BoundValue value) {
        if (!bindingsByName.containsKey(name)) {
            bindingsByName.put(name, new ArrayList<>(List.of(value)));
            return true;
        }

        List<BoundValue> existing = bindingsByName.get(name);
        return existing.size() == 1 && Objects.equals(existing.getFirst(), value);
    }

    public boolean bindAll(String name, List<BoundValue> values) {
        if (!bindingsByName.containsKey(name)) {
            bindingsByName.put(name, new ArrayList<>(values));
            return true;
        }

        return Objects.equals(bindingsByName.get(name), values);
    }

    public void append(String name, BoundValue value) {
        bindingsByName.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
    }

    public void appendAll(Bindings other) {
        for (Map.Entry<String, List<BoundValue>> entry : other.bindingsByName.entrySet()) {
            bindingsByName
                    .computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                    .addAll(entry.getValue());
        }
    }

    public Bindings copy() {
        Bindings copy = new Bindings();
        for (Map.Entry<String, List<BoundValue>> entry : bindingsByName.entrySet()) {
            copy.bindingsByName.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    public void replaceWith(Bindings other) {
        bindingsByName.clear();
        for (Map.Entry<String, List<BoundValue>> entry : other.bindingsByName.entrySet()) {
            bindingsByName.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
    }

    public Object getRequired(String name) {
        List<BoundValue> values = findValuesInternal(name);
        if (values == null) {
            throw new IllegalArgumentException("No binding for name: " + name);
        }
        return unwrapForLegacyUse(values);
    }

    public Object find(String name) {
        List<BoundValue> values = findValuesInternal(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return unwrapForLegacyUse(values);
    }

    public List<BoundValue> getRequiredValues(String name) {
        List<BoundValue> values = findValuesInternal(name);
        if (values == null) {
            throw new IllegalArgumentException("No binding for name: " + name);
        }
        return List.copyOf(values);
    }

    public List<BoundValue> findValues(String name) {
        List<BoundValue> values = findValuesInternal(name);
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }

    public boolean hasBinding(String name) {
        return findValuesInternal(name) != null;
    }

    public Set<String> getBindingNames() {
        return Collections.unmodifiableSet(bindingsByName.keySet());
    }

    public Map<String, List<BoundValue>> asUnmodifiableMap() {
        Map<String, List<BoundValue>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<BoundValue>> entry : bindingsByName.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public String toString() {
        return bindingsByName.toString();
    }

    private List<BoundValue> findValuesInternal(String name) {
        List<BoundValue> exact = bindingsByName.get(name);
        if (exact != null) {
            return exact;
        }

        String normalized = stripQuantifierSuffix(name);
        if (normalized.equals(name)) {
            return null;
        }
        return bindingsByName.get(normalized);
    }

    private String stripQuantifierSuffix(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        char last = name.charAt(name.length() - 1);
        return switch (last) {
            case '?', '*', '+' -> name.substring(0, name.length() - 1);
            default -> name;
        };
    }

    private Object unwrapForLegacyUse(List<BoundValue> values) {
        if (values.isEmpty()) {
            return null;
        }

        if (values.size() == 1) {
            return values.getFirst().legacyValue();
        }

        List<Object> legacyValues = new ArrayList<>(values.size());
        for (BoundValue value : values) {
            legacyValues.add(value.legacyValue());
        }
        return legacyValues;
    }
}
