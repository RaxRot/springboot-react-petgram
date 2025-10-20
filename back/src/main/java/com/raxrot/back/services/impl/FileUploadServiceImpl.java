package com.raxrot.back.services.impl;

import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.services.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${aws.bucket.name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    private final S3Client s3Client;

    public FileUploadServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        log.info("Starting file upload: original filename='{}'", originalFilename);

        if (originalFilename == null || !originalFilename.contains(".")) {
            log.warn("Invalid file name provided: '{}'", originalFilename);
            throw new ApiException("Invalid file name", HttpStatus.BAD_REQUEST);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
        String key = UUID.randomUUID() + "." + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .contentType(file.getContentType())
                    .build();

            log.debug("Uploading file to S3: bucket='{}', key='{}', contentType='{}'", bucketName, key, file.getContentType());

            PutObjectResponse response = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(file.getBytes())
            );

            if (response.sdkHttpResponse().isSuccessful()) {
                String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
                log.info("File uploaded successfully to S3. URL={}", url);
                return url;
            } else {
                log.error("Failed to upload file to S3 (HTTP status: {})", response.sdkHttpResponse().statusCode());
                throw new ApiException("Failed to upload file to S3", HttpStatus.BAD_REQUEST);
            }

        } catch (IOException e) {
            log.error("Error reading file bytes for '{}': {}", originalFilename, e.getMessage(), e);
            throw new RuntimeException("Error reading file bytes", e);
        }
    }

    @Override
    public void deleteFile(String imageUrl) {
        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        log.info("Attempting to delete file '{}' from bucket '{}'", fileName, bucketName);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        try {
            s3Client.deleteObject(deleteObjectRequest);
            log.info("File '{}' successfully deleted from S3 bucket '{}'", fileName, bucketName);
        } catch (Exception e) {
            log.error("Failed to delete file '{}' from S3 bucket '{}': {}", fileName, bucketName, e.getMessage(), e);
            throw new ApiException("Failed to delete file from S3", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
