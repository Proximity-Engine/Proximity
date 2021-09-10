package dev.hephaestus.proximity.json;


import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;


public final class JsonPrimitive extends JsonElement {
    private final Object value;

    public JsonPrimitive(boolean bool) {
        value = bool;
    }

    public JsonPrimitive(Number number) {
        value = number;
    }

    public JsonPrimitive(String string) {
        value = string;
    }

    @Override
    public JsonPrimitive deepCopy() {
        return this;
    }

    public boolean isBoolean() {
        return value instanceof Boolean;
    }

    @Override
    public boolean getAsBoolean() {
        if (isBoolean()) {
            return (Boolean) value;
        }
        // Check to see if the value as a String is "true" in any case.
        return Boolean.parseBoolean(getAsString());
    }

    public boolean isNumber() {
        return value instanceof Number;
    }

    @Override
    public Number getAsNumber() {
        return (Number) value;
    }

    public boolean isString() {
        return value instanceof String;
    }

    @Override
    public String getAsString() {
        if (isNumber()) {
            return getAsNumber().toString();
        } else if (isBoolean()) {
            return ((Boolean) value).toString();
        } else {
            return (String) value;
        }
    }

    @Override
    public double getAsDouble() {
        return isNumber() ? getAsNumber().doubleValue() : Double.parseDouble(getAsString());
    }

    @Override
    public BigDecimal getAsBigDecimal() {
        return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
    }

    @Override
    public BigInteger getAsBigInteger() {
        return value instanceof BigInteger ?
                (BigInteger) value : new BigInteger(value.toString());
    }

    @Override
    public float getAsFloat() {
        return isNumber() ? getAsNumber().floatValue() : Float.parseFloat(getAsString());
    }

    @Override
    public long getAsLong() {
        return isNumber() ? getAsNumber().longValue() : Long.parseLong(getAsString());
    }

    @Override
    public short getAsShort() {
        return isNumber() ? getAsNumber().shortValue() : Short.parseShort(getAsString());
    }

    @Override
    protected void write(JsonWriter writer) throws IOException {
        if (this.isString()) {
            writer.value(this.getAsString());
        } else if (this.isBoolean()) {
            writer.value(this.getAsBoolean());
        } else if (this.isNumber()) {
            writer.value(this.getAsNumber());
        }
    }

    @Override
    public int getAsInt() {
        return isNumber() ? getAsNumber().intValue() : Integer.parseInt(getAsString());
    }

    @Override
    public byte getAsByte() {
        return isNumber() ? getAsNumber().byteValue() : Byte.parseByte(getAsString());
    }

    @Override
    public int hashCode() {
        if (value == null) {
            return 31;
        }
        // Using recommended hashing algorithm from Effective Java for longs and doubles
        if (isIntegral(this)) {
            long value = getAsNumber().longValue();
            return (int) (value ^ (value >>> 32));
        }
        if (value instanceof Number) {
            long value = Double.doubleToLongBits(getAsNumber().doubleValue());
            return (int) (value ^ (value >>> 32));
        }
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        JsonPrimitive other = (JsonPrimitive) obj;
        if (value == null) {
            return other.value == null;
        }
        if (isIntegral(this) && isIntegral(other)) {
            return getAsNumber().longValue() == other.getAsNumber().longValue();
        }
        if (value instanceof Number && other.value instanceof Number) {
            double a = getAsNumber().doubleValue();
            // Java standard types other than double return true for two NaN. So, need
            // special handling for double.
            double b = other.getAsNumber().doubleValue();
            return a == b || (Double.isNaN(a) && Double.isNaN(b));
        }
        return value.equals(other.value);
    }

    /**
     * Returns true if the specified number is an integral type
     * (Long, Integer, Short, Byte, BigInteger)
     */
    private static boolean isIntegral(JsonPrimitive primitive) {
        if (primitive.value instanceof Number number) {
            return number instanceof BigInteger || number instanceof Long || number instanceof Integer
                    || number instanceof Short || number instanceof Byte;
        }
        return false;
    }
}
