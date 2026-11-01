package com.cowork.backend.payment.dto;

import com.cowork.backend.payment.Payment;
import com.cowork.backend.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDto(
        Long id,
        Long reservationId,
        String orderId,
        String paymentId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String checkoutUrl,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentDto from(Payment p) {
        return new PaymentDto(
                p.getId(),
                p.getReservation().getId(),
                p.getOrderId(),
                p.getPaymentId(),
                p.getStatus(),
                p.getAmount(),
                p.getCurrency(),
                p.getCheckoutUrl(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
