package aws;

import java.util.Iterator;
import java.util.Scanner;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

public class awsTest {

	static AmazonEC2 ec2;

	// Initialize AWS EC2 Client
	private static void init() throws Exception {
		String credentialsFilePath = "C:/Users/robot/.aws/credentials.txt"; // AWS 자격증명 파일 경로
		ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(credentialsFilePath, "default");

		try {
			credentialsProvider.getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the specified credentials file. " +
							"Ensure the credentials file is at " + credentialsFilePath + " and properly formatted.", e);
		}

		ec2 = AmazonEC2ClientBuilder.standard()
				.withCredentials(credentialsProvider)
				.withRegion("ap-southeast-2") // 리전 설정
				.build();
	}

	public static void main(String[] args) throws Exception {
		init();

		Scanner menu = new Scanner(System.in);
		Scanner idScanner = new Scanner(System.in);
		int option;

		while (true) {
			System.out.println("\n-----------------------------------------------");
			System.out.println("Amazon AWS EC2 Management Console (SDK)");
			System.out.println("-----------------------------------------------");
			System.out.println("1. List Instances        2. Show Availability Zones");
			System.out.println("3. Start Instance        4. Show Regions");
			System.out.println("5. Stop Instance         6. Create Instance");
			System.out.println("7. Reboot Instance       8. List Images");
			System.out.println("99. Quit");
			System.out.println("-----------------------------------------------");
			System.out.print("Select an option: ");

			if (menu.hasNextInt()) {
				option = menu.nextInt();
			} else {
				System.out.println("Invalid input! Please enter a number.");
				menu.next(); // Clear invalid input
				continue;
			}

			String instanceId;
			switch (option) {
				case 1:
					listInstances();
					break;
				case 2:
					showAvailabilityZones();
					break;
				case 3:
					System.out.print("Enter Instance ID to start: ");
					instanceId = idScanner.nextLine();
					startInstance(instanceId);
					break;
				case 4:
					showRegions();
					break;
				case 5:
					System.out.print("Enter Instance ID to stop: ");
					instanceId = idScanner.nextLine();
					stopInstance(instanceId);
					break;
				case 6:
					System.out.print("Enter AMI ID to create instance: ");
					String amiId = idScanner.nextLine();
					createInstance(amiId);
					break;
				case 7:
					System.out.print("Enter Instance ID to reboot: ");
					instanceId = idScanner.nextLine();
					rebootInstance(instanceId);
					break;
				case 8:
					listImages();
					break;
				case 99:
					System.out.println("Exiting... Goodbye!");
					menu.close();
					idScanner.close();
					return;
				default:
					System.out.println("Invalid option! Please select a valid menu option.");
			}
		}
	}

	public static void listInstances() {
		System.out.println("Listing EC2 Instances...");
		boolean done = false;
		DescribeInstancesRequest request = new DescribeInstancesRequest();

		while (!done) {
			DescribeInstancesResult response = ec2.describeInstances(request);

			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					System.out.printf("[ID] %s, [AMI] %s, [Type] %s, [State] %s, [Monitoring] %s\n",
							instance.getInstanceId(),
							instance.getImageId(),
							instance.getInstanceType(),
							instance.getState().getName(),
							instance.getMonitoring().getState());
				}
			}

			request.setNextToken(response.getNextToken());
			if (response.getNextToken() == null) {
				done = true;
			}
		}
	}

	public static void showAvailabilityZones() {
		System.out.println("Showing Available Zones...");
		try {
			DescribeAvailabilityZonesResult zonesResult = ec2.describeAvailabilityZones();
			for (AvailabilityZone zone : zonesResult.getAvailabilityZones()) {
				System.out.printf("[ID] %s, [Region] %s, [Zone] %s\n",
						zone.getZoneId(),
						zone.getRegionName(),
						zone.getZoneName());
			}
		} catch (AmazonServiceException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public static void startInstance(String instanceId) {
		try {
			StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instanceId);
			ec2.startInstances(request);
			System.out.printf("Successfully started instance: %s\n", instanceId);
		} catch (Exception e) {
			System.err.println("Error starting instance: " + e.getMessage());
		}
	}

	public static void showRegions() {
		System.out.println("Available Regions:");
		DescribeRegionsResult regionsResponse = ec2.describeRegions();

		for (Region region : regionsResponse.getRegions()) {
			System.out.printf("[Region] %s, [Endpoint] %s\n",
					region.getRegionName(),
					region.getEndpoint());
		}
	}

	public static void stopInstance(String instanceId) {
		try {
			StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instanceId);
			ec2.stopInstances(request);
			System.out.printf("Successfully stopped instance: %s\n", instanceId);
		} catch (Exception e) {
			System.err.println("Error stopping instance: " + e.getMessage());
		}
	}

	public static void createInstance(String amiId) {
		try {
			RunInstancesRequest runRequest = new RunInstancesRequest()
					.withImageId(amiId)
					.withInstanceType(InstanceType.T2Micro)
					.withMaxCount(1)
					.withMinCount(1);

			RunInstancesResult runResponse = ec2.runInstances(runRequest);
			String instanceId = runResponse.getReservation().getInstances().get(0).getInstanceId();

			System.out.printf("Successfully created instance: %s (AMI: %s)\n", instanceId, amiId);
		} catch (Exception e) {
			System.err.println("Error creating instance: " + e.getMessage());
		}
	}

	public static void rebootInstance(String instanceId) {
		try {
			RebootInstancesRequest request = new RebootInstancesRequest().withInstanceIds(instanceId);
			ec2.rebootInstances(request);
			System.out.printf("Successfully rebooted instance: %s\n", instanceId);
		} catch (Exception e) {
			System.err.println("Error rebooting instance: " + e.getMessage());
		}
	}

	public static void listImages() {
		System.out.println("Listing available images...");
		try {
			DescribeImagesRequest request = new DescribeImagesRequest();
			DescribeImagesResult result = ec2.describeImages(request);

			for (Image image : result.getImages()) {
				System.out.printf("[ImageID] %s, [Name] %s, [Owner] %s\n",
						image.getImageId(),
						image.getName(),
						image.getOwnerId());
			}
		} catch (Exception e) {
			System.err.println("Error listing images: " + e.getMessage());
		}
	}
}
