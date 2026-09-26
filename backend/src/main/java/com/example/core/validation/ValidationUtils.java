package com.example.core.validation;

import com.example.core.exception.ValidationException;

public final class ValidationUtils {

  private ValidationUtils() {}

  public static <T> T requireNonNull(T value, String message) {
    if (value == null) {
      throw new ValidationException(message);
    }

    return value;
  }
}
