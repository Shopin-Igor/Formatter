package org.example.sqlLikeRequest;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;

import java.util.Map;

public final class TypeRegistry {
    private final Map<String, Class<? extends Node>> map = Map.of(
            "IfStmt", IfStmt.class,
            "BlockStmt", BlockStmt.class
    );

    public Class<? extends Node> resolve(String name) {
        Class<? extends Node> cls = map.get(name);
        if (cls == null) throw new IllegalArgumentException("Unknown type: " + name);
        return cls;
    }

    public boolean isKnownType(String name) {
        return map.containsKey(name);
    }
}
