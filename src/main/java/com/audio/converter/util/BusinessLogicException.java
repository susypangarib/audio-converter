package com.audio.converter.util;

import lombok.Data;

@Data
public class BusinessLogicException extends RuntimeException {

  private String code;
  private String message;

  public BusinessLogicException(String code, String message) {
    super();
    this.setCode(code);
    this.setMessage(message);
  }

  @Override
  public String toString() {
    return "BusinessLogicException{" +
        "code='" + code + '\'' +
        "} " + super.toString();
  }
}
