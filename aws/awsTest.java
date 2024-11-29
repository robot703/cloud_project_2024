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
					availableZones();
					break;
				case 3:
					System.out.print("Enter Instance ID to start: ");
					instanceId = idScanner.nextLine();
					startInstance(instanceId);
					break;
				case 4:
					availableRegions();
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

		System.out.println("Listing instances....");
		boolean done = false;

		DescribeInstancesRequest request = new DescribeInstancesRequest();

		while(!done) {
			DescribeInstancesResult response = ec2.describeInstances(request);

			for(Reservation reservation : response.getReservations()) {
				for(Instance instance : reservation.getInstances()) {
					System.out.printf(
							"[id] %s, " +
									"[AMI] %s, " +
									"[type] %s, " +
									"[state] %10s, " +
									"[monitoring state] %s",
							instance.getInstanceId(),
							instance.getImageId(),
							instance.getInstanceType(),
							instance.getState().getName(),
							instance.getMonitoring().getState());
				}
				System.out.println();
			}

			request.setNextToken(response.getNextToken());

			if(response.getNextToken() == null) {
				done = true;
			}
		}
	}

	public static void availableZones()	{

		System.out.println("Available zones....");
		try {
			DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
			Iterator <AvailabilityZone> iterator = availabilityZonesResult.getAvailabilityZones().iterator();

			AvailabilityZone zone;
			while(iterator.hasNext()) {
				zone = iterator.next();
				System.out.printf("[id] %s,  [region] %15s, [zone] %15s\n", zone.getZoneId(), zone.getRegionName(), zone.getZoneName());
			}
			System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
					" Availability Zones.");

		} catch (AmazonServiceException ase) {
			System.out.println("Caught Exception: " + ase.getMessage());
			System.out.println("Reponse Status Code: " + ase.getStatusCode());
			System.out.println("Error Code: " + ase.getErrorCode());
			System.out.println("Request ID: " + ase.getRequestId());
		}

	}

	public static void startInstance(String instance_id)
	{

		System.out.printf("Starting .... %s\n", instance_id);

		DryRunSupportedRequest<StartInstancesRequest> dry_request =
				() -> {
					StartInstancesRequest request = new StartInstancesRequest()
							.withInstanceIds(instance_id);

					return request.getDryRunRequest();
				};

		StartInstancesRequest request = new StartInstancesRequest()
				.withInstanceIds(instance_id);

		ec2.startInstances(request);

		System.out.printf("Successfully started instance %s", instance_id);
	}


	public static void availableRegions() {

		System.out.println("Available regions ....");

		DescribeRegionsResult regions_response = ec2.describeRegions();

		for(Region region : regions_response.getRegions()) {
			System.out.printf(
					"[region] %15s, " +
							"[endpoint] %s\n",
					region.getRegionName(),
					region.getEndpoint());
		}
	}

	public static void stopInstance(String instance_id) {

		DryRunSupportedRequest<StopInstancesRequest> dry_request =
				() -> {
					StopInstancesRequest request = new StopInstancesRequest()
							.withInstanceIds(instance_id);

					return request.getDryRunRequest();
				};

		try {
			StopInstancesRequest request = new StopInstancesRequest()
					.withInstanceIds(instance_id);

			ec2.stopInstances(request);
			System.out.printf("Successfully stop instance %s\n", instance_id);

		} catch(Exception e)
		{
			System.out.println("Exception: "+e.toString());
		}

	}

	public static void createInstance(String ami_id) {
		RunInstancesRequest run_request = new RunInstancesRequest()
				.withImageId(ami_id)
				.withInstanceType(InstanceType.T2Micro)
				.withMaxCount(1)
				.withMinCount(1);

		RunInstancesResult run_response = ec2.runInstances(run_request);

		String reservation_id = run_response.getReservation().getInstances().get(0).getInstanceId();

		System.out.printf(
				"Successfully started EC2 instance %s based on AMI %s",
				reservation_id, ami_id);

	}

	public static void rebootInstance(String instance_id) {

		System.out.printf("Rebooting .... %s\n", instance_id);

		try {
			RebootInstancesRequest request = new RebootInstancesRequest()
					.withInstanceIds(instance_id);

			RebootInstancesResult response = ec2.rebootInstances(request);

			System.out.printf(
					"Successfully rebooted instance %s", instance_id);

		} catch(Exception e)
		{
			System.out.println("Exception: "+e.toString());
		}


	}

	public static void listImages() {
		System.out.println("Listing images....");

		DescribeImagesRequest request = new DescribeImagesRequest();
		ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();

		request.getFilters().add(new Filter().withName("name").withValues("htcondor-slave-image"));
		request.setRequestCredentialsProvider(credentialsProvider);

		DescribeImagesResult results = ec2.describeImages(request);

		for(Image images :results.getImages()){
			System.out.printf("[ImageID] %s, [Name] %s, [Owner] %s\n",
					images.getImageId(), images.getName(), images.getOwnerId());
		}

	}
}


