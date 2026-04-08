package com.bofalgan.pharmacy.util;

import com.bofalgan.pharmacy.config.AppConfig;
import java.text.DecimalFormat;

public class CurrencyFormatter {

    private static final DecimalFormat FMT = new DecimalFormat("#,##0.00");

    public static String format(double amount) {
        return AppConfig.CURRENCY_SYMBOL + FMT.format(amount);
    }

    public static String formatNoSymbol(double amount) {
        return FMT.format(amount);
    }

    public static double parse(String text) {
        if (text == null || text.isBlank()) return 0.0;
        try {
            return Double.parseDouble(text.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private CurrencyFormatter() {}
}
