package org.example.sqlLikeRequest;

import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import org.example.SqlLikeRequestParserParser;

import java.lang.reflect.Method;
import java.util.Optional;



public class OutputRenderer {
    private static final PrettyPrinterConfiguration PP = new PrettyPrinterConfiguration()
            .setIndentSize(2)
            .setPrintComments(false)
            .setEndOfLineCharacter("\n");

    private final String rootRef;

    OutputRenderer(String rootRef) {
        this.rootRef = rootRef;
    }

    String render(SqlLikeRequestParserParser.FormatStringContext fmt, Node root) {
        StringBuilder out = new StringBuilder();

        for (SqlLikeRequestParserParser.FormatPartContext part : fmt.formatPart()) {
            if (part.TEXT() != null) {
                out.append(part.TEXT().getText());
            } else if (part.PLACEHOLDER() != null) {
                String ph = part.PLACEHOLDER().getText();
                out.append(resolvePlaceholder(ph, root));
            }
        }

        String result = out.toString();
        result = result.replaceAll("\\belse\\b\\s*(?=\\s*$)", "");
        result = result.replaceAll("\\s+else\\s+$", " ");

        return toSingleLine(result);
    }

    private String toSingleLine(String s) {
        s = s.replace("\r\n", "\n").replace('\r', '\n').replace('\n', ' ');

        s = s.replaceAll("\\s+", " ").trim();

        s = s.replaceAll("\\s*\\(\\s*", "(")
                .replaceAll("\\s*\\)\\s*", ")")
                .replaceAll("\\s*\\{\\s*", "{")
                .replaceAll("\\s*\\}\\s*", "}");

        return s;
    }

    private String resolvePlaceholder(String placeholder, Node root) {
        if (placeholder.equals(rootRef)) {
            return root.toString(PP);
        }

        String prefix = rootRef + ".";
        if (!placeholder.startsWith(prefix)) {
            throw new IllegalArgumentException(
                    "Placeholder must start with " + prefix + " but got: " + placeholder
            );
        }

        String path = placeholder.substring(prefix.length()); // condition / thenStatement / elseStatement и тд и тп
        Object value = readPropertyPath(root, path);
        value = unwrapOptional(value);

        if (value == null) return "";

        if (value instanceof Node n) {
            return n.toString(PP);
        }

        return value.toString();
    }

    private Object unwrapOptional(Object v) {
        if (v instanceof Optional<?> opt) return opt.orElse(null);
        return v;
    }

    private Object readPropertyPath(Object root, String path) {
        Object cur = root;
        for (String part : path.split("\\.")) {
            cur = readOneProperty(cur, part);
            cur = unwrapOptional(cur);
            if (cur == null) return null;
        }
        return cur;
    }

    private Object readOneProperty(Object obj, String prop) {
        if (obj == null) return null;


        if (obj.getClass().getSimpleName().equals("IfStmt")) {
            prop = switch (prop) {
                case "thenStatement" -> "thenStmt";
                case "elseStatement" -> "elseStmt";
                default -> prop;
            };
        }

        String getter = "get" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1);

        try {
            Method m = obj.getClass().getMethod(getter);
            return m.invoke(obj);
        } catch (NoSuchMethodException e) {
            String isGetter = "is" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
            try {
                Method m = obj.getClass().getMethod(isGetter);
                return m.invoke(obj);
            } catch (Exception ex) {
                throw new IllegalArgumentException("No getter for '" + prop + "' on " + obj.getClass().getName(), ex);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to call getter '" + getter + "' on " + obj.getClass().getName(), e);
        }
    }
}
