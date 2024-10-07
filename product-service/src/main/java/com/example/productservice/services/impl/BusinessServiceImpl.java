package com.example.productservice.services.impl;

import com.example.productservice.services.BusinessService;
import org.springframework.stereotype.Service;

@Service
public class BusinessServiceImpl implements BusinessService {
    @Override
    public String doBusiness(String input) {
        try {
            Thread.sleep(1000); // 1s
            return input;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }
}
