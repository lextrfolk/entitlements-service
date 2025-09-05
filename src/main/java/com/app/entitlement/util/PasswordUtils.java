package com.app.entitlement.util;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PasswordUtils {

    private PasswordUtils() {}

    public static List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            errors.add("Password cannot be empty.");
            return errors;
        }

        for (PasswordRule rule : PasswordRule.values()) {
            if (!rule.matches(password)) {
                errors.add(rule.getMessage());
            }
        }

        return errors;
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.isEmpty()) return false;
        for (PasswordRule rule : PasswordRule.values()) {
            if (!rule.matches(password)) return false; // fail fast
        }
        return true;
    }


    @Getter
    public enum PasswordRule {

        MIN_LENGTH(".{8,}", "At least 8 characters."),
        UPPERCASE(".*[A-Z].*", "At least one uppercase letter."),
        LOWERCASE(".*[a-z].*", "At least one lowercase letter."),
        DIGIT(".*\\d.*", "At least one digit."),
        SPECIAL(".*[@$!%*?&].*", "At least one special character (@$!%*?&).");

        private final Pattern pattern;
        private final String regex;
        private final String message;

        PasswordRule(String regex, String message) {
            this.pattern = Pattern.compile(regex);
            this.regex = regex;
            this.message = message;
        }
        public boolean matches(String input) {
            return pattern.matcher(input).matches();
        }

        /** Returns all rules in insertion order */
        public static Map<String, String> getAllRules() {
            Map<String, String> map = new LinkedHashMap<>();
            for (PasswordRule rule : values()) {
                map.put(rule.regex, rule.message);
            }
            return map;
        }
    }

}
