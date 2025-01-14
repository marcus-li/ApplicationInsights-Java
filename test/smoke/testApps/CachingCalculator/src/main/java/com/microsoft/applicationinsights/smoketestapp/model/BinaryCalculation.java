package com.microsoft.applicationinsights.smoketestapp.model;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class BinaryCalculation {
    private static DecimalFormat getDefaultFormat() {
        DecimalFormat fmt = new DecimalFormat();
        fmt.setRoundingMode(RoundingMode.HALF_EVEN);
        fmt.setMaximumFractionDigits(256); // some arbitrarily large number
        fmt.setNegativePrefix("(-");
        fmt.setNegativeSuffix(")");
        fmt.setGroupingUsed(false);
        return fmt;
    }

    private final double leftOperand;
    private final double rightOperand;
    private final BinaryOperator operator;
    private final DecimalFormat format;

    public BinaryCalculation(double leftOperand, double rightOperand, BinaryOperator operator, DecimalFormat format) {
        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;
        this.operator = operator;
        this.format = format;
    }

    public BinaryCalculation(double leftOperand, double rightOperand, BinaryOperator operator) {
        this(leftOperand, rightOperand, operator, getDefaultFormat());
    }

    public DecimalFormat getFormat() {
        return this.format;
    }

    public double getLeftOperand() {
        return leftOperand;
    }

    public String getLeftOperandFormatted() {
        return this.format.format(getLeftOperand());
    }

    public double getRightOperand() {
        return rightOperand;
    }

    public String getRightOperandFormatted() {
        return this.format.format(getRightOperand());
    }

    public BinaryOperator getOperator() {
        return operator;
    }

    public Object getOperatorSymbol() {
        return getOperator().getSymbol();
    }

    public double result() {
        return getOperator().compute(getLeftOperand(), getRightOperand());
    }

    public String resultFormatted() {
        return getFormat().format(result());
    }
}
