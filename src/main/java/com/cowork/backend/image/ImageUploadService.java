package com.cowork.backend.image;

import com.cowork.backend.common.BadRequestException;
import com.cowork.backend.common.ForbiddenException;
import com.cowork.backend.security.AuthHelper;
import com.cowork.backend.user.AccountType;
import com.cowork.backend.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Service
public class ImageUploadService {

    private static final long MAX_IMAGE_BYTES = 32L * 1024 * 1024;
    private static final String IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload";

    private final AuthHelper authHelper;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;

    public ImageUploadService(AuthHelper authHelper,
                              ObjectMapper objectMapper,
                              @Value("${app.imgbb.api-key:}") String apiKey) {
        this.authHelper = authHelper;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
        this.apiKey = apiKey;
    }

    public String upload(MultipartFile file) {
        User user = authHelper.currentUser();
        if (user.getAccountType() != AccountType.OWNER) {
            throw new ForbiddenException("Solo los dueños de espacios pueden subir imágenes");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Debés seleccionar una imagen");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("La imagen supera el tamaño máximo permitido (32 MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("El archivo debe ser una imagen");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("La subida de imágenes no está configurada");
        }

        String base64;
        try {
            base64 = Base64.getEncoder().encodeToString(file.getBytes());
        } catch (Exception e) {
            throw new BadRequestException("No se pudo leer la imagen");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("key", apiKey);
        form.add("image", base64);
        if (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()) {
            form.add("name", file.getOriginalFilename());
        }

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri(IMGBB_UPLOAD_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new BadRequestException("No se pudo subir la imagen a imgbb");
        }

        return extractUrl(responseBody);
    }

    private String extractUrl(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (!root.path("success").asBoolean(false)) {
                String message = root.path("error").path("message").asText(null);
                throw new BadRequestException(message != null ? message : "imgbb rechazó la imagen");
            }
            String url = root.path("data").path("image").path("url").asText(null);
            if (url == null || url.isBlank()) {
                throw new BadRequestException("imgbb no devolvió la URL de la imagen");
            }
            return url;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("No se pudo procesar la respuesta de imgbb");
        }
    }
}
