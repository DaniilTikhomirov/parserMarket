package com.example.demo.dto;

import com.example.demo.entity.Smartphone;

import java.util.List;

public record SmartphoneListDto(
        int count,
        List<Smartphone> smartphoneDtos
) {
}
