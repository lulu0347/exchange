package com.example.demo.collection.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.collection.service.CollectionBatchService;
import com.example.demo.collection.vo.ErrorResponse;
import com.example.demo.collection.vo.ExchangeRequest;
import com.example.demo.collection.vo.ExchangeResponse;
import com.example.demo.collection.vo.ExchangeResponseWrapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CollectionBatchController {

    private final CollectionBatchService collectionBatchService;

    /**
     * 每日排程 18:00取得美元/台幣外匯成交資料，執行此api可直接獲得資料
     * @return
     */
    @GetMapping("/getExchangeData") // http://localhost:8080/api/getExchangeData
    public String runBatch() {
    	collectionBatchService.getExchangeData();
        return "Batch process executed!";
    }
    
    /**
     * 取出日期區間內美元/台幣的歷史資料
     * @param request
     * @return ResponseEntity<?>
     */
    @PostMapping("/getHistoryExchange") // http://localhost:8080/api/getHistoryExchange
    public ResponseEntity<?> getHistoricalExchange(@RequestBody ExchangeRequest request) {
        try {
            List<ExchangeResponse> result = collectionBatchService.getHistoricalExchange(
            		request.getStartDate(),
            		request.getEndDate(), 
                    request.getCurrency()
            );
            return ResponseEntity.ok(
            		new ExchangeResponseWrapper(new ErrorResponse("0000", "成功"),result)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            		new ExchangeResponseWrapper(new ErrorResponse("E001", e.getMessage()))
            );
        }
    }
}