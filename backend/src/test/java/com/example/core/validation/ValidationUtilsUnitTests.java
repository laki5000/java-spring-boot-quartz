package com.example.core.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.core.exception.ValidationException;
import org.junit.jupiter.api.Test;

class ValidationUtilsUnitTests {

  private static final String TEST_VALUE = "test";
  private static final String VALIDATION_ERROR_MESSAGE = "Value must not be null";

  @Test
  void testRequireNonNull_shouldReturnValue_whenValueIsNotNull() {
    // When
    String result = ValidationUtils.requireNonNull(TEST_VALUE, VALIDATION_ERROR_MESSAGE);

    // Then
    assertEquals(TEST_VALUE, result);
  }

  @Test
  void testRequireNonNull_shouldThrowValidationException_whenValueIsNull() {
    // When / Then
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> ValidationUtils.requireNonNull(null, VALIDATION_ERROR_MESSAGE));

    assertEquals(VALIDATION_ERROR_MESSAGE, exception.getMessage());
  }
}
