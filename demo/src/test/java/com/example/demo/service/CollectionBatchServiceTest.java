package com.example.demo.service;

import com.example.demo.collection.CollectionRepository;
import com.example.demo.collection.controller.CollectionBatchController;
import com.example.demo.collection.service.CollectionBatchService;
import com.example.demo.collection.vo.Collection;
import com.example.demo.collection.vo.ExchangeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CollectionBatchServiceTest {

    @Mock
    private CollectionBatchService collectionBatchService;

    @Mock
    private CollectionRepository collectionRepository;

    @InjectMocks
    private CollectionBatchController collectionBatchController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(collectionBatchController).build();
    }

    // 1. 呼叫 API 取得資料
    @Test
    public void testGetExchangeData_BatchProcessing() throws Exception {
        doAnswer(invocation -> {
            Collection collection1 = new Collection();
            collection1.setUsdToNtd(30.1234);
            collection1.setExchangeDate(LocalDateTime.of(2024, 12, 9, 18, 0));

            Collection collection2 = new Collection();
            collection2.setUsdToNtd(30.5678);
            collection2.setExchangeDate(LocalDateTime.of(2024, 12, 10, 18, 0));

            collectionRepository.save(collection1);
            collectionRepository.save(collection2);
            return null;
        }).when(collectionBatchService).getExchangeData();

        mockMvc.perform(get("/api/getExchangeData"))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch process executed!"));

        verify(collectionRepository, times(2)).save(argThat(collection -> {
            LocalDateTime exchangeDate = collection.getExchangeDate();
            return (exchangeDate.equals(LocalDateTime.of(2024, 12, 9, 18, 0)) &&
                    collection.getUsdToNtd().equals(30.1234)) ||
                   (exchangeDate.equals(LocalDateTime.of(2024, 12, 10, 18, 0)) &&
                    collection.getUsdToNtd().equals(30.5678));
        }));
    }

    // 2. 成功
    @Test
    public void testGetHistoricalExchange_Success() throws Exception {
        when(collectionBatchService.getHistoricalExchange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1), "usd"))
                .thenReturn(Arrays.asList(
                        new ExchangeResponse("20240103", "31.01"),
                        new ExchangeResponse("20240104", "31.016")
                ));

        mockMvc.perform(post("/api/getHistoryExchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\": \"2024-01-01\", \"endDate\": \"2024-01-01\", \"currency\": \"usd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value("0000"))
                .andExpect(jsonPath("$.error.message").value("成功"))
                .andExpect(jsonPath("$.currency[0].date").value("20240103"))
                .andExpect(jsonPath("$.currency[0].usd").value("31.01"))
                .andExpect(jsonPath("$.currency[1].date").value("20240104"))
                .andExpect(jsonPath("$.currency[1].usd").value("31.016"));
    }

    // 2. 日期範圍錯誤
    @Test
    public void testGetHistoricalExchange_InvalidDateRange() throws Exception {
        when(collectionBatchService.getHistoricalExchange(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("日期區間不符"));

        mockMvc.perform(post("/api/getHistoryExchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\": \"2022-01-01\", \"endDate\": \"2024-12-01\", \"currency\": \"usd\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("E001"))
                .andExpect(jsonPath("$.error.message").value("日期區間不符"))
                .andExpect(jsonPath("$.currency").doesNotExist());
    }

    // 2. 幣別錯誤
    @Test
    public void testGetHistoricalExchange_InvalidCurrency() throws Exception {
        when(collectionBatchService.getHistoricalExchange(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("一次僅能查一種幣別"));

        mockMvc.perform(post("/api/getHistoryExchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\": \"2024-01-01\", \"endDate\": \"2024-01-01\", \"currency\": \"eur\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("E001"))
                .andExpect(jsonPath("$.error.message").value("一次僅能查一種幣別"))
                .andExpect(jsonPath("$.currency").doesNotExist());
    }
}
