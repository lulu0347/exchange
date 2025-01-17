package com.example.demo.collection.vo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExchangeResponseWrapper{
	private ErrorResponse error;
    private List<ExchangeResponse> currency;
    
    // only error message
    public ExchangeResponseWrapper(ErrorResponse error) {
        this.error = error;
    }

	public ExchangeResponseWrapper(ErrorResponse error, List<ExchangeResponse> currency) {
		super();
		this.error = error;
		this.currency = currency;
	}
}

