package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public class App {

    private static String ec2Script =
    "#!/bin/bash\n" +
    "sudo yum update -y \n" +
    "curl -s \"https://get.sdkman.io\" | bash\n" +
    "source \"$HOME/.sdkman/bin/sdkman-init.sh\"\n" +
    "sdk install java 17.0.1-open\n" +
    "sdk use java 17.0.1-open\n" +
    "aws s3 cp s3://script-bucket-inbal-naor/Manager.jar /home/Manager.jar\n" +
    "java -cp /home/Manager.jar com.example.ManagerApp\n";
    final static AWS aws = AWS.getInstance();
    private static ManagerNode managerNode;
    private static int workersPerRevivews = 0;
    private static String bucketName = "naor-inbal1";
    private static List<Path> inputFilePaths;
    private static List<Path> outputFilePaths;
    private static String queueUrl;
    private static String responseQueueUrl;
    private static String queueName;
    private static String responseQueueName;

    public static void main(String[] args) { // args = [inFilePath, outFilePath, tasksPerWorker, -t (terminate,
                                             // optional)]
        try {
            boolean terminate = false;
            inputFilePaths = new ArrayList<>();
            outputFilePaths = new ArrayList<>();

            if (args.length < 3) { // Minimum arguments: 2 files and 1 task count
                System.out.println("Insufficient arguments provided, exiting ...");
                return;
            }

            // Check for the terminate flag
            if (args[args.length - 1].equals("terminate")) {
                terminate = true;
            }

            // Last argument before optional terminate is always the number of tasks per
            // worker
            int nPosition = terminate ? args.length - 2 : args.length - 1;
            workersPerRevivews = Integer.parseInt(args[nPosition]);

            // Assuming equal number of input and output files
            int filesCount = (nPosition) / 2;

            // Collect input and output file paths
            for (int i = 0; i < filesCount; i++) {
                inputFilePaths.add(Path.of(args[i]));
                outputFilePaths.add(Path.of(args[i + filesCount]));
            }

            // Debug output
            System.out.println("[App] Terminate: " + terminate);
            System.out.println("[App] Tasks per worker: " + workersPerRevivews);
            inputFilePaths.forEach(path -> System.out.println("[App] Input file: " + path));
            outputFilePaths.forEach(path -> System.out.println("[App] Output file: " + path));
            setup();
            // Collect input and output file paths
             for (int i = 0; i < filesCount; i++) {
                retrieveDataFromManager();
             }
        } catch (NumberFormatException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Create Buckets, Create Queues, Upload JARs to S3
    private static void setup() {

        int inputCounter = 0;
        System.out.println("[DEBUG] Create bucket if not exist.");
        aws.createBucketIfNotExists(bucketName);
        createSqs();
        int totalFiles = inputFilePaths.size(); // Get the total number of files

        for (Path inputfile : inputFilePaths) {// Upload all of the files to AWS bucket with unique filename
            String newName = "reviews" + UUID.randomUUID().toString();
            String S3Url = aws.uploadFile(bucketName, newName, inputfile);
            // Check if it's the last file
            if (inputCounter == totalFiles - 1) {
                // For the last file, call sendLocationFileTerminate
                sendLocationFileTerminate(inputfile, S3Url, inputCounter);
            } else {
                // For all other files, call sendLocationFile as before
                sendLocationFile( inputfile, S3Url, inputCounter);
            }

            inputCounter++;
        }

        // Initialize ManagerNode instance if not already done
        if (managerNode == null) {
            managerNode = new ManagerNode(AWS.region2, aws.getEC2Client()); // Assuming default constructor
        }

        // Now, use managerNode to activate EC2 Manager
        managerNode.activateEC2Manager(aws, ec2Script);
    }

    private static void sendLocationFile( Path inputPath, String S3Url, int inputCounter) {
        // Send the message to the SQS queue
        String messageDeduplicationId = "dedup-" + inputCounter + "-" + System.currentTimeMillis();

        SendMessageResponse response = aws.getSqsClient().sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(S3Url)
                .messageGroupId("MyMessageGroup")
                .messageDeduplicationId(messageDeduplicationId) 
                .messageAttributes(Map.of(
                        "Response-Sqs-Url", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(responseQueueUrl)
                                .build(),
                        "TaskCount", MessageAttributeValue.builder()
                                .dataType("Number")
                                .stringValue(String.valueOf(workersPerRevivews))
                                .build(),
                        "FileKey", MessageAttributeValue.builder() // Add Index attribute
                                .dataType("Number")
                                .stringValue(String.valueOf(inputCounter))
                                .build()))
                .build());

        // Print the message ID from the response
        System.out.println("[Local] Message sent with ID: " + response.messageId());
    }

    private static void sendLocationFileTerminate( Path inputPath, String S3Url, int inputCounter) {
        // Send the message to the SQS queue

        String messageDeduplicationId = "dedup-" + inputCounter + "-" + System.currentTimeMillis();

        System.out.println(queueUrl);

        SendMessageResponse response = aws.getSqsClient().sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(S3Url)
                .messageGroupId("MyMessageGroup")
                .messageDeduplicationId(messageDeduplicationId) 
                .messageAttributes(Map.of(
                        "Response-Sqs-Url", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(responseQueueUrl)
                                .build(),
                        "TaskCount", MessageAttributeValue.builder()
                                .dataType("Number")
                                .stringValue(String.valueOf(workersPerRevivews))
                                .build(),
                        "FileKey", MessageAttributeValue.builder() 
                                .dataType("Number")
                                .stringValue(String.valueOf(inputCounter))
                                .build(),
                        "terminate", MessageAttributeValue.builder() 
                        .dataType("String")
                        .stringValue("true")
                        .build()))
                .build());

        // Print the message ID from the response
        System.out.println("[Local] Message sent with ID: " + response.messageId());
    }

    private static void createSqs() {

        String randomName = "local_" + UUID.randomUUID().toString().replace("-", "");

        App.responseQueueName = aws.createSqsQueue(randomName);
        responseQueueUrl = aws.getSqsClient().getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(responseQueueName)
                .build())
                .queueUrl();

        App.queueName = aws.createSqsQueue("Sqs-Local-Manager");
        queueUrl = aws.getSqsClient().getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build())
                .queueUrl();

    }

    private static void retrieveDataFromManager() throws IOException {// Get a URL in S3 from Manager of the txt file

        boolean messageReceived = false;
        while (!messageReceived) {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(responseQueueUrl)
                .maxNumberOfMessages(1) // Maximum number of messages to retrieve
                .messageAttributeNames("All") // Request the "Response-Sqs" attribute
                .build();

            ReceiveMessageResponse receiveResponse = aws.getSqsClient().receiveMessage(receiveRequest);
            if (!receiveResponse.messages().isEmpty()) {
                for (Message message : receiveResponse.messages()) {
                    // Process the message
                    messageReceived = true;
                    String S3Url = message.body();
                    System.out.println(S3Url);
                    int key = Integer.parseInt(messageAttribute(message,"keyFile"));
                    String[] urlParts = S3Url.split("/", 4);
                    String[] bucketNameParts = urlParts[2].split("\\.");
                    String bucketName = bucketNameParts[0];
                    String objectKey = urlParts[3];
        
                    // Download the object from S3
                    ResponseInputStream responseInputStream = aws.getS3().getObject(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build());
        
                    Path tempFilePath = Files.createTempFile("temp-" + UUID.randomUUID(), ".tmp");
                    Files.copy(responseInputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
                    convertTextToHtml("" + tempFilePath, "" + outputFilePaths.get(key));

                    // Delete the received message
                    DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                            .queueUrl(responseQueueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build();

                    aws.getSqsClient().deleteMessage(deleteRequest);
                }
            } else {
                System.out.println("[App] Waiting for message...");
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } 
            }
        }
    }

    public static void convertTextToHtml(String inputFilePath, String outputFilePath) throws IOException {

        // Open the input file
        BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));

        // Open the output file
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));

        try {
            // Write HTML header
            writer.write("<!DOCTYPE html>\n<html>\n<head>\n");
            writer.write("<title>Text to HTML Conversion</title>\n");
            writer.write("<style>\n");
            writer.write(".black { color: black; }\n");
            writer.write(".lightgreen { color: lightgreen; }\n");
            writer.write(".darkgreen { color: darkgreen; }\n");
            writer.write(".red { color: red; }\n"); // Add more colors as needed
            writer.write("</style>\n</head>\n<body>\n");

            // Read lines from the input file and process each line
            String line;
            while ((line = reader.readLine()) != null) {
                // Split the line into its components
                String[] parts = line.split(",", 4);
                if (parts.length < 4) continue; // Skip lines that don't match the expected format

                String link = parts[0];
                String color = parts[1];
                String namedEntities = parts[2];
                String sarcasm = parts.length > 3 ? parts[3] : "";

                // Construct the HTML content
                writer.write(String.format("<div style=\"color: %s;\">\n", color));
                writer.write("<h2>Link:</h2>\n");
                writer.write(String.format("<a href=\"%s\">%s</a>\n", link, link));
                writer.write("<p>NamedEntities: " + namedEntities + "</p>\n");
                writer.write("<p>Sarcasm: " + sarcasm + "</p>\n");
                writer.write("</div>\n<br/>\n");
            }

            // Close HTML tags
            writer.write("</body>\n</html>");
        } finally {
            // Ensure resources are closed
            reader.close();
            writer.close();
        }
    }

    // Method to escape HTML special characters
    private static String escapeHtml(String input) {
        input = input.replace("&", "&amp;");
        input = input.replace("<", "&lt;");
        input = input.replace(">", "&gt;");
        input = input.replace("\"", "&quot;");
        input = input.replace("'", "&apos;");
        return input;
    }
    private static String messageAttribute(Message msg, String attribute) {

        // Check if the message has the "KeyFi;e" attribute
        if (msg.messageAttributes().containsKey(attribute)) {
            return msg.messageAttributes().get(attribute).stringValue();
        }
        return "";
    }
}