package org.example.ebnfFormatter.match;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Bindings {

    private final Map<String, Object> bindingsByName = new LinkedHashMap<>();

    public boolean bind(String name, Object value) {
        if (!bindingsByName.containsKey(name)) {
            bindingsByName.put(name, value);
            return true;
        }
        return Objects.equals(bindingsByName.get(name), value);
    }

    public Bindings copy() {
        Bindings copy = new Bindings();
        copy.bindingsByName.putAll(this.bindingsByName);
        return copy;
    }

    public void replaceWith(Bindings other) {
        bindingsByName.clear();
        bindingsByName.putAll(other.bindingsByName);
    }

    public Object getRequired(String name) {
        if (!bindingsByName.containsKey(name)) {
            throw new IllegalArgumentException("No binding for name: " + name);
        }
        return bindingsByName.get(name);
    }

    public Object find(String name) {
        return bindingsByName.get(name);
    }

    public boolean hasBinding(String name) {
        return bindingsByName.containsKey(name);
    }

    public Set<String> getBindingNames() {
        return Collections.unmodifiableSet(bindingsByName.keySet());
    }

    public Map<String, Object> asUnmodifiableMap() {
        return Collections.unmodifiableMap(bindingsByName);
    }

    @Override
    public String toString() {
        return bindingsByName.toString();
    }
}