package org.example.sqlLikeRequest;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;

import java.lang.reflect.Method;
import java.util.Map;

public final class TypeRegistry {
    private final Map<String, Class<? extends Node>> javaTypes = Map.of(
            "IfStmt", IfStmt.class,
            "BlockStmt", BlockStmt.class
    );

//    private final Map<Class<?>, Map<String, String>> dslGetters = Map.of(
//            IfStmt.class, Map.of(
//                    "condition", "getCondition",
//                    "thenStatement", "getThenStmt",
//                    "elseStatement", "getElseStmt",
//                    "thenStmt", "getThenStmt",
//                    "elseStmt", "getElseStmt"
//            )
//    );
//
//    public Method resolvePropertyGetter(Class<?> ownerType, String dslProperty) {
//        String getter = dslGetters.getOrDefault(ownerType, Map.of()).get(dslProperty);
//        if (getter == null) return null;
//        try {
//            return ownerType.getMethod(getter);
//        } catch (NoSuchMethodException e) {
//            throw new IllegalStateException(
//                    "Mapped getter not found: " + ownerType.getName() + "." + getter + "()", e
//            );
//        }
//    }

    public Class<? extends Node> resolve(String name) {
        Class<? extends Node> cls = javaTypes.get(name);
        if (cls == null) throw new IllegalArgumentException("Unknown type: " + name);
        return cls;
    }

    public boolean isKnownType(String name) {
        return javaTypes.containsKey(name);
    }
}
