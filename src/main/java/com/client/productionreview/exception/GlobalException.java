package com.client.productionreview.exception;


import lombok.Data;
import org.springframework.http.HttpStatus;



@Data
public class GlobalException extends RuntimeException {

  private final String mensage;
  private final HttpStatus httpStatus;
  private final String category;


  public GlobalException(String mensage, HttpStatus httpStatus, String category) {
    this.mensage = mensage;
    this.httpStatus = httpStatus;
    this.category = category;
  }


   public GlobalException(String mensage, HttpStatus httpStatus) {
    this(mensage, httpStatus, null);
  }

  public GlobalException(String mensage) {
      this(mensage, null, null);
  }



}
