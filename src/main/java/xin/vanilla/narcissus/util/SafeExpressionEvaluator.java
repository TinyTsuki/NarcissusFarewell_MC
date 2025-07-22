package xin.vanilla.narcissus.util;

import java.util.*;

public class SafeExpressionEvaluator {

    private static final Set<String> FUNCTIONS = new HashSet<>(Arrays.asList(
            "sqrt", "pow", "log", "sin", "cos", "abs", "random"
    ));

    private final String expression;

    public SafeExpressionEvaluator(String expression) {
        this.expression = expression;
    }

    public double evaluate(Map<String, Double> variables) {
        return parseExpression(expression.replaceAll("\\s+", ""), variables);
    }

    private double parseExpression(String expr, Map<String, Double> vars) {
        Deque<Double> numbers = new ArrayDeque<>();
        Deque<Character> operators = new ArrayDeque<>();

        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    sb.append(expr.charAt(i++));
                }
                numbers.push(Double.parseDouble(sb.toString()));
                continue;
            }

            if (Character.isLetter(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < expr.length() && Character.isLetter(expr.charAt(i))) {
                    sb.append(expr.charAt(i++));
                }
                String name = sb.toString();

                if (vars.containsKey(name)) {
                    numbers.push(vars.get(name));
                } else if (FUNCTIONS.contains(name)) {
                    if (i >= expr.length() || expr.charAt(i) != '(') {
                        throw new RuntimeException("Function " + name + " must be followed by '('");
                    }
                    i++; // skip '('
                    int start = i, depth = 1;
                    while (i < expr.length() && depth > 0) {
                        if (expr.charAt(i) == '(') depth++;
                        else if (expr.charAt(i) == ')') depth--;
                        i++;
                    }
                    if (depth != 0) throw new RuntimeException("Mismatched parentheses in function " + name);

                    String argExpr = expr.substring(start, i - 1);
                    List<Double> args = splitArguments(argExpr, vars);
                    numbers.push(applyFunction(name, args));
                } else {
                    throw new RuntimeException("Unknown identifier: " + name);
                }
                continue;
            }

            if (c == '(') {
                operators.push(c);
                i++;
            } else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    applyOperator(numbers, operators.pop());
                }
                if (operators.isEmpty() || operators.pop() != '(') {
                    throw new RuntimeException("Mismatched parentheses");
                }
                i++;
            } else if ("+-*/^".indexOf(c) >= 0) {
                while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(c)) {
                    applyOperator(numbers, operators.pop());
                }
                operators.push(c);
                i++;
            } else {
                throw new RuntimeException("Unexpected character: " + c);
            }
        }

        while (!operators.isEmpty()) {
            applyOperator(numbers, operators.pop());
        }

        if (numbers.size() != 1) {
            throw new RuntimeException("Invalid expression");
        }

        return numbers.pop();
    }

    private int precedence(char op) {
        if (op == '+' || op == '-') return 1;
        if (op == '*' || op == '/') return 2;
        if (op == '^') return 3;
        return 0;
    }

    private void applyOperator(Deque<Double> numbers, char op) {
        double b = numbers.pop();
        double a = numbers.pop();
        switch (op) {
            case '+':
                numbers.push(a + b);
                break;
            case '-':
                numbers.push(a - b);
                break;
            case '*':
                numbers.push(a * b);
                break;
            case '/':
                numbers.push(a / b);
                break;
            case '^':
                numbers.push(Math.pow(a, b));
                break;
            default:
                throw new RuntimeException("Unknown operator: " + op);
        }
    }

    private List<Double> splitArguments(String expr, Map<String, Double> vars) {
        List<Double> args = new ArrayList<>();
        int depth = 0;
        int lastSplit = 0;

        for (int i = 0; i <= expr.length(); i++) {
            char c = i < expr.length() ? expr.charAt(i) : ',';
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if ((c == ',' || i == expr.length()) && depth == 0) {
                String arg = expr.substring(lastSplit, i);
                args.add(parseExpression(arg, vars));
                lastSplit = i + 1;
            }
        }
        return args;
    }

    private double applyFunction(String name, List<Double> args) {
        if ("sqrt".equals(name)) return Math.sqrt(args.get(0));
        if ("pow".equals(name)) return Math.pow(args.get(0), args.get(1));
        if ("log".equals(name)) return Math.log(args.get(0));
        if ("sin".equals(name)) return Math.sin(args.get(0));
        if ("cos".equals(name)) return Math.cos(args.get(0));
        if ("abs".equals(name)) return Math.abs(args.get(0));
        if ("random".equals(name)) {
            if (args.size() != 2) throw new RuntimeException("random(start, end) need 2 arguments");
            double min = args.get(0);
            double max = args.get(1);
            if (min > max) {
                double tmp = min;
                min = max;
                max = tmp;
            }
            return min + (Math.random() * (max - min));
        }
        throw new RuntimeException("Unsupported function: " + name);
    }
}
