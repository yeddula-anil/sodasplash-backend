package com.sodasplash.product_service.repository;


import com.sodasplash.product_service.entity.Flavour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FlavourRepository extends JpaRepository<Flavour, Long> {

    Optional<Flavour> findByProductIdAndNameIgnoreCase(Long productId, String name);
}
