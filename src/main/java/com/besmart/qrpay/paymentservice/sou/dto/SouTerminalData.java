package com.besmart.qrpay.paymentservice.sou.dto;

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
