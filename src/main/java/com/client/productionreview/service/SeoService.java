package com.client.productionreview.service;

import com.client.productionreview.dtos.seo.SeoProductDTO;

public interface SeoService {

    /** sitemap.xml com as URLs públicas do site (base = SITE_URL). */
    String sitemap();

    /** 404 se o produto não existir. */
    SeoProductDTO product(String slug);
}
