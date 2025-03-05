package com.audio.converter.util;

import lombok.Data;

@Data
public class RequestValidationException extends RuntimeException {

  private String code;
  private String message;

  public RequestValidationException(String code, String message) {
    super();
    this.setCode(code);
    this.setMessage(message);
  }

  @Override
  public String toString() {
    return "RequestValidationException{" +
        "code='" + code + '\'' +
        "} " + super.toString();
  }
}
