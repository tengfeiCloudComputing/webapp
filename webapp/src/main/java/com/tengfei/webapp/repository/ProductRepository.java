package com.tengfei.webapp.repository;

import com.tengfei.webapp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    public Product findBySku(String sku);
}
