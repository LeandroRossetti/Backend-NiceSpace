package com.cowork.backend.mail;

import com.cowork.backend.payment.Payment;
import com.cowork.backend.reservation.RentalType;
import com.cowork.backend.reservation.Reservation;
import com.cowork.backend.user.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Envío de mails transaccionales. El envío es best-effort: cualquier fallo de
 * SMTP se loguea y nunca propaga, para no afectar el flujo que lo dispara.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    // Las reservas se guardan en UTC; para el mail se muestra en hora local argentina.
    private static final ZoneId LOCAL_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(LOCAL_ZONE);

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;

    public MailService(JavaMailSender mailSender,
                       @Value("${app.mail.from:}") String from,
                       @Value("${app.mail.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = enabled;
    }

    public void sendPaymentConfirmation(User recipient, Reservation reservation, Payment payment) {
        if (!enabled) {
            return;
        }
        if (recipient == null || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            log.warn("No se envió el mail de confirmación de pago: el destinatario no tiene email");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipient.getEmail());
            helper.setSubject("Tu pago fue confirmado — OpenSpot");
            helper.setText(buildHtml(recipient, reservation, payment), true);
            helper.addInline("openspot-logo", new ClassPathResource("mail/openspot-logo.png"));
            mailSender.send(message);
            log.info("Mail de confirmación de pago enviado a {}", recipient.getEmail());
        } catch (MessagingException e) {
            log.error("No se pudo enviar el mail de confirmación de pago a {}", recipient.getEmail(), e);
        }
    }

    private String buildHtml(User recipient, Reservation reservation, Payment payment) {
        String nombre = esc(recipient.getNombre() != null ? recipient.getNombre() : "");
        String saludo = nombre.isBlank() ? "Hola" : "Hola " + nombre;

        String sede = reservation.getWorkspace().getLocation().getNombre();
        String espacio = reservation.getWorkspace().getNombre();
        String inicio = DATE_FORMATTER.format(reservation.getStartTime());
        String fin = DATE_FORMATTER.format(reservation.getEndTime());
        String modalidad = rentalTypeLabel(reservation.getRentalType());
        String monto = formatAmount(payment.getAmount()) + " " + esc(payment.getCurrency());
        String paymentId = esc(payment.getPaymentId() != null ? payment.getPaymentId() : "-");

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <title>Confirmación de pago</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f4f6f8;padding:24px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="560" cellspacing="0" cellpadding="0" style="max-width:560px;width:100%%;background-color:#ffffff;border-radius:12px;overflow:hidden;">
                          <tr>
                            <td style="padding:24px 32px;background-color:#0ea5b7;color:#ffffff;">
                              <table role="presentation" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td style="width:32px;vertical-align:middle;">
                                    <img src="cid:openspot-logo" alt="OpenSpot" width="32" height="32" style="width:32px;height:32px;border:0;display:block;">
                                  </td>
                                  <td style="vertical-align:middle;padding-left:10px;color:#ffffff;font-size:20px;font-weight:bold;">OpenSpot</td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <h2 style="margin:0 0 8px;font-size:18px;color:#0f172a;">%s, ¡tu pago fue confirmado!</h2>
                              <p style="margin:0 0 24px;color:#475569;font-size:14px;line-height:1.6;">
                                Ya recibimos tu pago a través de Mercado Pago. Acá tenés el detalle de tu reserva.
                              </p>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border:1px solid #e2e8f0;border-radius:8px;overflow:hidden;">
                                <tr><td style="padding:12px 16px;background-color:#f8fafc;color:#0f172a;font-size:13px;font-weight:bold;">Sede</td><td style="padding:12px 16px;color:#334155;font-size:13px;">%s</td></tr>
                                <tr><td style="padding:12px 16px;background-color:#f8fafc;color:#0f172a;font-size:13px;font-weight:bold;">Espacio</td><td style="padding:12px 16px;color:#334155;font-size:13px;">%s</td></tr>
                                <tr><td style="padding:12px 16px;background-color:#f8fafc;color:#0f172a;font-size:13px;font-weight:bold;">Desde</td><td style="padding:12px 16px;color:#334155;font-size:13px;">%s</td></tr>
                                <tr><td style="padding:12px 16px;background-color:#f8fafc;color:#0f172a;font-size:13px;font-weight:bold;">Hasta</td><td style="padding:12px 16px;color:#334155;font-size:13px;">%s</td></tr>
                                <tr><td style="padding:12px 16px;background-color:#f8fafc;color:#0f172a;font-size:13px;font-weight:bold;">Modalidad</td><td style="padding:12px 16px;color:#334155;font-size:13px;">%s</td></tr>
                                <tr><td style="padding:12px 16px;background-color:#f8fafc;color:#0f172a;font-size:13px;font-weight:bold;">Total</td><td style="padding:12px 16px;color:#0f172a;font-size:15px;font-weight:bold;">%s</td></tr>
                                <tr><td style="padding:12px 16px;background-color:#f8fafc;color:#0f172a;font-size:13px;font-weight:bold;">Pago</td><td style="padding:12px 16px;color:#334155;font-size:13px;">%s</td></tr>
                              </table>
                              <p style="margin:24px 0 0;color:#94a3b8;font-size:12px;line-height:1.6;">
                                Si tenés alguna duda, podés responder este mail. Gracias por usar OpenSpot.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(saludo, esc(sede), esc(espacio), inicio, fin, modalidad, monto, paymentId);
    }

    private String rentalTypeLabel(RentalType rentalType) {
        if (rentalType == null) {
            return "-";
        }
        return switch (rentalType) {
            case HORA -> "Por hora";
            case DIA -> "Por día";
            case MES -> "Por mes";
        };
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
