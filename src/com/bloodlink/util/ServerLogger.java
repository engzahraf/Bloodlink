package com.bloodlink.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standardized thread-safe console logger for the BloodLink server.
 * Prevents leaking passwords and sensitive tokens into console logs.
 */
public final class ServerLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Pattern SENSITIVE_KEY_PATTERN =
            Pattern.compile("(?i)(password|pass|secret|token|credential)=([^&|;\\s]+)");

    private ServerLogger() {
        // Utility class
    }

    public static void info(String message) {
        log("INFO", message, null);
    }

    public static void info(String message, Object... args) {
        log("INFO", String.format(message, args), null);
    }

    public static void warn(String message) {
        log("WARN", message, null);
    }

    public static void warn(String message, Object... args) {
        log("WARN", String.format(message, args), null);
    }

    public static void warn(String message, Throwable throwable) {
        log("WARN", message, throwable);
    }

    public static void error(String message) {
        log("ERROR", message, null);
    }

    public static void error(String message, Object... args) {
        log("ERROR", String.format(message, args), null);
    }

    public static void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
    }

    public static void debug(String message) {
        log("DEBUG", message, null);
    }

    public static void debug(String message, Object... args) {
        log("DEBUG", String.format(message, args), null);
    }

    private static void log(String level, String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String threadName = Thread.currentThread().getName();
        String safeMessage = sanitize(message);

        String line = String.format("[%s] [%s] [%s] %s", timestamp, threadName, level, safeMessage);
        if ("ERROR".equals(level)) {
            System.err.println(line);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        } else {
            System.out.println(line);
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }

    /**
     * Sanitizes sensitive key-value pairs (passwords, secrets) from logs.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "null";
        }
        Matcher matcher = SENSITIVE_KEY_PATTERN.matcher(input);
        return matcher.replaceAll("$1=***PROTECTED***");
    }
}
