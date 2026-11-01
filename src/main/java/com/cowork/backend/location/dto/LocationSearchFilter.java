package com.cowork.backend.location.dto;

import java.time.Instant;
import java.util.List;

/**
 * Criterios de búsqueda del catálogo de sedes (inquilino).
 * Todos los campos son opcionales: un campo null/empty significa "sin filtro".
 */
public record LocationSearchFilter(
        String city,
        String province,
        List<Long> amenityIds,
        Instant start,
        Instant end
) {
}
