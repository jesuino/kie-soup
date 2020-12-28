package org.dashbuilder.dataprovider.prometheus.client;

public enum ResultType {

    VECTOR,
    MATRIX,
    SCALAR,
    STRING;

    public static ResultType of(String name) {
        return ResultType.valueOf(name.toUpperCase());
    }
}
