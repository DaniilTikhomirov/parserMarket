package com.example.demo.repository;

import com.example.demo.entity.Smartphone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmartphoneRepository extends JpaRepository<Smartphone, String> {
    Page<Smartphone> findAll(Pageable pageable);
}
