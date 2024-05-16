package com.example;

import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;


public class AWS {
    private final S3Client s3;
    private final SqsClient sqs;
    private final Ec2Client ec2;

    public static String ami = "ami-00e95a9222311e8ed";
    public static Region region1 = Region.US_WEST_2;
    public static Region region2 = Region.US_EAST_1;

    
    private String s3Url = "";

    private static final AWS instance = new AWS();

    private AWS() {
        s3 = S3Client.builder().region(region1).build();
        sqs = SqsClient.builder().region(region1).build();
        ec2 = Ec2Client.builder().region(region2).build();
    }

    public static AWS getInstance() {
        return instance;
    }

    public Ec2Client getEC2Client(){
        return this.ec2;
    }

    public SqsClient getSqsClient(){
        return this.sqs;
    }

    public S3Client getS3(){
        return this.s3;
    }

    public String getS3Url(){
        return this.s3Url;
    }

    // S3
    public void createBucketIfNotExists(String bucketName) {
        try {
            s3.createBucket(CreateBucketRequest
                    .builder()
                    .bucket(bucketName)
                    .createBucketConfiguration(
                            CreateBucketConfiguration.builder()
                                    .locationConstraint(BucketLocationConstraint.US_WEST_2)
                                    .build())
                    .build());
            s3.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
        } catch (S3Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // EC2
    public String createEC2(String script, String tagName, int numberOfInstances) {

        RunInstancesRequest runRequest = (RunInstancesRequest) RunInstancesRequest.builder()
                .instanceType(InstanceType.M4_LARGE)
                .imageId(ami)
                .maxCount(numberOfInstances)
                .minCount(1)
                .keyName("vockey")
                .iamInstanceProfile(IamInstanceProfileSpecification.builder().name("LabInstanceProfile").build())
                .userData(Base64.getEncoder().encodeToString((script).getBytes()))
                .build();


        RunInstancesResponse response = ec2.runInstances(runRequest);

        String instanceId = response.instances().get(0).instanceId();

        software.amazon.awssdk.services.ec2.model.Tag tag = Tag.builder()
                .key("Name")
                .value(tagName)
                .build();

        CreateTagsRequest tagRequest = (CreateTagsRequest) CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();

        try {
            ec2.createTags(tagRequest);
            System.out.printf(
                    "[DEBUG] Successfully started EC2 instance %s based on AMI %s\n",
                    instanceId, ami);

        } catch (Ec2Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            System.exit(1);
        }
        return instanceId;
    }
public String createSqsQueue(String queueName) {
    // Define FIFO queue name with .fifo suffix
    String fifoQueueName = queueName + ".fifo";

    // Define FIFO queue attributes
    Map<QueueAttributeName, String> attributes = Collections.singletonMap(QueueAttributeName.FIFO_QUEUE, "true");

    // Create FIFO queue request
    CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
            .queueName(fifoQueueName)
            .attributes(attributes)
            .build();

    // Create FIFO queue
    CreateQueueResponse createQueueResponse = sqs.createQueue(createQueueRequest);

    // Print the created queue URL
    System.out.println("[AWS] FIFO queue created: " + createQueueResponse.queueUrl());
    return fifoQueueName;
}


    public String uploadFile(String bucketName, String key, Path filePath) {
        try {
            s3.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build(),
                    RequestBody.fromFile(filePath));

            // Construct the S3 URL
            System.out.println("File uploaded successfully to S3!");
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region2, key);
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            return "";
        }
    }
}
