package com.cowork.backend.payment.mercadopago;

import com.cowork.backend.common.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Cliente HTTP mínimo para la API de Mercado Pago (Checkout Pro / Orders).
 * Usa RestClient (ya disponible en Spring Web), sin dependencias externas.
 */
@Component
public class MercadoPagoClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String accessToken;
    private final String baseUrl;

    public MercadoPagoClient(ObjectMapper objectMapper,
                             @Value("${app.mercadopago.access-token:}") String accessToken,
                             @Value("${app.mercadopago.base-url:https://api.mercadopago.com}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
        this.accessToken = accessToken;
        this.baseUrl = baseUrl;
    }

    public record OrderResult(String orderId, String checkoutUrl) {
    }

    /**
     * Crea una order de Checkout Pro (processing_mode = manual) y devuelve el
     * id de la order y la checkout_url para redirigir al comprador.
     */
    public OrderResult createOrder(String title,
                                   BigDecimal amount,
                                   String currency,
                                   String externalReference,
                                   String successUrl,
                                   String failureUrl,
                                   String pendingUrl) {
        ensureConfigured();

        String formattedAmount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();

        ObjectNode item = objectMapper.createObjectNode();
        item.put("title", title);
        item.put("quantity", 1);
        item.put("unit_price", formattedAmount);

        ObjectNode online = objectMapper.createObjectNode();
        online.put("success_url", successUrl);
        online.put("failure_url", failureUrl);
        online.put("pending_url", pendingUrl);
        online.put("auto_return", "approved");

        ObjectNode config = objectMapper.createObjectNode();
        config.set("online", online);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "online");
        body.put("processing_mode", "manual");
        body.put("total_amount", formattedAmount);
        body.put("external_reference", externalReference);
        body.put("description", title);
        body.set("items", objectMapper.createArrayNode().add(item));
        body.set("config", config);

        String response;
        try {
            response = restClient.post()
                    .uri(baseUrl + "/v1/orders")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new BadRequestException("No se pudo generar el pago en Mercado Pago");
        }

        return parseOrder(response);
    }

    /**
     * Consulta el estado de un pago. Se sigue usando la Payments API porque la
     * back_url devuelve el payment_id y este endpoint continúa soportado.
     */
    public String getPaymentStatus(String paymentId) {
        ensureConfigured();

        String response;
        try {
            response = restClient.get()
                    .uri(baseUrl + "/v1/payments/" + paymentId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new BadRequestException("No se pudo consultar el estado del pago en Mercado Pago");
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText(null);
            if (status == null || status.isBlank()) {
                throw new BadRequestException("Mercado Pago no devolvió el estado del pago");
            }
            return status;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("No se pudo procesar la respuesta de Mercado Pago");
        }
    }

    private OrderResult parseOrder(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String orderId = root.path("id").asText(null);
            String checkoutUrl = root.path("checkout_url").asText(null);
            if (orderId == null || orderId.isBlank() || checkoutUrl == null || checkoutUrl.isBlank()) {
                throw new BadRequestException("Mercado Pago no devolvió los datos de la order");
            }
            return new OrderResult(orderId, checkoutUrl);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("No se pudo procesar la respuesta de Mercado Pago");
        }
    }

    private void ensureConfigured() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BadRequestException("Mercado Pago no está configurado");
        }
    }
}
