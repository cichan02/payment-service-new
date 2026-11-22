package com.besmart.qrpay.paymentservicenew.infra.network.to;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SouTerminalData {

    private String terminalId;
    private String trxId;
    private String terminalDate;
    private Integer shiftNum;
    private String shiftDate;
    private String cashierId;
    private String cashierFIO;
    private String deviceNo;
}
