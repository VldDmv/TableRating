package org.criticizer.service.helper;

import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.exceptions.validation.InvalidScoreException;
import org.criticizer.exceptions.validation.WeakPasswordException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ServiceValidator utility class.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceValidator Tests")
class ServiceValidatorTest {

    @InjectMocks
    private ServiceValidator validator;

    // ==================== VALIDATE SCORE TESTS ====================

    @Nested
    @DisplayName("validateScore() Tests")
    class ValidateScoreTests {

        @Test
        @DisplayName("Should accept valid score of 1")
        void shouldAcceptScoreOne() {
            assertThatCode(() -> validator.validateScore(1, 1, "Game"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid score of 100")
        void shouldAcceptScoreHundred() {
            assertThatCode(() -> validator.validateScore(100, 1, "Movie"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid score of 50")
        void shouldAcceptScoreFifty() {
            assertThatCode(() -> validator.validateScore(50, 1, "Book"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw for score less than 1")
        void shouldThrowForScoreLessThanOne() {
            assertThatThrownBy(() -> validator.validateScore(0, 1, "Game"))
                    .isInstanceOf(InvalidScoreException.class)
                    .hasMessageContaining("0");
        }

        @Test
        @DisplayName("Should throw for score greater than 100")
        void shouldThrowForScoreGreaterThanHundred() {
            assertThatThrownBy(() -> validator.validateScore(101, 1, "Movie"))
                    .isInstanceOf(InvalidScoreException.class)
                    .hasMessageContaining("101");
        }

        @Test
        @DisplayName("Should throw for negative score")
        void shouldThrowForNegativeScore() {
            assertThatThrownBy(() -> validator.validateScore(-10, 1, "Show"))
                    .isInstanceOf(InvalidScoreException.class);
        }
    }

    // ==================== VALIDATE USERNAME TESTS ====================

    @Nested
    @DisplayName("validateUsername() Tests")
    class ValidateUsernameTests {

        @Test
        @DisplayName("Should accept valid username")
        void shouldAcceptValidUsername() {
            String result = validator.validateUsername("john_doe");
            assertThat(result).isEqualTo("john_doe");
        }

        @Test
        @DisplayName("Should trim whitespace from username")
        void shouldTrimWhitespace() {
            String result = validator.validateUsername("  john_doe  ");
            assertThat(result).isEqualTo("john_doe");
        }

        @Test
        @DisplayName("Should throw for null username")
        void shouldThrowForNull() {
            assertThatThrownBy(() -> validator.validateUsername(null))
                    .isInstanceOf(EmptyNameException.class)
                    .hasMessageContaining("Username");
        }

        @Test
        @DisplayName("Should throw for empty username")
        void shouldThrowForEmpty() {
            assertThatThrownBy(() -> validator.validateUsername(""))
                    .isInstanceOf(EmptyNameException.class);
        }

        @Test
        @DisplayName("Should throw for whitespace-only username")
        void shouldThrowForWhitespaceOnly() {
            assertThatThrownBy(() -> validator.validateUsername("   "))
                    .isInstanceOf(EmptyNameException.class);
        }

        @Test
        @DisplayName("Should throw for username exceeding max length")
        void shouldThrowForTooLong() {
            String longUsername = "a".repeat(51);  // Max is 50

            assertThatThrownBy(() -> validator.validateUsername(longUsername))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("50");
        }

        @Test
        @DisplayName("Should accept username at max length")
        void shouldAcceptMaxLength() {
            String maxLengthUsername = "a".repeat(50);

            String result = validator.validateUsername(maxLengthUsername);
            assertThat(result).hasSize(50);
        }
    }

    // ==================== VALIDATE PASSWORD TESTS ====================

    @Nested
    @DisplayName("validatePassword() Tests")
    class ValidatePasswordTests {

        @Test
        @DisplayName("Should accept valid password")
        void shouldAcceptValidPassword() {
            assertThatCode(() -> validator.validatePassword("password123"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept password at minimum length")
        void shouldAcceptMinLength() {
            assertThatCode(() -> validator.validatePassword("pass12"))  // 6 chars
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw for password too short")
        void shouldThrowForTooShort() {
            assertThatThrownBy(() -> validator.validatePassword("pass"))  // 4 chars
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("6");
        }

        @Test
        @DisplayName("Should throw for null password")
        void shouldThrowForNull() {
            assertThatThrownBy(() -> validator.validatePassword(null))
                    .isInstanceOf(WeakPasswordException.class);
        }

        @Test
        @DisplayName("Should throw for password exceeding max length")
        void shouldThrowForTooLong() {
            String longPassword = "a".repeat(129);  // Max is 128

            assertThatThrownBy(() -> validator.validatePassword(longPassword))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("128");
        }

        @Test
        @DisplayName("Should accept password at max length")
        void shouldAcceptMaxLength() {
            String maxLengthPassword = "a".repeat(128);

            assertThatCode(() -> validator.validatePassword(maxLengthPassword))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== VALIDATE PAGINATION TESTS ====================

    @Nested
    @DisplayName("validatePagination() Tests")
    class ValidatePaginationTests {

        @Test
        @DisplayName("Should return sanitized pagination params")
        void shouldReturnSanitizedParams() {
            ServiceValidator.PaginationParams result = validator.validatePagination(2, 20);

            assertThat(result.page()).isEqualTo(2);
            assertThat(result.pageSize()).isEqualTo(20);
            assertThat(result.offset()).isEqualTo(20);  // (2-1) * 20
        }

        @Test
        @DisplayName("Should enforce minimum page of 1")
        void shouldEnforceMinPage() {
            ServiceValidator.PaginationParams result = validator.validatePagination(0, 10);

            assertThat(result.page()).isEqualTo(1);
            assertThat(result.offset()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should enforce minimum page size of 1")
        void shouldEnforceMinPageSize() {
            ServiceValidator.PaginationParams result = validator.validatePagination(1, 0);

            assertThat(result.pageSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should enforce maximum page size of 100")
        void shouldEnforceMaxPageSize() {
            ServiceValidator.PaginationParams result = validator.validatePagination(1, 200);

            assertThat(result.pageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should calculate correct offset")
        void shouldCalculateCorrectOffset() {
            ServiceValidator.PaginationParams result = validator.validatePagination(3, 15);

            assertThat(result.offset()).isEqualTo(30);  // (3-1) * 15
        }

        @Test
        @DisplayName("Should handle negative page number")
        void shouldHandleNegativePage() {
            ServiceValidator.PaginationParams result = validator.validatePagination(-5, 10);

            assertThat(result.page()).isEqualTo(1);
            assertThat(result.offset()).isEqualTo(0);
        }
    }

    // ==================== SANITIZE SEARCH TERM TESTS ====================

    @Nested
    @DisplayName("sanitizeSearchTerm() Tests")
    class SanitizeSearchTermTests {

        @Test
        @DisplayName("Should return trimmed search term")
        void shouldTrimSearchTerm() {
            String result = validator.sanitizeSearchTerm("  test  ");
            assertThat(result).isEqualTo("test");
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNull() {
            String result = validator.sanitizeSearchTerm(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for empty string")
        void shouldReturnNullForEmpty() {
            String result = validator.sanitizeSearchTerm("");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for whitespace-only string")
        void shouldReturnNullForWhitespace() {
            String result = validator.sanitizeSearchTerm("   ");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should throw for search term exceeding max length")
        void shouldThrowForTooLong() {
            String longTerm = "a".repeat(101);  // Max is 100

            assertThatThrownBy(() -> validator.sanitizeSearchTerm(longTerm))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("Should accept search term at max length")
        void shouldAcceptMaxLength() {
            String maxLengthTerm = "a".repeat(100);

            String result = validator.sanitizeSearchTerm(maxLengthTerm);
            assertThat(result).hasSize(100);
        }
    }

    // ==================== VALIDATE NAME TESTS ====================

    @Nested
    @DisplayName("validateName() Tests")
    class ValidateNameTests {

        @Test
        @DisplayName("Should accept valid name")
        void shouldAcceptValidName() {
            String result = validator.validateName("The Witcher 3", "Game name");
            assertThat(result).isEqualTo("The Witcher 3");
        }

        @Test
        @DisplayName("Should trim whitespace from name")
        void shouldTrimWhitespace() {
            String result = validator.validateName("  Game Name  ", "Game name");
            assertThat(result).isEqualTo("Game Name");
        }

        @Test
        @DisplayName("Should throw for null name")
        void shouldThrowForNull() {
            assertThatThrownBy(() -> validator.validateName(null, "Game name"))
                    .isInstanceOf(EmptyNameException.class)
                    .hasMessageContaining("Game name");
        }

        @Test
        @DisplayName("Should throw for empty name")
        void shouldThrowForEmpty() {
            assertThatThrownBy(() -> validator.validateName("", "Movie name"))
                    .isInstanceOf(EmptyNameException.class);
        }

        @Test
        @DisplayName("Should throw for whitespace-only name")
        void shouldThrowForWhitespace() {
            assertThatThrownBy(() -> validator.validateName("   ", "Book name"))
                    .isInstanceOf(EmptyNameException.class);
        }

        @Test
        @DisplayName("Should throw for name exceeding max length")
        void shouldThrowForTooLong() {
            String longName = "a".repeat(256);  // Max is 255

            assertThatThrownBy(() -> validator.validateName(longName, "Show name"))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("255");
        }

        @Test
        @DisplayName("Should accept name at max length")
        void shouldAcceptMaxLength() {
            String maxLengthName = "a".repeat(255);

            String result = validator.validateName(maxLengthName, "Game name");
            assertThat(result).hasSize(255);
        }

        @Test
        @DisplayName("Should include field name in exception message")
        void shouldIncludeFieldNameInException() {
            assertThatThrownBy(() -> validator.validateName("", "Custom Field"))
                    .isInstanceOf(EmptyNameException.class)
                    .hasMessageContaining("Custom Field");
        }
    }
}