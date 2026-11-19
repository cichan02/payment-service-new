package com.besmart.qrpay.paymentservice.util;

public final class TrxUtils {

    private TrxUtils() {
    }

    /**
     * Stored storno/refund rows carry a {@code -S}/{@code -R} suffix; SOU must receive
     * the original terminal transaction number without it. Only the known suffix is
     * removed: a transaction number may itself contain a hyphen.
     */
    public static String clearTrx(String trx) {
        if (trx == null) {
            return null;
        }
        String trimmed = trx.trim();
        if (trimmed.endsWith("-S") || trimmed.endsWith("-R")) {
            return trimmed.substring(0, trimmed.length() - 2).trim();
        }
        return trimmed;
    }

    public static String withSuffix(String trx, String suffix) {
        return trx.endsWith(suffix) ? trx : trx + suffix;
    }
}
