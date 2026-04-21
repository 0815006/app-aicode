package com.bocfintech.allstar.service;

import com.bocfintech.allstar.entity.ResourceCheckResponse;

import java.util.List;

public interface ResourceService {
    ResourceCheckResponse getResourceCheckByProduct(String productId, String fileSource);

    // ResourceService.java
    int deleteByOriginalFileName(String productId, String originalFileName);

    List<String> getAllProductIds();

}