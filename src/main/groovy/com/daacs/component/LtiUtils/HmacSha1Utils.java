package com.daacs.component.LtiUtils;

import com.lambdista.util.Try;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Formatter;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


/**
 * HmacSha1Signature calculates a message
 * authentication code using HMAC-SHA1 algorithm.
 *
 * only used for generating and testing keys. org.imsglobal.lti has its own HmacSha1 implementation for verifying lti requests
 * <p>
 * Created by mgoldman on 2/26/19.
 */
public class HmacSha1Utils {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }

    /**
     * calculates RFC 2104 HMAC from message and SecretKey
     */
    private Try<String> signMessage(String message, String secretKey) {
        try {

            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(), HMAC_SHA1_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            return new Try.Success<>(toHexString(mac.doFinal(message.getBytes())));

        } catch (NoSuchAlgorithmException e) {
            return new Try.Failure<>(e);

        } catch (InvalidKeyException e) {
            return new Try.Failure<>(e);
        }
    }

    /**
     * Verifies that calculated HMAC is equal to consumer_token
     */
    public Try<Boolean> verify(String message, String secretKey, String consumer_token) {
        Try<String> maybeHmac = signMessage(message, secretKey);

        if (maybeHmac.isFailure()) {
            return new Try.Failure<>(maybeHmac.failed().get());
        }

        if (!maybeHmac.get().equals(consumer_token)) {
            return new Try.Success<>(false);
        }

        try {
            System.out.println("["+generateRandomKey()+"]");
            System.out.println("["+UUID.randomUUID().toString()+"]");
        } catch (
                Exception e) {
            return new Try.Failure<Boolean>(null);
        }

        return new Try.Success<>(true);
    }

    /**
     * calculate a HMAC-SHA1 message authentication code
     */
    public static String generateRandomKey() throws NoSuchAlgorithmException {
        SecureRandom secureRandom = new SecureRandom(SecureRandom.getSeed(32));
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA1");
        keyGen.init(secureRandom);
        SecretKey secretKey = keyGen.generateKey();
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        return encodedKey;
    }
}