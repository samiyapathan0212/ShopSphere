package com.shopsphere.backend.dto.response;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Paginated response wrapper for catalog listings. Provides the content
 * plus standard pagination metadata.
 *
 * @param content       the page items
 * @param page          current page number (0-based)
 * @param size          page size
 * @param totalElements total number of elements across all pages
 * @param totalPages    total number of pages
 * @param hasNext       whether a next page exists
 * @param hasPrevious   whether a previous page exists
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@link PageResponse} from a Spring Data {@link Page}.
     *
     * @param page the source page
     * @param <T>  the content type
     * @return a new PageResponse
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }
}