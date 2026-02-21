package org.example.sqlLikeRequest;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;

public record Match(String nodeType, Range range, String code) {
    private static final PrettyPrinterConfiguration PRETTY_PRINTER_CONFIGURATION = new PrettyPrinterConfiguration()
            .setIndentSize(2)
            .setPrintComments(false)
            .setEndOfLineCharacter("\n");

    public static Match from(Node node, String code) {
        Range r = node.getRange().orElse(new Range(new Position(0, 0), new Position(0, 0)));
        return new Match(node.getClass().getSimpleName(), r, code);
    }

    public static Match from(Node node) {
        return from(node, node.toString(PRETTY_PRINTER_CONFIGURATION));
    }


    @Override
    public String toString() {  //  для будущих тестов
        return nodeType + " @ " + range.begin.line + ":" + range.begin.column + " -> " + range.end.line + ":" + range.end.column;
    }
}

