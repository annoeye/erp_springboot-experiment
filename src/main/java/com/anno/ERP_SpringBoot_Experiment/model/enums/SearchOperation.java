package com.anno.ERP_SpringBoot_Experiment.model.enums;

public enum SearchOperation {
    EQUALITY(":"), NEGATION("!"), GREATER_THAN(">"), LESS_THAN("<"), LIKE("~"), STARTS_WITH(""), ENDS_WITH(""), CONTAINS(""), IN("IN");

    private final String symbol;

    SearchOperation(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
