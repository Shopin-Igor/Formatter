package org.example.ebnfFormatter.runtime;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class TypeRegistry {

    private final Map<String, TypeSpec> byDslName = new LinkedHashMap<>();
    private final Map<Class<?>, TypeSpec> byJavaType = new LinkedHashMap<>();

    public TypeRegistry() {
        registerDefaults();
    }

    public TypeSpec requireByDslName(String dslName) {
        TypeSpec spec = byDslName.get(dslName);
        if (spec == null) {
            throw new IllegalArgumentException("Unknown DSL type: " + dslName);
        }
        return spec;
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
        Object raw = spec.property(propertyName).get(node);
        return normalizePropertyValue(raw);
    }

    private void registerDefaults() {
        register(ifStmtSpec());
        register(blockStmtSpec());
        register(methodDeclarationSpec());
        register(forStmtSpec());

        register(simpleType("Statement", Statement.class));
        register(simpleType("Expression", Expression.class));
        register(simpleType("Node", Node.class));
    }

    private static TypeSpec simpleType(String dslName, Class<?> type) {
        return new TypeSpec(dslName, type, Map.of());
    }

    private static TypeSpec ifStmtSpec() {
        Map<String, PropertySpec> props = new LinkedHashMap<>();

        props.put("condition", required("condition", o -> ((IfStmt) o).getCondition()));
        props.put("thenStmt", required("thenStmt", o -> ((IfStmt) o).getThenStmt()));
        props.put("thenStatement", required("thenStatement", o -> ((IfStmt) o).getThenStmt()));

        props.put("elseStmt", optional("elseStmt", o -> ((IfStmt) o).getElseStmt()));
        props.put("elseStatement", optional("elseStatement", o -> ((IfStmt) o).getElseStmt()));

        return new TypeSpec("IfStmt", IfStmt.class, props);
    }

    private static TypeSpec blockStmtSpec() {
        Map<String, PropertySpec> props = new LinkedHashMap<>();
        props.put("statements", list("statements", o -> ((BlockStmt) o).getStatements()));
        return new TypeSpec("BlockStmt", BlockStmt.class, props);
    }

    private static TypeSpec methodDeclarationSpec() {
        Map<String, PropertySpec> props = new LinkedHashMap<>();

        props.put("name", required("name", o -> ((MethodDeclaration) o).getNameAsString()));
        props.put("type", required("type", o -> ((MethodDeclaration) o).getType()));
        props.put("parameters", list("parameters", o -> ((MethodDeclaration) o).getParameters()));
        props.put("body", optional("body", o -> ((MethodDeclaration) o).getBody()));
        props.put("modifiers", list("modifiers", o -> ((MethodDeclaration) o).getModifiers()));

        return new TypeSpec("MethodDeclaration", MethodDeclaration.class, props);
    }

    private static TypeSpec forStmtSpec() {
        Map<String, PropertySpec> props = new LinkedHashMap<>();

        props.put("initialization", list("initialization", o -> ((ForStmt) o).getInitialization()));
        props.put("compare", optional("compare", o -> ((ForStmt) o).getCompare()));
        props.put("update", list("update", o -> ((ForStmt) o).getUpdate()));
        props.put("body", required("body", o -> ((ForStmt) o).getBody()));

        return new TypeSpec("ForStmt", ForStmt.class, props);
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

    public TypeSpec get(String dslName) {
        return byDslName.get(dslName);
    }
}