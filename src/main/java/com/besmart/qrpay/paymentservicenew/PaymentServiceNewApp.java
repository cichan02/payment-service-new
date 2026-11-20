package com.besmart.qrpay.paymentservicenew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class PaymentServiceNewApp {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceNewApp.class, args);
    }
}
