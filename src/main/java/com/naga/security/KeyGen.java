package com.naga.security;

import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Base64;

/* Util class to generate and validate a valid secret jwt token */
public class KeyGen {
    public static void main(String[] args) {
        byte[] key = Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
        String secret = Base64.getEncoder().encodeToString(key);
        System.out.println(secret);
        Base64.getDecoder().decode(secret);
        System.out.println("Valid Base64 secret");
    }
}
