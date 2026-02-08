package org.example.sqlLikeRequest;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;

public record Match(String nodeType, Range range, String code) {

    public static Match from(Node node) {
        Range r = node.getRange().orElse(new Range(new Position(0, 0), new Position(0, 0)));
        return new Match(node.getClass().getSimpleName(), r, node.toString());
    }

    @Override
    public String toString() {  //  для будующих тестов
        return nodeType + " @ " + range.begin.line + ":" + range.begin.column + " -> " + range.end.line + ":" + range.end.column;
    }
}

