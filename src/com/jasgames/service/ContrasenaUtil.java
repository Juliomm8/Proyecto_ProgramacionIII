package com.jasgames.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class ContrasenaUtil {

    private static final SecureRandom random = new SecureRandom();

    public static String generarSaltBase64() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPasswordBase64(String passwordPlano, String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(passwordPlano.getBytes(StandardCharsets.UTF_8));

            byte[] digest = md.digest();
            return Base64.getEncoder().encodeToString(digest);

        } catch (Exception e) {
            throw new RuntimeException("Error al hashear password", e);
        }
    }

    public static boolean verificar(String passwordPlano, String saltBase64, String hashEsperadoBase64) {
        String hashActual = hashPasswordBase64(passwordPlano, saltBase64);
        return hashActual.equals(hashEsperadoBase64);
    }
}
