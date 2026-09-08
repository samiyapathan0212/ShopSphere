package com.shopsphere.backend.domain;

/**
 * User roles. Registration always creates CUSTOMER accounts; ADMIN accounts
 * are provisioned out-of-band (e.g. SQL seed/UPDATE) until an admin flow
 * exists. Method security maps these onto {@code ROLE_<NAME>} authorities —
 * never define roles anywhere else.
 */
public enum Role {
    CUSTOMER,
    ADMIN
}