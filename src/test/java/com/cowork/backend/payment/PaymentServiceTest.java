package com.cowork.backend.payment;

import com.cowork.backend.mail.MailService;
import com.cowork.backend.payment.dto.ConfirmPaymentRequest;
import com.cowork.backend.payment.mercadopago.MercadoPagoClient;
import com.cowork.backend.reservation.Reservation;
import com.cowork.backend.reservation.ReservationRepository;
import com.cowork.backend.reservation.ReservationStatus;
import com.cowork.backend.security.AuthHelper;
import com.cowork.backend.team.TeamMemberRepository;
import com.cowork.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private AuthHelper authHelper;

    @Mock
    private MercadoPagoClient mercadoPagoClient;

    @Mock
    private MailService mailService;

    private PaymentService paymentService;
    private User payer;
    private Reservation reservation;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                reservationRepository,
                teamMemberRepository,
                authHelper,
                mercadoPagoClient,
                mailService,
                "http://localhost:5173");

        payer = User.builder().id(1L).email("cliente@openspot.app").nombre("Cliente").build();
        reservation = Reservation.builder()
                .id(1000L)
                .createdBy(payer)
                .status(ReservationStatus.CONFIRMADA)
                .build();
        payment = Payment.builder()
                .id(2000L)
                .reservation(reservation)
                .orderId("order-1")
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void confirmApprovedSendsConfirmationEmail() {
        when(authHelper.currentUser()).thenReturn(payer);
        when(reservationRepository.findById(1000L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(1000L)).thenReturn(Optional.of(payment));
        when(mercadoPagoClient.getPaymentStatus("pay-123")).thenReturn("approved");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.confirm(new ConfirmPaymentRequest(1000L, "pay-123"));

        verify(mailService).sendPaymentConfirmation(eq(payer), eq(reservation), any(Payment.class));
    }

    @Test
    void confirmRejectedDoesNotSendEmail() {
        when(authHelper.currentUser()).thenReturn(payer);
        when(reservationRepository.findById(1000L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(1000L)).thenReturn(Optional.of(payment));
        when(mercadoPagoClient.getPaymentStatus("pay-123")).thenReturn("rejected");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.confirm(new ConfirmPaymentRequest(1000L, "pay-123"));

        verify(mailService, never()).sendPaymentConfirmation(any(), any(), any());
    }

    @Test
    void confirmAlreadyPaidDoesNotSendDuplicateEmail() {
        payment.setStatus(PaymentStatus.PAID);
        when(authHelper.currentUser()).thenReturn(payer);
        when(reservationRepository.findById(1000L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(1000L)).thenReturn(Optional.of(payment));
        when(mercadoPagoClient.getPaymentStatus("pay-123")).thenReturn("approved");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.confirm(new ConfirmPaymentRequest(1000L, "pay-123"));

        verify(mailService, never()).sendPaymentConfirmation(any(), any(), any());
    }
}
