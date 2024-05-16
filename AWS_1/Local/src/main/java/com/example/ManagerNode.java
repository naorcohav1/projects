package com.example;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;


public class ManagerNode {

    private final Ec2Client ec2Client;
    private String instanceId;

    public ManagerNode(Region region, Ec2Client ec2) {
        this.ec2Client = ec2;
        this.instanceId ="";
    }

    public String getInstanceId(){
        return this.instanceId;
    }

    public void activateEC2Manager(AWS awsInstance, String script) {
        
        String managerTagValue = "ManagerNode"; // Identifier for the manager node
        // Check if the manager node exists and is running
        boolean isRunning = checkAndStartManagerNode(ec2Client, managerTagValue);

        if (!isRunning) {
            // Start the manager node if it doesn't exist or isn't running
            this.instanceId = awsInstance.createEC2(script, managerTagValue, 1);
        }
    }

    public boolean checkAndStartManagerNode(Ec2Client ec2, String managerTagValue) {
        DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .filters(Filter.builder()
                        .name("tag:Name") 
                        .values(managerTagValue)
                        .build(),
                        Filter.builder()
                        .name("instance-state-name")
                        .values("running")
                        .build())
                .build();
    
        DescribeInstancesResponse response = ec2.describeInstances(request);
        
        // Stream through all reservations and instances to find if any manager node is running
        boolean isManagerRunning = response.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .anyMatch(instance -> instance.state().name().equals(InstanceStateName.RUNNING));
    
        if (isManagerRunning) {
            System.out.println("A manager node is already running.");
        } else {
            System.out.println("No manager node is currently running.");
        }
        
        return isManagerRunning;
    }
    
}
