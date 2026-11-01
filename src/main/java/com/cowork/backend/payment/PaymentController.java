package com.cowork.backend.payment;

import com.cowork.backend.payment.dto.ConfirmPaymentRequest;
import com.cowork.backend.payment.dto.PaymentDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/reservations/{id}/pay")
    public PaymentDto pay(@PathVariable Long id) {
        return paymentService.pay(id);
    }

    @PostMapping("/payments/confirm")
    public PaymentDto confirm(@Valid @RequestBody ConfirmPaymentRequest request) {
        return paymentService.confirm(request);
    }
}
