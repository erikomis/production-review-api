package com.client.productionreview.service;

import com.client.productionreview.model.jpa.ProductImage;
import org.springframework.web.multipart.MultipartFile;



public interface ProductImageService {

    ProductImage createProductImage(MultipartFile file, Long idProduct);

    void deleteFile(Long idProductImage);


}
