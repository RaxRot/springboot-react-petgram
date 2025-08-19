package com.raxrot.back.services.impl;

import com.raxrot.back.exceptions.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileUploadServiceImplTest {

    private S3Client s3Client;
    private FileUploadServiceImpl fileUploadService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        fileUploadService = new FileUploadServiceImpl(s3Client);

        ReflectionTestUtils.setField(fileUploadService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(fileUploadService, "region", "eu-west-1");
    }

    @Test
    void uploadFile_ShouldReturnUrl_WhenUploadSuccessful() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "dummy".getBytes()
        );

        PutObjectResponse mockResponse = mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse())
                .thenReturn(SdkHttpResponse.builder().statusCode(200).build());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        String result = fileUploadService.uploadFile(file);

        assertTrue(result.startsWith("https://test-bucket.s3.eu-west-1.amazonaws.com/"));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_ShouldThrowException_WhenUploadFails() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "dummy".getBytes()
        );

        PutObjectResponse mockResponse = mock(PutObjectResponse.class);
        when(mockResponse.sdkHttpResponse())
                .thenReturn(SdkHttpResponse.builder().statusCode(500).build());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        ApiException ex = assertThrows(ApiException.class, () -> fileUploadService.uploadFile(file));
        assertEquals("Failed to upload file to S3", ex.getMessage());
    }

    @Test
    void uploadFile_ShouldThrowException_WhenFileNameInvalid() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invalidfile", "image/jpeg", "dummy".getBytes()
        );

        ApiException ex = assertThrows(ApiException.class, () -> fileUploadService.uploadFile(file));
        assertEquals("Invalid file name", ex.getMessage());
        verifyNoInteractions(s3Client);
    }

    @Test
    void deleteFile_ShouldCallDeleteObjectWithCorrectKey() {
        String imageUrl = "https://test-bucket.s3.eu-west-1.amazonaws.com/myfile.jpg";

        fileUploadService.deleteFile(imageUrl);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest request = captor.getValue();
        assertEquals("myfile.jpg", request.key());
        assertEquals("test-bucket", request.bucket());
    }
}