package org.example.ebnfFormatter.runtime;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class TypeRegistryUniversal {
    private static final List<String> AST_PACKAGES = List.of(
            "com.github.javaparser.ast",
            "com.github.javaparser.ast.body",
            "com.github.javaparser.ast.comments",
            "com.github.javaparser.ast.expr",
            "com.github.javaparser.ast.modules",
            "com.github.javaparser.ast.stmt",
            "com.github.javaparser.ast.type"
    );

    private final Map<String, TypeSpec> byDslName = new LinkedHashMap<>();
    private final Map<Class<?>, TypeSpec> byJavaType = new LinkedHashMap<>();

    public TypeRegistryUniversal() {
        registerAbstractAliases();
    }

    private void registerAbstractAliases() {
        register(simpleType("Node", Node.class));
        register(simpleType("Statement", Statement.class));
        register(simpleType("Expression", Expression.class));
    }

    public TypeSpec requireByDslName(String dslName) {
        TypeSpec existing = byDslName.get(dslName);
        if (existing != null) {
            return existing;
        }

        Class<?> javaType = resolveJavaParserType(dslName);
        if (javaType == null) {
            throw new IllegalArgumentException("Unknown DSL type: " + dslName);
        }

        TypeSpec generated = buildTypeSpec(javaType);
        register(generated);
        return generated;
    }

    public TypeSpec findByJavaType(Class<?> javaType) {
        TypeSpec exact = byJavaType.get(javaType);
        if (exact != null) {
            return exact;
        }

        for (Map.Entry<Class<?>, TypeSpec> entry : byJavaType.entrySet()) {
            if (entry.getKey().isAssignableFrom(javaType)) {
                return entry.getValue();
            }
        }

        if (Node.class.isAssignableFrom(javaType)) {
            TypeSpec generated = buildTypeSpec(javaType);
            register(generated);
            return generated;
        }

        return null;
    }

    public void register(TypeSpec spec) {
        TypeSpec prevDsl = byDslName.put(spec.dslName(), spec);
        TypeSpec prevJava = byJavaType.put(spec.javaType(), spec);

        if (prevDsl != null && prevDsl != spec) {
            throw new IllegalStateException("Duplicate DSL type: " + spec.dslName());
        }
        if (prevJava != null && prevJava != spec) {
            throw new IllegalStateException("Duplicate Java type: " + spec.javaType().getName());
        }
    }

    public Object readProperty(Node node, String propertyName) {
        TypeSpec spec = findByJavaType(node.getClass());
        if (spec == null) {
            throw new IllegalArgumentException("No TypeSpec registered for " + node.getClass().getName());
        }

        PropertySpec property = spec.property(propertyName);

        Object raw = property.get(node);
        return normalizePropertyValue(raw);
    }

    public TypeSpec get(String dslName) {
        return byDslName.get(dslName);
    }

    private Class<?> resolveJavaParserType(String dslName) {
        for (String pkg : AST_PACKAGES) {
            try {
                Class<?> cls = Class.forName(pkg + "." + dslName);
                if (Node.class.isAssignableFrom(cls)) {
                    return cls;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private TypeSpec buildTypeSpec(Class<?> javaType) {
        Class<? extends Node> nodeType = javaType.asSubclass(Node.class);
        return new TypeSpec(
                nodeType.getSimpleName(),
                nodeType,
                buildProperties(nodeType)
        );
    }

    private Map<String, PropertySpec> buildProperties(Class<? extends Node> nodeType) {
        Map<String, PropertySpec> props = new LinkedHashMap<>();

        for (Method method : nodeType.getMethods()) {
            if (!isGetter(method)) {
                continue;
            }

            String name = getterToPropertyName(method);
            if (isIgnoredProperty(name)) {
                continue;
            }

            Class<?> returnType = method.getReturnType();

            Function<Object, Object> getter = node -> {
                try {
                    return method.invoke(node);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(
                            "Cannot read property '" + name + "' from " + nodeType.getName(), e
                    );
                }
            };

            PropertySpec propertySpec;
            if (Optional.class.isAssignableFrom(returnType)) {
                propertySpec = optional(name, getter);
            } else if (NodeList.class.isAssignableFrom(returnType)) {
                propertySpec = list(name, getter);
            } else {
                propertySpec = required(name, getter);
            }

            props.putIfAbsent(name, propertySpec);
        }

        return props;
    }

    private boolean isGetter(Method method) {
        if (Modifier.isStatic(method.getModifiers())) return false;
        if (method.getParameterCount() != 0) return false;
        if (method.getReturnType() == void.class) return false;
        if (method.getDeclaringClass() == Object.class) return false;

        String name = method.getName();
        return (name.startsWith("get") && name.length() > 3)
                || (name.startsWith("is") && name.length() > 2);
    }

    private String getterToPropertyName(Method method) {
        String name = method.getName();

        if (name.startsWith("get")) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        if (name.startsWith("is")) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }

        throw new IllegalArgumentException("Not a getter: " + method.getName());
    }

    private boolean isIgnoredProperty(String name) {
        return switch (name) {
            case "metaModel",
                 "range",
                 "tokenRange",
                 "parsed",
                 "comment",
                 "orphanComments",
                 "allContainedComments",
                 "childNodes",
                 "parentNode" -> true;
            default -> false;
        };
    }

    private static TypeSpec simpleType(String dslName, Class<?> type) {
        return new TypeSpec(dslName, type, Map.of());
    }

    private static PropertySpec required(String name, Function<Object, Object> getter) {
        return new PropertySpec(name, getter, false, false);
    }

    private static PropertySpec optional(String name, Function<Object, Object> getter) {
        return new PropertySpec(name, getter, true, false);
    }

    private static PropertySpec list(String name, Function<Object, Object> getter) {
        return new PropertySpec(name, getter, false, true);
    }

    public static Object normalizePropertyValue(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        if (value instanceof NodeList<?> nodeList) {
            return nodeList;
        }
        return value;
    }
}