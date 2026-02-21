package org.example.sqlLikeRequest;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.IfStmt;
import org.example.SqlLikeRequestParserParser;

import java.lang.reflect.Method;
import java.util.Optional;

public final class ExprEvaluator {

    private final TypeRegistry types;
    private final String rootRef;

    public ExprEvaluator(TypeRegistry types, String rootRef) {
        this.types = types;
        this.rootRef = rootRef;
    }

    public boolean eval(SqlLikeRequestParserParser.ExprContext expr, Node root) {
        return evalOr(expr.orExpr(), root);
    }

    private boolean evalOr(SqlLikeRequestParserParser.OrExprContext ctx, Node root) {
        boolean acc = evalAnd(ctx.andExpr(0), root);
        for (int i = 1; i < ctx.andExpr().size(); i++) {
            acc = acc || evalAnd(ctx.andExpr(i), root);
        }
        return acc;
    }

    private boolean evalAnd(SqlLikeRequestParserParser.AndExprContext ctx, Node root) {
        boolean acc = evalNot(ctx.notExpr(0), root);
        for (int i = 1; i < ctx.notExpr().size(); i++) {
            acc = acc && evalNot(ctx.notExpr(i), root);
        }
        return acc;
    }

    private boolean evalNot(SqlLikeRequestParserParser.NotExprContext ctx, Node root) {
        if (ctx.NOT() != null) return !evalNot(ctx.notExpr(), root);
        return evalComparison(ctx.comparisonExpr(), root);
    }

    private boolean evalComparison(SqlLikeRequestParserParser.ComparisonExprContext ctx, Node root) {
        Object left = evalPrimary(ctx.additiveExpr(0).primary(), root);

        if (ctx.compOp() == null) {
            return truthy(left);
        }

        Object right = evalPrimary(ctx.additiveExpr(1).primary(), root);
        String op = ctx.compOp().getText();

        return switch (op) {
            case "==" -> equalsDsl(left, right);
            case "!=" -> !equalsDsl(left, right);
            default -> throw new IllegalArgumentException("Unsupported operator: " + op);
        };
    }

    private Object evalPrimary(SqlLikeRequestParserParser.PrimaryContext ctx, Node root) {
        if (ctx.literal() != null) return evalLiteral(ctx.literal());

        if (ctx.qualifiedName() != null) {
            String text = ctx.qualifiedName().getText(); // IfStmt.thenStmt or BlockStmt

            // Если справа написали тип (BlockStmt) - то помечаем как тип-маркер
            if (types.isKnownType(text))
                return new TypeMarker(text);

            // Иначе это доступ к полю: IfStmt.thenStmt
            String prefix = rootRef + ".";
            if (!text.startsWith(prefix)) {
                throw new IllegalArgumentException("QualifiedName must start with " + prefix + " but got: " + text);
            }
            String path = text.substring(prefix.length()); // thenStmt или a.b.c
            return readPropertyPath(root, path);
        }

        // (expr)
        return eval(ctx.expr(), root);
    }

    private Object evalLiteral(SqlLikeRequestParserParser.LiteralContext lit) {
        if (lit.NULL() != null) return null;
        if (lit.TRUE() != null) return Boolean.TRUE;
        if (lit.FALSE() != null) return Boolean.FALSE;

        if (lit.NUMBER() != null) {
            String t = lit.NUMBER().getText();
            return t.contains(".") ? Double.parseDouble(t) : Long.parseLong(t);
        }

        if (lit.STRING() != null) {
            String raw = lit.STRING().getText(); // включая кавычки
            return raw.substring(1, raw.length() - 1);
        }

        throw new IllegalStateException("Unknown literal: " + lit.getText());
    }

    private boolean equalsDsl(Object left, Object right) {
        left = unwrapOptional(left);
        right = unwrapOptional(right);

        // Сравнение по типу, если справ или слева TypeMarker
        if (right instanceof TypeMarker tm) return isInstanceOf(left, tm.typeName());
        if (left instanceof TypeMarker tm) return isInstanceOf(right, tm.typeName());

        return java.util.Objects.equals(left, right);
    }

    private boolean isInstanceOf(Object value, String typeName) {
        value = unwrapOptional(value);
        if (value == null) return false;
        return value.getClass().getSimpleName().equals(typeName);
    }

    private boolean truthy(Object v) {
        v = unwrapOptional(v);
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        return true;
    }

    private Object unwrapOptional(Object v) {
        if (v instanceof Optional<?> opt) return opt.orElse(null);
        return v;
    }

    // чтение свойств через get...()/is...() + есть поддержка a.b.c

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

        if (obj instanceof IfStmt) {
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

    private record TypeMarker(String typeName) {}
}
