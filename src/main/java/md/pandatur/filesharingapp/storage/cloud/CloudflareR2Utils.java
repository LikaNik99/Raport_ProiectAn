package md.pandatur.filesharingapp.storage.cloud;

import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CloudflareR2Utils {

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String REGION = "auto";
    private static final String SERVICE = "s3";

    public static HttpHeaders createR2AuthHeaders(
            final String method,
            final String key,
            final String contentType,
            final byte[] content,
            final String accessKey,
            final String secretToken,
            final String accountId,
            final String bucketName
    ) {
        try {
            final String timestamp = Instant.now().atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
            final String date = timestamp.substring(0, 8);
            final String host = accountId + ".r2.cloudflarestorage.com";
            final byte[] payload = content != null ? content : new byte[0];
            final String payloadHash = sha256Hash(payload);

            // Build canonical request
            final String canonicalUri = "/" + bucketName + "/" + key;
            final String canonicalHeaders = buildCanonicalHeaders(host, contentType, payloadHash, timestamp);
            final String signedHeaders = contentType != null
                    ? "content-type;host;x-amz-content-sha256;x-amz-date"
                    : "host;x-amz-content-sha256;x-amz-date";

            final String canonicalRequest = method + "\n" +
                    canonicalUri + "\n" +
                    "\n" + // empty query string
                    canonicalHeaders + "\n" +
                    signedHeaders + "\n" +
                    payloadHash;

            // Create string to sign
            final String credentialScope = date + "/" + REGION + "/" + SERVICE + "/aws4_request";
            final String stringToSign = ALGORITHM + "\n" +
                    timestamp + "\n" +
                    credentialScope + "\n" +
                    sha256Hash(canonicalRequest.getBytes(StandardCharsets.UTF_8));

            // Calculate signature
            final byte[] signingKey = getSigningKey(secretToken, date);
            final String signature = bytesToHex(hmacSha256(stringToSign.getBytes(StandardCharsets.UTF_8), signingKey));

            // Build authorization header
            final String authorization = ALGORITHM + " " +
                    "Credential=" + accessKey + "/" + credentialScope + ", " +
                    "SignedHeaders=" + signedHeaders + ", " +
                    "Signature=" + signature;

            // Create headers
            final HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorization);
            headers.set("X-Amz-Date", timestamp);
            headers.set("X-Amz-Content-Sha256", payloadHash);
            headers.set("Host", host);
            if (contentType != null) {
                headers.set("Content-Type", contentType);
            }

            return headers;
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create R2 auth headers", e);
        }
    }

    private static String buildCanonicalHeaders(
            final String host,
            final String contentType,
            final String payloadHash,
            final String timestamp
    ) {
        final StringBuilder headers = new StringBuilder();
        if (contentType != null) {
            headers.append("content-type:").append(contentType).append("\n");
        }
        headers.append("host:").append(host).append("\n");
        headers.append("x-amz-content-sha256:").append(payloadHash).append("\n");
        headers.append("x-amz-date:").append(timestamp).append("\n");
        return headers.toString();
    }

    private static byte[] getSigningKey(final String secretKey, final String date) throws Exception {
        byte[] kDate = hmacSha256(date.getBytes(StandardCharsets.UTF_8),
                ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = hmacSha256(REGION.getBytes(StandardCharsets.UTF_8), kDate);
        byte[] kService = hmacSha256(SERVICE.getBytes(StandardCharsets.UTF_8), kRegion);
        return hmacSha256("aws4_request".getBytes(StandardCharsets.UTF_8), kService);
    }

    private static byte[] hmacSha256(final byte[] data, final byte[] key) throws Exception {
        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static String sha256Hash(final byte[] data) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return bytesToHex(digest.digest(data));
    }

    private static String bytesToHex(final byte[] bytes) {
        final StringBuilder result = new StringBuilder();
        for (final byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
