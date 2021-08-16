package se.kry.codetest.model;

public enum ServiceStatusValueEnum {
    OK,
    FAIL,
    UNKNOWN;

    public static ServiceStatusValueEnum valueOfOrDefault(String name) {
        ServiceStatusValueEnum defaultValue = ServiceStatusValueEnum.UNKNOWN;

        if (null == name) return defaultValue;

        try {
            return valueOf(name);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}
