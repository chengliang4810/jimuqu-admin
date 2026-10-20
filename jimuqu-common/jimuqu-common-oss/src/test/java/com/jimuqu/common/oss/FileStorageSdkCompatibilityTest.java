package com.jimuqu.common.oss;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.junit.jupiter.api.Test;
import org.dromara.x.file.storage.core.platform.MinioFileStorage;
import org.dromara.x.file.storage.core.platform.FileStorageClientFactory;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用合成凭据做离线签名，覆盖 HTTP/Kotlin/S3 依赖的真实调用路径。 */
class FileStorageSdkCompatibilityTest {
    @Test
    void minioSignsUsingTheUpgradedOkHttpAndKotlinRuntime() throws Exception {
        MinioClient client = MinioClient.builder().endpoint("http://127.0.0.1:19999")
                .credentials("compat-access", "compat-secret").region("us-east-1").build();
        String url = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET).bucket("compat-bucket").object("test.txt").expiry(60).build());
        assertTrue(url.contains("X-Amz-Signature="));
    }

    @Test
    void fileStorageAdapterCanGenerateMinioPresignedUrls() {
        MinioClient client = MinioClient.builder().endpoint("http://127.0.0.1:19999")
                .credentials("compat-access", "compat-secret").region("us-east-1").build();
        MinioFileStorage storage = new MinioFileStorage();
        storage.setPlatform("compat-minio");
        storage.setBucketName("compat-bucket");
        storage.setBasePath("");
        storage.setClientFactory(new FileStorageClientFactory<MinioClient>() {
            public String getPlatform() { return "compat-minio"; }
            public MinioClient getClient() { return client; }
        });
        String url = storage.generatePresignedUrl(new GeneratePresignedUrlPretreatment()
                .setPlatform("compat-minio").setPath("").setFilename("test.txt").setMethod("GET")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))).getUrl();
        assertTrue(url.contains("X-Amz-Signature="));
    }

    @Test
    void s3SignsUsingTheUpgradedAwsSdk() {
        try (S3Presigner presigner = S3Presigner.builder().region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("compat-access", "compat-secret"))).build()) {
            String url = presigner.presignGetObject(builder -> builder.signatureDuration(Duration.ofMinutes(1))
                    .getObjectRequest(GetObjectRequest.builder().bucket("compat-bucket").key("test.txt").build()))
                    .url().toString();
            assertTrue(url.contains("X-Amz-Signature="));
        }
    }
}
