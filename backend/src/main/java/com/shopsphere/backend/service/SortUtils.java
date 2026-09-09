package com.shopsphere.backend.service;

import org.springframework.data.domain.Sort;

/**
 * Helpers for safe sorting on whitelisted entity fields.
 */
public final class SortUtils {

    private SortUtils() {
    }

    public static Sort createdAtDesc() {
        return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
