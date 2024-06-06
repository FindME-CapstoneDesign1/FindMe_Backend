package com.findme.FindMeAI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.findme.FindMeBack.Config", "com.findme.FindMeAI"})  // S3Config 패키지 추가
public class S3GetImageUrl implements CommandLineRunner {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Autowired
    private AmazonS3Client s3Client;

    public static void main(String[] args) {
        SpringApplication.run(S3GetImageUrl.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ImageProcessor imageProcessor = new ImageProcessor(s3Client, bucketName);
        imageProcessor.processImage();
    }
}

class ImageProcessor {

    private AmazonS3Client s3Client;
    private String bucketName;

    public ImageProcessor(AmazonS3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void processImage() {
        String imageUrl = getMostRecentImageUrl();
        if (imageUrl != null) {
            String flaskResponse = sendImageUrlToFlask(imageUrl);
            System.out.println("Flask Server Response: " + flaskResponse);
        } else {
            System.out.println("No objects found in the S3 bucket.");
        }
    }

    private String getMostRecentImageUrl() {
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result result = s3Client.listObjectsV2(req);
        List<S3ObjectSummary> objects = result.getObjectSummaries();

        if (objects.isEmpty()) {
            return null;  // Return null if no objects are found
        }

        S3ObjectSummary mostRecent = objects.stream()
                .max(Comparator.comparing(S3ObjectSummary::getLastModified))
                .orElseThrow(() -> new RuntimeException("No objects found in the bucket"));

        return s3Client.getUrl(bucketName, mostRecent.getKey()).toString();
    }

    private String sendImageUrlToFlask(String imageUrl) {
        try {
            // URL 인코딩 추가
            String encodedImageUrl = URLEncoder.encode(imageUrl, StandardCharsets.UTF_8.toString());

            URL url = new URL("http://127.0.0.1:5000/process_image");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{\"image_url\": \"" + encodedImageUrl + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            conn.disconnect();
            return response.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
