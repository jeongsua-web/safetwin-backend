package com.safetwin.analysis.service;

import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L; // 20MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/heic", "image/heif"
    );

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    public String upload(MultipartFile file, Long userId) {
        validateFile(file);

        String extension = resolveExtension(file.getContentType());
        String key = "analyses/%d/%s%s".formatted(userId, UUID.randomUUID(), extension);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);

        } catch (IOException e) {
            log.error("S3 upload failed for key={}", key, e);
            throw new SafeTwinException(ErrorCode.ANALYSIS_FAILED);
        }
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        try {
            // URL 형식: https://{bucket}.s3.{region}.amazonaws.com/{key}
            String key = imageUrl.substring(imageUrl.indexOf(".amazonaws.com/") + ".amazonaws.com/".length());
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (Exception e) {
            log.warn("S3 delete failed for url={}", imageUrl, e);
        }
    }

    public String uploadBytes(byte[] data, String key, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) data.length)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(data));
            return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
        } catch (Exception e) {
            log.error("S3 bytes upload failed for key={}", key, e);
            throw new SafeTwinException(ErrorCode.ANALYSIS_FAILED);
        }
    }

    public String uploadMultipart(MultipartFile file, String folder) {
        String extension = resolveExtension(file.getContentType());
        String key = "%s/%s%s".formatted(folder, UUID.randomUUID(), extension);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
        } catch (IOException e) {
            log.error("S3 upload failed for key={}", key, e);
            throw new SafeTwinException(ErrorCode.ANALYSIS_FAILED);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new SafeTwinException(ErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new SafeTwinException(ErrorCode.ANALYSIS_FAILED);
        }
    }

    private String resolveExtension(String contentType) {
        if (contentType == null) return ".jpg";
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/heic", "image/heif" -> ".heic";
            default -> ".jpg";
        };
    }
}
