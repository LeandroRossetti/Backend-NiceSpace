package com.cowork.backend.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(
        @NotNull(message = "La reserva es obligatoria")
        Long reservationId,

        @NotBlank(message = "El pago es obligatorio")
        String paymentId
) {
}
