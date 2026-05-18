package com.bocfintech.allstar.service;

import com.bocfintech.allstar.entity.ResourceCheckResponse;

import java.util.List;

public interface ResourceService {
    ResourceCheckResponse getResourceCheckByProduct(String productId, String batchNo, String fileSource);

    int deleteByOriginalFileName(String productId, String batchNo, String originalFileName);

    List<String> getAllProductIds();

}