package com.salesmanager.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Fmt {

    private Fmt() {
    }

    public static String money(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String nowTime() {
        return new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
    }

    /** Returns NaN when the text is not a valid number. */
    public static double parseDouble(String text) {
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /** Returns -1 when the text is not a valid integer. */
    public static int parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
