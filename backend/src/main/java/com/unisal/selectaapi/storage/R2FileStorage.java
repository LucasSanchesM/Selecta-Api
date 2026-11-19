package com.unisal.selectaapi.storage;

import java.net.URI;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.unisal.selectaapi.exception.BusinessException;

import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class R2FileStorage implements FileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(R2FileStorage.class);
    private static final String PRIVATE_RESUMES = "private-resumes/";
    private static final String PRIVATE_DOCUMENTS = "private-documents/";

    private final String endpoint;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String bucket;
    private final String resumeBucket;
    private final String publicBaseUrl;
    private final String region;
    private volatile S3Client client;

    public R2FileStorage(
            @Value("${r2.endpoint:}") String endpoint,
            @Value("${r2.access-key-id:}") String accessKeyId,
            @Value("${r2.secret-access-key:}") String secretAccessKey,
            @Value("${r2.bucket:}") String bucket,
            @Value("${r2.resume-bucket:}") String resumeBucket,
            @Value("${r2.public-base-url:}") String publicBaseUrl,
            @Value("${r2.region:auto}") String region) {
        this.endpoint = endpoint.trim();
        this.accessKeyId = accessKeyId.trim();
        this.secretAccessKey = secretAccessKey.trim();
        this.bucket = bucket.trim();
        this.resumeBucket = resumeBucket.trim();
        this.publicBaseUrl = removeTrailingSlash(publicBaseUrl.trim());
        this.region = region.isBlank() ? "auto" : region.trim();
    }

    @Override
    public StoredFile store(String directory, String fileName, byte[] content, String contentType) {
        ensureConfigured();
        String key = directory + "/" + fileName;
        return put(bucket, key, publicBaseUrl + "/" + key, content, contentType);
    }

    @Override
    public StoredFile storeResume(UUID userId, byte[] content) {
        requireResumeBucket();
        String fileName = UUID.randomUUID() + ".pdf";
        String key = PRIVATE_RESUMES + userId + "/" + fileName;
        return put(resumeBucket, key, "/candidate-profile/" + userId + "/resume/" + fileName,
                content, "application/pdf");
    }

    @Override
    public byte[] readResume(String key) {
        return readPrivateObject(key, "currículo");
    }

    @Override
    public StoredFile storePrivate(String directory, String fileName, byte[] content, String contentType) {
        requireResumeBucket();
        String key = PRIVATE_DOCUMENTS + directory + "/" + fileName;
        return put(resumeBucket, key, null, content, contentType);
    }

    @Override
    public byte[] readPrivate(String key) {
        if (key == null || !key.startsWith(PRIVATE_DOCUMENTS)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Arquivo não disponível");
        }
        return readPrivateObject(key, "arquivo");
    }

    private byte[] readPrivateObject(String key, String label) {
        requireResumeBucket();
        try {
            return getClient().getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(resumeBucket).key(key).build()).asByteArray();
        } catch (S3Exception exception) {
            logS3Error("Falha ao ler " + label + " do storage", exception);
            throw new BusinessException(
                    exception.statusCode() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY,
                    "Não foi possível obter o " + label);
        }
    }

    @Override
    public void deleteQuietly(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            getClient().deleteObject(DeleteObjectRequest.builder()
                    .bucket(key.startsWith(PRIVATE_RESUMES) || key.startsWith(PRIVATE_DOCUMENTS) ? resumeBucket : bucket)
                    .key(key)
                    .build());
        } catch (RuntimeException exception) {
            LOGGER.warn("Não foi possível remover o arquivo antigo do storage: key={}", key, exception);
        }
    }

    private StoredFile put(String targetBucket, String key, String url, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(targetBucket)
                .key(key)
                .contentType(contentType)
                .contentLength((long) content.length)
                .build();

        try {
            getClient().putObject(request, RequestBody.fromBytes(content));
            return new StoredFile(key, url);
        } catch (S3Exception exception) {
            logS3Error("Falha ao enviar arquivo ao storage (bucket=" + targetBucket + ")", exception);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Não foi possível armazenar o arquivo");
        } catch (RuntimeException exception) {
            LOGGER.error("Falha ao enviar arquivo ao storage (bucket={})", targetBucket, exception);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Não foi possível armazenar o arquivo");
        }
    }

    private void logS3Error(String message, S3Exception exception) {
        String code = exception.awsErrorDetails() != null ? exception.awsErrorDetails().errorCode() : "-";
        String detail = exception.awsErrorDetails() != null ? exception.awsErrorDetails().errorMessage() : "-";
        LOGGER.error("{}: status={} code={} msg={} endpoint={} region={}",
                message, exception.statusCode(), code, detail, endpoint, region);
    }

    private S3Client getClient() {
        S3Client current = client;
        if (current == null) {
            synchronized (this) {
                current = client;
                if (current == null) {
                    current = S3Client.builder()
                            .endpointOverride(URI.create(endpoint))
                            .region(Region.of(region))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                            .serviceConfiguration(S3Configuration.builder()
                                    .pathStyleAccessEnabled(true)
                                    .build())
                            .build();
                    client = current;
                }
            }
        }
        return current;
    }

    private void requireResumeBucket() {
        if (endpoint.isBlank() || accessKeyId.isBlank() || secretAccessKey.isBlank()
                || resumeBucket.isBlank() || resumeBucket.equals(bucket)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure um bucket privado para currículos");
        }
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Upload de arquivos não foi configurado");
        }
    }

    private boolean isConfigured() {
        return !endpoint.isBlank()
                && !accessKeyId.isBlank()
                && !secretAccessKey.isBlank()
                && !bucket.isBlank()
                && !publicBaseUrl.isBlank();
    }

    private String removeTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @PreDestroy
    void close() {
        S3Client current = client;
        if (current != null) {
            current.close();
        }
    }
}