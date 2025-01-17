package com.example.demo.collection.service;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.demo.collection.CollectionRepository;
import com.example.demo.collection.vo.Collection;
import com.example.demo.collection.vo.ExchangeResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class, timeout = 60)
public class CollectionBatchService {

    private final CollectionRepository collectionRepository;
    private final RestTemplate restTemplate;

    /**
     * 每日18:00呼叫 API，取得外匯成交資料，並insert至DB
     */
    @Scheduled(cron = "0 0 18 * * ?")
    public void getExchangeData() {
    	
        final String apiUrl = "https://openapi.taifex.com.tw/v1/DailyForeignExchangeRates";
        
        try {
            String response = restTemplate.getForObject(apiUrl, String.class);
            
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
            List<Map<String, String>> rates = gson.fromJson(response, listType);

            for (Map<String, String> rate : rates) {
                String dateStr = rate.get("Date");
                String usdToNtdStr = rate.get("USD/NTD");

                if (dateStr != null && usdToNtdStr != null) {
                    LocalDateTime exchangeDate = LocalDateTime.parse(dateStr + " 18:00:00", DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss"));
                    Double usdToNtd = Double.valueOf(usdToNtdStr);

                    Collection collection = new Collection();
                    collection.setUsdToNtd(usdToNtd);
                    collection.setExchangeDate(exchangeDate);

                    collectionRepository.save(collection);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * 取出日期區間內美元 /台幣的歷史資料
     * @param startDate
     * @param endDate
     * @param currency
     * @return List<ExchangeResponse>
     */
    public List<ExchangeResponse> getHistoricalExchange(LocalDate startDate, LocalDate endDate, String currency) {
    	
        if (startDate.isBefore(LocalDate.now().minusYears(1)) || endDate.isAfter(LocalDate.now().minusDays(1))) {
            throw new IllegalArgumentException("日期區間不符");
        }

        if (!"usd".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("一次僅能查一種幣別");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        List<Collection> allCollections = collectionRepository.findAll();
        List<ExchangeResponse> filteredResponses = new ArrayList<>();

        for (Collection collection : allCollections) {
            LocalDate exchangeDate = collection.getExchangeDate().toLocalDate();
            if (!exchangeDate.isBefore(startDate) && !exchangeDate.isAfter(endDate)) {
                String formattedDate = collection.getExchangeDate().format(formatter);
                String usdToNtd = collection.getUsdToNtd().toString();
                ExchangeResponse response = new ExchangeResponse(formattedDate, usdToNtd);
                filteredResponses.add(response);
            }
        }
        return filteredResponses;
        
    }
}