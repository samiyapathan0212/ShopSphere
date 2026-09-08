package com.shopsphere.backend.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.shopsphere.backend.domain.Role;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.dto.request.CategoryRequest;
import com.shopsphere.backend.dto.request.ProductRequest;
import com.shopsphere.backend.dto.response.CategoryResponse;
import com.shopsphere.backend.dto.response.PageResponse;
import com.shopsphere.backend.dto.response.ProductResponse;
import com.shopsphere.backend.exception.CategoryNotFoundException;
import com.shopsphere.backend.exception.DuplicateCategoryNameException;
import com.shopsphere.backend.exception.DuplicateSkuException;
import com.shopsphere.backend.exception.GlobalExceptionHandler;
import com.shopsphere.backend.exception.ProductNotFoundException;
import com.shopsphere.backend.security.JwtProperties;
import com.shopsphere.backend.security.JwtService;
import com.shopsphere.backend.security.RestAccessDeniedHandler;
import com.shopsphere.backend.security.SecurityConfig;
import com.shopsphere.backend.security.UserPrincipal;
import com.shopsphere.backend.service.CategoryService;
import com.shopsphere.backend.service.ProductService;

@WebMvcTest({CategoryController.class, ProductController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CatalogAuthorizationTest.JwtTestBeans.class})
class CatalogAuthorizationTest {

    private static final String DEV_SECRET =
            "ZGV2LW9ubHktc2hvcHNwaGVyZS1qd3Qtc2VjcmV0LWtleS1wbGFjZWhvbGRlci0wMTIzNDU2Nzg5YWJjZGVm";

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @MockitoBean private CategoryService categoryService;
    @MockitoBean private ProductService productService;
    @MockitoBean private UserDetailsService userDetailsService;

    @TestConfiguration
    static class JwtTestBeans {
        @Bean JwtProperties jwtProperties() {
            JwtProperties p = new JwtProperties();
            p.setSecret(DEV_SECRET);
            p.setExpirationMs(900_000L);
            return p;
        }
        @Bean JwtService jwtService(JwtProperties properties) {
            return new JwtService(properties);
        }
    }

    private UserPrincipal principal(Role role, String email) {
        User user = User.builder().id(1L).name("Test User").email(email)
                .passwordHash("$2a$10$devOnlyHash").role(role).createdAt(NOW).updatedAt(NOW).build();
        return UserPrincipal.from(user);
    }

    private String tokenFor(UserPrincipal p) {
        when(userDetailsService.loadUserByUsername(p.getUsername())).thenReturn(p);
        return jwtService.generateToken(p);
    }

    private String adminToken() { return tokenFor(principal(Role.ADMIN, "admin@example.com")); }
    private String customerToken() { return tokenFor(principal(Role.CUSTOMER, "ada@example.com")); }

    private CategoryResponse categoryResponse() {
        return new CategoryResponse(1L, "Electronics", "Gadgets", true, NOW, NOW);
    }

    private ProductResponse productResponse() {
        return new ProductResponse(10L, "SKU-001", "Desk Lamp", "Warm LED",
                new BigDecimal("24.99"), true, categoryResponse(), NOW, NOW);
    }
    // --- Customer can read catalog ---

    @Test
    void customerCanListCategories() throws Exception {
        when(categoryService.listActiveCategories()).thenReturn(List.of(categoryResponse()));

        mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Electronics")));
    }

    @Test
    void customerCanGetCategoryById() throws Exception {
        when(categoryService.getActiveCategory(1L)).thenReturn(categoryResponse());

        mockMvc.perform(get("/api/categories/1").header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Electronics")));
    }

    @Test
    void customerCanListProducts() throws Exception {
        when(productService.listProducts(any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), anyBoolean()))
                .thenReturn(new PageResponse<>(List.of(productResponse()), 0, 20, 1L, 1, false, false));

        mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku", is("SKU-001")));
    }

    @Test
    void customerCanGetProductById() throws Exception {
        when(productService.getActiveProduct(10L)).thenReturn(productResponse());

        mockMvc.perform(get("/api/products/10").header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.category.name", is("Electronics")));
    }

    // --- Unauthenticated catalog access → 401 ---

    @Test
    void unauthenticatedCategoryAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Authentication is required")));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    // --- Customer cannot create/update/delete → 403 ---

    @Test
    void customerCannotCreateCategory() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + customerToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Toys\",\"description\":\"Fun\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", is(RestAccessDeniedHandler.FORBIDDEN_MESSAGE)));
    }

    @Test
    void customerCannotUpdateCategory() throws Exception {
        mockMvc.perform(put("/api/categories/1")
                        .header("Authorization", "Bearer " + customerToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotDeleteCategory() throws Exception {
        mockMvc.perform(delete("/api/categories/1")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotCreateProduct() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + customerToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-002\",\"name\":\"Chair\",\"price\":49.99,\"categoryId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotUpdateProduct() throws Exception {
        mockMvc.perform(put("/api/products/10")
                        .header("Authorization", "Bearer " + customerToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-002\",\"name\":\"Chair\",\"price\":49.99,\"categoryId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/10")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isForbidden());
    }

    // --- Admin can create/update/delete ---

    @Test
    void adminCanCreateCategory() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(categoryResponse());

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Electronics\",\"description\":\"Gadgets\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Electronics")));
    }

    @Test
    void adminCanUpdateCategory() throws Exception {
        when(categoryService.updateCategory(anyLong(), any(CategoryRequest.class)))
                .thenReturn(new CategoryResponse(1L, "Gaming", "Consoles", true, NOW, NOW));

        mockMvc.perform(put("/api/categories/1")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Gaming\",\"description\":\"Consoles\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Gaming")));
    }

    @Test
    void adminCanDeleteCategory() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanCreateProduct() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(productResponse());

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"Desk Lamp\",\"description\":\"Warm LED\",\"price\":24.99,\"categoryId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku", is("SKU-001")))
                .andExpect(jsonPath("$.category.name", is("Electronics")));
    }

    @Test
    void adminCanUpdateProduct() throws Exception {
        when(productService.updateProduct(anyLong(), any(ProductRequest.class)))
                .thenReturn(new ProductResponse(10L, "SKU-001", "LED Lamp", "Bright",
                        new BigDecimal("29.99"), true, categoryResponse(), NOW, NOW));

        mockMvc.perform(put("/api/products/10")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"LED Lamp\",\"description\":\"Bright\",\"price\":29.99,\"categoryId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("LED Lamp")));
    }

    @Test
    void adminCanDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct(10L);

        mockMvc.perform(delete("/api/products/10")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNoContent());
    }

    // --- Service-layer errors surface correctly ---

    @Test
    void getUnknownCategoryReturns404() throws Exception {
        when(categoryService.getActiveCategory(404L))
                .thenThrow(new CategoryNotFoundException(404L));

        mockMvc.perform(get("/api/categories/404").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Category not found: 404")));
    }

    @Test
    void getUnknownProductReturns404() throws Exception {
        when(productService.getActiveProduct(404L))
                .thenThrow(new ProductNotFoundException(404L));

        mockMvc.perform(get("/api/products/404").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Product not found: 404")));
    }

    @Test
    void createCategoryWithDuplicateNameReturns409() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new DuplicateCategoryNameException("Electronics"));

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Electronics\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("Category name is already in use: Electronics")));
    }

    @Test
    void createProductWithDuplicateSkuReturns409() throws Exception {
        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new DuplicateSkuException("SKU-001"));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"Lamp\",\"price\":24.99,\"categoryId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("SKU is already registered: SKU-001")));
    }

    @Test
    void createProductWithUnknownCategoryReturns404() throws Exception {
        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new CategoryNotFoundException(404L));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"name\":\"Lamp\",\"price\":24.99,\"categoryId\":404}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Category not found: 404")));
    }
}

