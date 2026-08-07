package com.flakomencia.agendaflow.identity.application;

import java.security.SecureRandom;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class JwtIdentifierGenerator {
    private final SecureRandom secureRandom;

    public JwtIdentifierGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String next() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
