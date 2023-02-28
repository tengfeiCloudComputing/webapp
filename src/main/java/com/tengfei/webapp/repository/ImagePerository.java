package com.tengfei.webapp.repository;

import com.tengfei.webapp.model.Image;
import com.tengfei.webapp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImagePerository extends JpaRepository<Image, String> {
    Optional<List<Image>> findImagesByProduct_Id(int id);
}
