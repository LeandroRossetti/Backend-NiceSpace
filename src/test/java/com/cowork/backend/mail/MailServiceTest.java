package com.cowork.backend.mail;

import com.cowork.backend.location.Location;
import com.cowork.backend.payment.Payment;
import com.cowork.backend.payment.PaymentStatus;
import com.cowork.backend.reservation.RentalType;
import com.cowork.backend.reservation.Reservation;
import com.cowork.backend.reservation.ReservationStatus;
import com.cowork.backend.user.AccountType;
import com.cowork.backend.user.User;
import com.cowork.backend.workspace.Workspace;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendsPaymentConfirmationWhenEnabled() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        MailService mailService = new MailService(mailSender, "noreply@openspot.app", true);

        mailService.sendPaymentConfirmation(user(), reservation(), payment());

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        MimeMessage message = captor.getValue();
        assertThat(message.getSubject()).isEqualTo("Tu pago fue confirmado — OpenSpot");
        assertThat(message.getRecipients(Message.RecipientType.TO))
                .extracting(a -> ((InternetAddress) a).getAddress())
                .containsExactly("cliente@openspot.app");

        MimeMultipart multipart = (MimeMultipart) reparse(message).getContent();
        assertThat(htmlContent(multipart))
                .contains("Sede Palermo")
                .contains("Sala Norte")
                .contains("5000.00 ARS")
                .contains("Por hora")
                .contains("cid:openspot-logo");
        assertThat(hasInlineLogo(multipart)).isTrue();
    }

    @Test
    void doesNotSendWhenDisabled() {
        MailService mailService = new MailService(mailSender, "noreply@openspot.app", false);

        mailService.sendPaymentConfirmation(user(), reservation(), payment());

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void doesNotSendWhenRecipientHasNoEmail() {
        MailService mailService = new MailService(mailSender, "noreply@openspot.app", true);
        User withoutEmail = User.builder().id(1L).nombre("Cliente").accountType(AccountType.TENANT).build();

        mailService.sendPaymentConfirmation(withoutEmail, reservation(), payment());

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void swallowsMailExceptionWhenSendFails() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        MailService mailService = new MailService(mailSender, "noreply@openspot.app", true);

        assertThatCode(() -> mailService.sendPaymentConfirmation(user(), reservation(), payment()))
                .doesNotThrowAnyException();
    }

    private MimeMessage reparse(MimeMessage message) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(baos.toByteArray()));
    }

    private String htmlContent(Object content) throws Exception {
        if (content instanceof String s) {
            return s;
        }
        if (content instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String nested = htmlContent(multipart.getBodyPart(i).getContent());
                if (!nested.isEmpty()) {
                    return nested;
                }
            }
        }
        return "";
    }

    private boolean hasInlineLogo(Object content) throws Exception {
        if (content instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                String[] ids = part.getHeader("Content-ID");
                if (ids != null) {
                    for (String id : ids) {
                        if (id.contains("openspot-logo")) {
                            return true;
                        }
                    }
                }
                if (hasInlineLogo(part.getContent())) {
                    return true;
                }
            }
        }
        return false;
    }

    private User user() {
        return User.builder()
                .id(1L)
                .nombre("Cliente")
                .email("cliente@openspot.app")
                .accountType(AccountType.TENANT)
                .build();
    }

    private Reservation reservation() {
        Location location = Location.builder()
                .id(10L)
                .nombre("Sede Palermo")
                .build();
        Workspace workspace = Workspace.builder()
                .id(100L)
                .nombre("Sala Norte")
                .location(location)
                .build();
        return Reservation.builder()
                .id(1000L)
                .workspace(workspace)
                .startTime(Instant.parse("2026-11-02T12:00:00Z"))
                .endTime(Instant.parse("2026-11-02T14:00:00Z"))
                .rentalType(RentalType.HORA)
                .status(ReservationStatus.CONFIRMADA)
                .build();
    }

    private Payment payment() {
        return Payment.builder()
                .id(2000L)
                .paymentId("123456789")
                .status(PaymentStatus.PAID)
                .amount(new BigDecimal("5000.00"))
                .currency("ARS")
                .build();
    }
}
