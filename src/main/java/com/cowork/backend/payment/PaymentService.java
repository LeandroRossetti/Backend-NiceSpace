package com.cowork.backend.payment;

import com.cowork.backend.common.BadRequestException;
import com.cowork.backend.common.ConflictException;
import com.cowork.backend.common.ForbiddenException;
import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.mail.MailService;
import com.cowork.backend.payment.dto.ConfirmPaymentRequest;
import com.cowork.backend.payment.dto.PaymentDto;
import com.cowork.backend.payment.mercadopago.MercadoPagoClient;
import com.cowork.backend.reservation.Reservation;
import com.cowork.backend.reservation.ReservationRepository;
import com.cowork.backend.reservation.ReservationStatus;
import com.cowork.backend.security.AuthHelper;
import com.cowork.backend.team.TeamMemberRepository;
import com.cowork.backend.team.TeamMemberStatus;
import com.cowork.backend.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuthHelper authHelper;
    private final MercadoPagoClient mercadoPagoClient;
    private final MailService mailService;
    private final String frontendUrl;

    public PaymentService(PaymentRepository paymentRepository,
                          ReservationRepository reservationRepository,
                          TeamMemberRepository teamMemberRepository,
                          AuthHelper authHelper,
                          MercadoPagoClient mercadoPagoClient,
                          MailService mailService,
                          @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.authHelper = authHelper;
        this.mercadoPagoClient = mercadoPagoClient;
        this.mailService = mailService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public PaymentDto pay(Long reservationId) {
        User user = authHelper.currentUser();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
        assertCanPay(user, reservation);

        if (reservation.getStatus() != ReservationStatus.CONFIRMADA) {
            throw new ConflictException("Solo se puede pagar una reserva confirmada");
        }
        if (reservation.getTotalPrice() == null) {
            throw new BadRequestException("La reserva no tiene un monto para cobrar");
        }

        Payment existing = paymentRepository.findByReservationId(reservationId).orElse(null);
        if (existing != null && existing.getStatus() == PaymentStatus.PAID) {
            throw new ConflictException("La reserva ya fue pagada");
        }
        if (existing != null) {
            return PaymentDto.from(existing);
        }

        String currency = reservation.getWorkspace().getCurrency();
        String successUrl = frontendUrl + "/reservations";
        String failureUrl = frontendUrl + "/reservations";
        String pendingUrl = frontendUrl + "/reservations";

        MercadoPagoClient.OrderResult result = mercadoPagoClient.createOrder(
                "Reserva " + reservation.getWorkspace().getNombre(),
                reservation.getTotalPrice(),
                currency,
                String.valueOf(reservation.getId()),
                successUrl,
                failureUrl,
                pendingUrl);

        Payment payment = Payment.builder()
                .reservation(reservation)
                .orderId(result.orderId())
                .paymentId(null)
                .status(PaymentStatus.PENDING)
                .amount(reservation.getTotalPrice())
                .currency(currency)
                .checkoutUrl(result.checkoutUrl())
                .build();

        return PaymentDto.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentDto confirm(ConfirmPaymentRequest request) {
        User user = authHelper.currentUser();
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
        assertCanPay(user, reservation);

        Payment payment = paymentRepository.findByReservationId(request.reservationId())
                .orElseThrow(() -> new NotFoundException("No hay un pago iniciado para esta reserva"));

        boolean wasAlreadyPaid = payment.getStatus() == PaymentStatus.PAID;

        String mpStatus = mercadoPagoClient.getPaymentStatus(request.paymentId());

        PaymentStatus newStatus = switch (mpStatus) {
            case "approved" -> PaymentStatus.PAID;
            case "rejected", "cancelled", "refunded", "charged_back" -> PaymentStatus.REJECTED;
            default -> PaymentStatus.PENDING;
        };

        payment.setPaymentId(request.paymentId());
        payment.setStatus(newStatus);
        Payment saved = paymentRepository.save(payment);

        if (newStatus == PaymentStatus.PAID && !wasAlreadyPaid) {
            mailService.sendPaymentConfirmation(user, reservation, saved);
        }

        return PaymentDto.from(saved);
    }

    private void assertCanPay(User user, Reservation reservation) {
        boolean allowed = reservation.getCreatedBy().getId().equals(user.getId())
                || (reservation.getTeam() != null
                    && teamMemberRepository.existsByTeamIdAndUserIdAndStatus(
                        reservation.getTeam().getId(), user.getId(), TeamMemberStatus.ACTIVE));
        if (!allowed) {
            throw new ForbiddenException("No tienes permisos para pagar esta reserva");
        }
    }
}
