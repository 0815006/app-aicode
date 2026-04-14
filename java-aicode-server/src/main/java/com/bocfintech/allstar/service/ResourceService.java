package com.bocfintech.allstar.service;

import com.bocfintech.allstar.entity.ResourceCheckResponse;

public interface ResourceService {
    ResourceCheckResponse getResourceCheckByProduct(String productId);

    // ResourceService.java
    int deleteByOriginalFileName(String productId, String originalFileName);

}