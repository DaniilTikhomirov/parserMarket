package com.example.demo.service;

import com.example.demo.entity.Smartphone;
import com.example.demo.repository.SmartphoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class ParsingService {
    private final SmartphoneRepository smartphoneRepository;

    public Page<Smartphone> getAll(int offset, int limit) {
        return smartphoneRepository.findAll(PageRequest.of(offset, limit));
    }
}