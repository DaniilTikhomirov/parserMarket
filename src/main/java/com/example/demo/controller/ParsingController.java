package com.example.demo.controller;

import com.example.demo.entity.Smartphone;
import com.example.demo.service.ParsingService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("parse")
@Validated
public class ParsingController {
    private final ParsingService parsingService;

    @GetMapping("getAll")
    public ResponseEntity<Page<Smartphone>> getAll(@RequestParam(value = "offset", defaultValue = "0") @Min(0) Integer offset,
                                                   @RequestParam(value = "limit", defaultValue = "20") @Min(1) Integer limit){
        return ResponseEntity.ok(parsingService.getAll(offset, limit));
    }
}
