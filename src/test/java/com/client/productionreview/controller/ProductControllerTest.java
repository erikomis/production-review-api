package com.client.productionreview.controller;


import com.client.productionreview.controller.mapper.ProductMapper;
import com.client.productionreview.dtos.product.ProductResponseDTO;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


import static org.hamcrest.Matchers.is;


@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductMapper productMapper;

    private Product product;
    private ProductResponseDTO productResponseDTO;



}

