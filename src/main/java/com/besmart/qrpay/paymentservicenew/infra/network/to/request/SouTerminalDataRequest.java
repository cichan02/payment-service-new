package com.besmart.qrpay.paymentservicenew.infra.network.to.request;

public record SouTerminalDataRequest(
        String terminalId,
        String trxId,
        String terminalDate,
        Integer shiftNum,
        String shiftDate,
        String cashierId,
        String cashierFIO,
        String deviceNo
) {
}
