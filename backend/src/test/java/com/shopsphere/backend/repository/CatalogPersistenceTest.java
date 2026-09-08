package com.shopsphere.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.shopsphere.backend.domain.Category;
import com.shopsphere.backend.domain.Product;

/**
 * Persistence tests for the Phase 3A catalog entities and repositories,
 * running against an embedded database. Flyway is disabled here and Hibernate
 * creates the schema from the mappings ({@code ddl-auto=create-drop},
 * overriding the {@code validate} in {@code application.yml}): the V4
 * migration itself is validated against real PostgreSQL in the live phase;
 * this suite proves the entity mappings, constraints surfaced through
 * Hibernate, and every repository lookup method.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CatalogPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Category category(String name) {
        return Category.builder().name(name).description(name + " gear").build();
    }

    private Product product(String sku, String name, Category cat) {
        return Product.builder()
                .sku(sku)
                .name(name)
                .description(name + " description")
                .price(new BigDecimal("19.99"))
                .category(cat)
                .build();
    }

    @Test
    void categoryPersistsWithGeneratedIdAndDefaults() {
        Category saved = categoryRepository.saveAndFlush(category("Electronics"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Category loaded = categoryRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getName()).isEqualTo("Electronics");
        assertThat(loaded.getDescription()).isEqualTo("Electronics gear");
    }

    @Test
    void categoryNameIsUnique() {
        categoryRepository.saveAndFlush(category("Books"));

        // Spring Data translates the underlying constraint violation into
        // Spring's DataAccessException hierarchy.
        assertThatThrownBy(() -> categoryRepository.saveAndFlush(category("Books")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void productPersistsWithCategoryRelationship() {
        Category cat = categoryRepository.saveAndFlush(category("Fitness"));
        Product saved = productRepository.saveAndFlush(product("FIT-001", "Yoga Mat", cat));

        assertThat(saved.getId()).isNotNull();

        Product loaded = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getSku()).isEqualTo("FIT-001");
        assertThat(loaded.getName()).isEqualTo("Yoga Mat");
        assertThat(loaded.getPrice()).isEqualByComparingTo("19.99");
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();

        // The product-category relationship resolves to the persisted category.
        assertThat(loaded.getCategory().getId()).isEqualTo(cat.getId());
        assertThat(loaded.getCategory().getName()).isEqualTo("Fitness");
    }

    @Test
    void skuIsUnique() {
        Category cat = categoryRepository.saveAndFlush(category("Games"));
        productRepository.saveAndFlush(product("GAME-001", "Chess", cat));

        assertThatThrownBy(() -> productRepository.saveAndFlush(product("GAME-001", "Checkers", cat)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void productRequiresACategory() {
        Product orphan = Product.builder()
                .sku("ORPHAN-001")
                .name("No Category")
                .price(new BigDecimal("5.00"))
                .category(null)
                .build();

        assertThatThrownBy(() -> productRepository.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingCategoryWithProductsIsBlockedByFk() {
        Category cat = categoryRepository.saveAndFlush(category("Music"));
        productRepository.saveAndFlush(product("MUS-001", "Vinyl", cat));

        // Bypass the entity graph and delete the category directly at the SQL
        // level, so the database's fk_products_category constraint is what
        // rejects the delete (Hibernate alone would raise TransientObjectException).
        assertThatThrownBy(() -> entityManager.getEntityManager()
                .createNativeQuery("DELETE FROM categories WHERE id = :id")
                .setParameter("id", cat.getId())
                .executeUpdate())
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void categoryLookupsByName() {
        Category saved = categoryRepository.saveAndFlush(category("Toys"));

        assertThat(categoryRepository.findByName("Toys")).hasValueSatisfying(
                c -> assertThat(c.getId()).isEqualTo(saved.getId()));
        assertThat(categoryRepository.findByName("Missing")).isEmpty();
        assertThat(categoryRepository.existsByName("Toys")).isTrue();
        assertThat(categoryRepository.existsByName("Toys-elsewhere")).isFalse();
        assertThat(categoryRepository.existsByNameAndIdNot("Toys", saved.getId())).isFalse();
        assertThat(categoryRepository.existsByNameAndIdNot("Toys", saved.getId() + 1)).isTrue();
    }

    @Test
    void productLookupsBySku() {
        Category cat = categoryRepository.saveAndFlush(category("Garden"));
        Product saved = productRepository.saveAndFlush(product("GRD-001", "Hose", cat));

        assertThat(productRepository.findBySku("GRD-001")).hasValueSatisfying(
                p -> assertThat(p.getId()).isEqualTo(saved.getId()));
        assertThat(productRepository.findBySku("GRD-404")).isEmpty();
        assertThat(productRepository.existsBySku("GRD-001")).isTrue();
        assertThat(productRepository.existsBySku("GRD-404")).isFalse();
        assertThat(productRepository.existsBySkuAndIdNot("GRD-001", saved.getId())).isFalse();
        assertThat(productRepository.existsBySkuAndIdNot("GRD-001", saved.getId() + 1)).isTrue();
    }

    @Test
    void activeAndCategoryFilters() {
        Category activeCat = categoryRepository.saveAndFlush(category("Active Cat"));
        Category archivedCat = categoryRepository.saveAndFlush(
                Category.builder().name("Archived Cat").active(false).build());

        assertThat(categoryRepository.findAllByActiveTrue())
                .extracting(Category::getName)
                .containsExactly("Active Cat");

        productRepository.saveAndFlush(product("A-1", "Active Product", activeCat));
        productRepository.saveAndFlush(Product.builder()
                .sku("A-2")
                .name("Archived Product")
                .price(new BigDecimal("5.00"))
                .active(false)
                .category(archivedCat)
                .build());

        assertThat(productRepository.findAllByActiveTrue())
                .extracting(Product::getSku)
                .containsExactly("A-1");
        assertThat(productRepository.findAllByCategoryId(activeCat.getId()))
                .extracting(Product::getSku)
                .containsExactly("A-1");
        assertThat(productRepository.findAllByCategoryId(archivedCat.getId()))
                .extracting(Product::getSku)
                .containsExactly("A-2");
        assertThat(productRepository.findAllByCategoryIdAndActiveTrue(activeCat.getId()))
                .extracting(Product::getSku)
                .containsExactly("A-1");
        assertThat(productRepository.findAllByCategoryIdAndActiveTrue(archivedCat.getId()))
                .isEmpty();
    }
}