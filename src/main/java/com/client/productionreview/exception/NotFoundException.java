package com.client.productionreview.exception;

public class NotFoundException extends  RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
