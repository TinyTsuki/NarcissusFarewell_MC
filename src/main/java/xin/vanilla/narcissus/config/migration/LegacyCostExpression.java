package xin.vanilla.narcissus.config.migration;

import java.util.*;

/** Bounded syntax migration only; the generated Java is evaluated by Banira. */
public final class LegacyCostExpression {
    private static final Set<String> UNARY = new HashSet<>(Arrays.asList("sqrt", "abs", "log", "log10", "exp",
            "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "ceil", "floor", "round",
            "signum", "toRadians", "toDegrees"));
    private static final Set<String> BINARY = new HashSet<>(Arrays.asList("pow", "max", "min", "atan2"));
    private final String source;
    private int cursor, offset, depth, nodes;
    private String token;

    private LegacyCostExpression(String source) {
        if (source == null || source.isEmpty() || source.length() > 8192) throw new IllegalArgumentException("Missing or oversized cost expression");
        this.source = source;
        next();
    }

    public static String toJava(String expression, double num, double rate) {
        if (!Double.isFinite(num) || !Double.isFinite(rate)) throw new IllegalArgumentException("Invalid legacy parameters");
        Node root = parse(expression);
        return "// Parameters captured from the original configuration\n"
                + "final double legacyNum = " + Double.toString(num) + "d;\n"
                + "final double legacyRate = " + Double.toString(rate) + "d;\n"
                + "return " + (root.bool ? "(" + root.java + " ? 1.0d : 0.0d)" : root.java) + ";\n";
    }

    static boolean standard(String expression) {
        List<String> factors = new ArrayList<>();
        if (!factors(parse(expression), factors)) return false;
        Collections.sort(factors);
        return factors.equals(Arrays.asList("distance", "num", "rate"));
    }

    static OptionalDouble constant(String expression, double num, double rate) {
        Node root = parse(expression);
        if (root.op.equals("literal")) return OptionalDouble.of(Double.parseDouble(root.java));
        if (root.op.equals("num")) return OptionalDouble.of(num);
        if (root.op.equals("rate")) return OptionalDouble.of(rate);
        return OptionalDouble.empty();
    }

    private static boolean factors(Node node, List<String> result) {
        if (node.op.equals("*")) return factors(node.children.get(0), result) && factors(node.children.get(1), result);
        if (!Arrays.asList("num", "distance", "rate").contains(node.op)) return false;
        result.add(node.op); return true;
    }

    private static Node parse(String source) {
        LegacyCostExpression parser = new LegacyCostExpression(source);
        Node root = parser.expression(1);
        parser.expect("");
        return root;
    }

    private Node expression(int minimum) {
        if (++depth > 64) fail("Expression nesting exceeds 64");
        Node left = prefix();
        while (precedence(token) >= minimum) {
            String op = token; int priority = precedence(op); next();
            Node right = expression(op.equals("^") ? priority : priority + 1);
            boolean logical = op.equals("&&") || op.equals("||");
            boolean comparison = Arrays.asList("==", "!=", "<", ">", "<=", ">=").contains(op);
            if (logical) { requireBool(left); requireBool(right); }
            else if (comparison && (op.equals("==") || op.equals("!="))) {
                if (left.bool != right.bool) fail("Comparison requires matching types");
            } else { requireNumber(left); requireNumber(right); }
            left = node(op, op.equals("^") ? "Math.pow(" + left.java + ", " + right.java + ")"
                    : "(" + left.java + " " + op + " " + right.java + ")", logical || comparison, left, right);
        }
        if (minimum == 1 && take("?")) {
            requireBool(left);
            Node yes = expression(1); expect(":"); Node no = expression(1);
            if (yes.bool != no.bool) fail("Conditional branches require matching types");
            left = node("?:", "(" + left.java + " ? " + yes.java + " : " + no.java + ")", yes.bool, left, yes, no);
        }
        depth--;
        return left;
    }

    private Node prefix() {
        String value = token; next();
        if (value.equals("(") ) { Node inside = expression(1); expect(")"); return inside; }
        if (value.equals("-") || value.equals("+") || value.equals("!")) {
            Node inside = expression(8);
            if (value.equals("!")) requireBool(inside); else requireNumber(inside);
            return node(value, "(" + value + inside.java + ")", inside.bool, inside);
        }
        if (value.equals("true") || value.equals("false")) return node(value, value, true);
        if (value.equals("Math")) { expect("."); value = token; next(); }
        if (UNARY.contains(value) || BINARY.contains(value) || value.equals("random")) {
            expect("("); List<Node> args = new ArrayList<>();
            if (!take(")")) { do { Node arg = expression(1); requireNumber(arg); args.add(arg); } while (take(",")); expect(")"); }
            int count = args.size();
            if (value.equals("random") ? count != 0 && count != 2 : count != (BINARY.contains(value) ? 2 : 1)) fail("Wrong argument count for " + value);
            String java;
            if (value.equals("random") && count == 2) {
                java = "randomRange(" + args.get(0).java + ", " + args.get(1).java + ")";
            } else {
                StringJoiner join = new StringJoiner(", "); for (Node arg : args) join.add(arg.java);
                java = "Math." + value + "(" + join + ")";
            }
            return node(value, java, false, args.toArray(new Node[0]));
        }
        if (value.equals("num") || value.equals("rate") || value.equals("distance")) {
            return node(value, value.equals("distance") ? "distance" : value.equals("num") ? "legacyNum" : "legacyRate", false);
        }
        if (!value.isEmpty() && (Character.isDigit(value.charAt(0)) || value.charAt(0) == '.')) {
            try { if (!Double.isFinite(Double.parseDouble(value))) fail("Non-finite literal"); }
            catch (NumberFormatException error) { fail("Invalid numeric literal"); }
            return node("literal", value, false);
        }
        fail("Unsupported identifier or token: " + value); return null;
    }

    static String helpers() {
        return "private static double randomRange(double a, double b) { "
                + "double min = Math.min(a, b); return min + Math.random() * (Math.max(a, b) - min); }\n";
    }

    private Node node(String op, String java, boolean bool, Node... children) {
        if (++nodes > 1024) fail("Expression exceeds 1024 nodes");
        return new Node(op, java, bool, Arrays.asList(children));
    }

    private void next() {
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) cursor++;
        offset = cursor;
        if (cursor == source.length()) { token = ""; return; }
        char c = source.charAt(cursor++);
        if (Character.isLetter(c) || c == '_') {
            while (cursor < source.length() && (Character.isLetterOrDigit(source.charAt(cursor)) || source.charAt(cursor) == '_')) cursor++;
        } else if (Character.isDigit(c) || c == '.' && cursor < source.length() && Character.isDigit(source.charAt(cursor))) {
            while (cursor < source.length() && Character.isDigit(source.charAt(cursor))) cursor++;
            if (c != '.' && cursor < source.length() && source.charAt(cursor) == '.') {
                cursor++; while (cursor < source.length() && Character.isDigit(source.charAt(cursor))) cursor++;
            }
            if (cursor < source.length() && (source.charAt(cursor) == 'e' || source.charAt(cursor) == 'E')) {
                cursor++; if (cursor < source.length() && (source.charAt(cursor) == '+' || source.charAt(cursor) == '-')) cursor++;
                int start = cursor; while (cursor < source.length() && Character.isDigit(source.charAt(cursor))) cursor++;
                if (start == cursor) fail("Missing exponent");
            }
        } else if (cursor < source.length() && Arrays.asList("&&", "||", "==", "!=", "<=", ">=").contains(source.substring(offset, cursor + 1))) {
            cursor++;
        } else if ("()+-*/%^!,<>?:.".indexOf(c) < 0) fail("Unsupported character");
        token = source.substring(offset, cursor);
    }

    private boolean take(String value) { if (!token.equals(value)) return false; next(); return true; }
    private void expect(String value) { if (!take(value)) fail("Expected " + (value.isEmpty() ? "end" : value)); }
    private void requireBool(Node node) { if (!node.bool) fail("Boolean operand required"); }
    private void requireNumber(Node node) { if (node.bool) fail("Numeric operand required"); }
    private void fail(String message) { throw new IllegalArgumentException(message + " at offset " + offset); }
    private static int precedence(String op) {
        switch (op) {
            case "||": return 1; case "&&": return 2;
            case "==": case "!=": return 3;
            case "<": case ">": case "<=": case ">=": return 4;
            case "+": case "-": return 5;
            case "*": case "/": case "%": return 6;
            case "^": return 8; default: return 0;
        }
    }

    private static final class Node {
        final String op, java;
        final boolean bool;
        final List<Node> children;
        Node(String op, String java, boolean bool, List<Node> children) {
            this.op = op; this.java = java; this.bool = bool; this.children = children;
        }
    }
}
