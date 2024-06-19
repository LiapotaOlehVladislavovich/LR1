package ua.nure.calculator.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CalculatorService {

    private final Map<String, String> variables = new HashMap<>();
    private static final Map<String, Integer> precedence = new HashMap<>();
    static {
        precedence.put("+", 1);
        precedence.put("-", 1);
        precedence.put("–", 1);
        precedence.put("*", 2);
        precedence.put("/", 2);
        precedence.put("^", 3);
    }

    public String calculate(String expression) {
        String[] lines = expression.split("\\n");
        for (String line : lines) {
            if (line.matches("^\\s*[a-zA-Z]\\s*=\\s*[a-zA-Z0-9.()+\\-–*/^ 0-9a-fA-Fxbo]+;?$")) {
                String[] parts = line.split("=");
                String varName = parts[0].trim();
                String varValue = parts[1].trim();
                variables.put(varName, evaluateExpression(varValue));
            } else if (line.matches("^\\s*\\?\\s*[a-zA-Z]\\s*$")) {
                String varName = line.trim().substring(1).trim();
                if (variables.containsKey(varName)) {
                    return evaluateExpression(variables.get(varName));
                }
            } else if (line.matches("^[a-zA-Z0-9.()+\\-–*/^ 0-9a-fA-Fxbo]+=$")) {
                return evaluateExpression(line.substring(0, line.length() - 1));
            }
        }

        return "Invalid input";
    }

    private String evaluateExpression(String expression) {
        try {
            Queue<String> infixExpression = infixToPostfix(expression);

            Stack<String> stack = new Stack<>();

            for (String i : infixExpression) {
                if (precedence.containsKey(i)) {
                    String b = stack.pop();
                    String a = stack.pop();

                    if (!b.matches("-?(0x[0-9a-fA-F]+|0b[01]+|0o[0-7]+|\\d+(\\.\\d+)?)")
                            || !a.matches("-?(0x[0-9a-fA-F]+|0b[01]+|0o[0-7]+|\\d+(\\.\\d+)?)")) {
                        stack.push("(" + a + i + b + ")");
                    } else {
                        double numB = parseNumber(b);
                        double numA = parseNumber(a);

                        switch (i) {
                            case "+":
                                stack.push(String.valueOf(numA + numB));
                                break;
                            case "-", "–":
                                stack.push(String.valueOf(numA - numB));
                                break;
                            case "*":
                                stack.push(String.valueOf(numA * numB));
                                break;
                            case "/":
                                stack.push(String.valueOf(numA / numB));
                                break;
                            case "^":
                                stack.push(String.valueOf(Math.pow(numA, numB)));
                                break;
                        }
                    }
                } else {
                    stack.push(i);
                }
            }

            return stack.pop();
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private double parseNumber(String number) {
        if (number.startsWith("0x")) {
            return Integer.parseInt(number.substring(2), 16);
        } else if (number.startsWith("0b")) {
            return Integer.parseInt(number.substring(2), 2);
        } else if (number.startsWith("0o")) {
            return Integer.parseInt(number.substring(2), 8);
        } else {
            return Double.parseDouble(number);
        }
    }

    private Queue<String> infixToPostfix(String expression) throws IllegalArgumentException {
        String[] tokens = expression.split("(?<=[-+*/^()\\u2013])|(?=[-+*/^()\\u2013])|(?<=\\b)(?=\\b)\n");
        Queue<String> output = new ArrayDeque<>();
        Stack<String> operators = new Stack<>();
        boolean lastTokenWasOperator = true;

        for (String t : tokens) {
            String fixedToken = t.trim();

            if (fixedToken.matches(".*\\b0\\d+\\b.*")) {
                throw new IllegalArgumentException("Leading zeros are not allowed in numbers");
            }

            if (!fixedToken.isEmpty() && fixedToken.charAt(fixedToken.length() - 1) == ';') {
                fixedToken = fixedToken.substring(0, fixedToken.length() - 1);
            }

            if (fixedToken.matches("^\\s*-?\\d+(\\.\\d+)?[a-zA-Z]\\s*$")) {
                output.offer(fixedToken.substring(0, fixedToken.length() - 1));

                while (!operators.isEmpty() && precedence.getOrDefault(operators.peek(), 0) >= precedence.get("*")) {
                    output.offer(operators.pop());
                }

                operators.push("*");
                String letter = fixedToken.substring(fixedToken.length() - 1);
                output.offer(variables.containsKey(letter) ? evaluateExpression(variables.get(letter)) : letter);
                lastTokenWasOperator = false;
            } else if (fixedToken.matches("^-?\\d+$") || fixedToken.matches("^-?\\d*\\.\\d+$") ||
                    fixedToken.matches("0x[0-9a-fA-F]+") || fixedToken.matches("0b[01]+") || fixedToken.matches("0o[0-7]+")) {
                output.offer(fixedToken);
                lastTokenWasOperator = false;
            } else if (fixedToken.matches("^[a-zA-Z]$")) {
                output.offer(variables.containsKey(fixedToken) ? evaluateExpression(variables.get(fixedToken)) : fixedToken);
                lastTokenWasOperator = false;
            } else if (fixedToken.equals("(")) {
                operators.push(fixedToken);
                lastTokenWasOperator = true;
            } else if (fixedToken.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.offer(operators.pop());
                }

                operators.pop();
                lastTokenWasOperator = false;
            } else if (precedence.containsKey(fixedToken)) {
                if (fixedToken.equals("-") && lastTokenWasOperator) {
                    output.offer("-1");
                    fixedToken = "*";
                }

                while (!operators.isEmpty() && precedence.getOrDefault(operators.peek(), 0) >= precedence.get(fixedToken)) {
                    output.offer(operators.pop());
                }

                operators.push(fixedToken);
                lastTokenWasOperator = true;
            }
        }

        while (!operators.isEmpty()) {
            output.offer(operators.pop());
        }

        return output;
    }
}