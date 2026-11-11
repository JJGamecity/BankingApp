package com.example.bankingapp.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.codePointCount(0, value.length()) < 10
                || value.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        boolean special = value.codePoints().anyMatch(PasswordValidator::isSpecial);
        // Ignore whitespace for sequence detection only; never alter the stored password.
        int[] characters = value.codePoints()
                .filter(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))
                .map(Character::toLowerCase).toArray();
        for (int i = 2; i < characters.length; i++) {
            int a = characters[i - 2], b = characters[i - 1], c = characters[i];
            boolean sameAlphabet = (isDigit(a) && isDigit(b) && isDigit(c))
                    || (isLetter(a) && isLetter(b) && isLetter(c));
            if (sameAlphabet && ((b - a == 1 && c - b == 1) || (a - b == 1 && b - c == 1))) {
                return false;
            }
        }
        return special;
    }

    private static boolean isDigit(int c) { return c >= '0' && c <= '9'; }
    private static boolean isLetter(int c) { return c >= 'a' && c <= 'z'; }

    private static boolean isSpecial(int c) {
        return switch (Character.getType(c)) {
            case Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION,
                    Character.START_PUNCTUATION, Character.END_PUNCTUATION,
                    Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
                    Character.OTHER_PUNCTUATION, Character.MATH_SYMBOL,
                    Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL,
                    Character.OTHER_SYMBOL -> true;
            default -> false;
        };
    }
}
