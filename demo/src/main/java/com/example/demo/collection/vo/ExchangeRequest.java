package com.example.demo.collection.vo;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String currency;
}

