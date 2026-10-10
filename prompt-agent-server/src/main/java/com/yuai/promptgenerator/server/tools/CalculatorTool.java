package com.yuai.promptgenerator.server.tools;

import java.math.BigDecimal;
import java.math.MathContext;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class CalculatorTool {
    @Tool(description = "Evaluate an arithmetic expression containing numbers, parentheses, +, -, *, and /")
    public String calculate(@ToolParam(description = "Arithmetic expression") String expression) {
        try {
            if (expression == null || expression.length() > 200) return "Expression is empty or too long";
            Parser parser = new Parser(expression);
            BigDecimal result = parser.expression();
            parser.skipSpaces();
            if (!parser.end()) throw new IllegalArgumentException("Unexpected input");
            return result.stripTrailingZeros().toPlainString();
        }
        catch (RuntimeException exception) {
            return "Invalid expression: " + exception.getMessage();
        }
    }

    private static final class Parser {
        private final String value;
        private int offset;
        private Parser(String value) { this.value = value; }
        private BigDecimal expression() {
            BigDecimal result = term();
            while (true) {
                skipSpaces();
                if (take('+')) result = result.add(term());
                else if (take('-')) result = result.subtract(term());
                else return result;
            }
        }
        private BigDecimal term() {
            BigDecimal result = factor();
            while (true) {
                skipSpaces();
                if (take('*')) result = result.multiply(factor());
                else if (take('/')) result = result.divide(factor(), MathContext.DECIMAL128);
                else return result;
            }
        }
        private BigDecimal factor() {
            skipSpaces();
            if (take('-')) return factor().negate();
            if (take('(')) {
                BigDecimal result = expression();
                skipSpaces();
                if (!take(')')) throw new IllegalArgumentException("Missing )");
                return result;
            }
            int start = offset;
            while (!end() && (Character.isDigit(value.charAt(offset)) || value.charAt(offset) == '.')) offset++;
            if (start == offset) throw new IllegalArgumentException("Number expected");
            return new BigDecimal(value.substring(start, offset));
        }
        private boolean take(char expected) {
            if (!end() && value.charAt(offset) == expected) { offset++; return true; }
            return false;
        }
        private void skipSpaces() { while (!end() && Character.isWhitespace(value.charAt(offset))) offset++; }
        private boolean end() { return offset >= value.length(); }
    }
}
