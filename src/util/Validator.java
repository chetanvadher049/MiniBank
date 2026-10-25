package util;

import java.util.regex.Pattern;

public final class Validator {
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}\\d{4}[A-Z]$");
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^[1-9]\\d*$");

    private Validator() {
    }

    public static boolean isValidMobile(String value) {
        return value != null && MOBILE_PATTERN.matcher(value).matches();
    }

    public static boolean isValidEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    public static boolean isValidPan(String value) {
        return value != null && PAN_PATTERN.matcher(value).matches();
    }

    public static boolean isValidIfsc(String value) {
        return value != null && IFSC_PATTERN.matcher(value).matches();
    }

    public static boolean isValidAmount(String value) {
        return value != null && AMOUNT_PATTERN.matcher(value).matches();
    }
}
