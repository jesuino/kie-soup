package org.dashbuilder.dataprovider.prometheus.client;

public class Value {

    private long timestamp;
    private String value;

    public static Value of(long timestamp, String sampleValue) {
        Value value = new Value();
        value.setTimestamp(timestamp);
        value.setValue(sampleValue);
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Value [timestamp=" + timestamp + ", value=" + value + "]";
    }

}
