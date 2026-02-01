package md.pandatur.filesharingapp.storage.cloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class R2CloudStorageService implements CloudStorageService {

    @Value("${r2.access_key}")
    private String accessKey;

    @Value("${r2.secret_token}")
    private String secretToken;

    @Value("${r2.account_id}")
    private String accountId;

    @Value("${r2.main_bucket_name}")
    private String bucketName;

    @Value("${r2.public_domain}")
    private String publicDomain;

    private final RestClient restClient;

    public R2CloudStorageService(@Value("${r2.account_id}") final String accountId) {
        this.restClient = RestClient.builder()
                .baseUrl("https://" + accountId + ".r2.cloudflarestorage.com")
                .build();
    }

    @Override
    public String uploadFile(final MultipartFile file) {
        String fileName = null;
        try {
            fileName = generateFileName(file.getOriginalFilename());
            final byte[] fileBytes = file.getBytes();

            if (fileBytes.length == 0) {
                throw new RuntimeException("File is empty");
            }

            final String contentType = file.getContentType();

            log.info("Uploading file: {}, size: {}, type: {}", fileName, fileBytes.length, contentType);

            final var authHeaders = CloudflareR2Utils.createR2AuthHeaders(
                    "PUT", fileName, contentType, fileBytes, accessKey, secretToken, accountId, bucketName
            );

            this.restClient.put()
                    .uri("/{bucket}/{key}", bucketName, fileName)
                    .headers(headers -> headers.addAll(authHeaders))
                    .body(fileBytes)
                    .retrieve()
                    .toBodilessEntity();

            final String publicUrl = String.format("%s/%s", publicDomain, fileName);
            log.info("Upload successful: {}", publicUrl);

            return publicUrl;
        } catch (Exception e) {
            log.error("Upload failed for file: {} - Error: {}", fileName, e.getMessage());
            throw new RuntimeException("Failed to upload file: " + fileName, e);
        }
    }

    @Override
    public void deleteFile(final String url) {
        try {
            final String fileName = url.substring(this.publicDomain.length() + 1);
            log.info("Deleting file: {}", fileName);

            final var authHeaders = CloudflareR2Utils.createR2AuthHeaders(
                    "DELETE",
                    fileName,
                    null,
                    null,
                    this.accessKey,
                    this.secretToken,
                    this.accountId,
                    this.bucketName
            );

            this.restClient.delete()
                    .uri("/{bucket}/{key}", this.bucketName, fileName)
                    .headers(headers -> headers.addAll(authHeaders))
                    .retrieve()
                    .toBodilessEntity();

            log.info("File deleted successfully: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to delete file from URL: {}", url);
        }
    }

    private String generateFileName(final String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return UUID.randomUUID().toString();
        }

        final String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        return UUID.randomUUID() + extension;
    }
}
