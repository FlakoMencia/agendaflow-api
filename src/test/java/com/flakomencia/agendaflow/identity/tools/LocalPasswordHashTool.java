package com.flakomencia.agendaflow.identity.tools;

import java.io.Console;
import java.util.Arrays;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Development-only helper. Run this test-scope main class from IntelliJ with terminal emulation enabled.
 */
public final class LocalPasswordHashTool {
    private LocalPasswordHashTool() {
    }

    public static void main(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("Do not pass a password as a command-line argument");
        }
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("A secure console is required; enable terminal emulation in the IntelliJ run configuration");
        }
        char[] password = console.readPassword("Password to hash: ");
        char[] confirmation = console.readPassword("Confirm password: ");
        try {
            if (password.length == 0 || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Passwords are empty or do not match");
            }
            console.writer().println(new BCryptPasswordEncoder().encode(new String(password)));
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }
}
