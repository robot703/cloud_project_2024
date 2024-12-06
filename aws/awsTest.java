	package aws;

	import java.io.BufferedReader;
	import java.io.InputStream;
	import java.io.InputStreamReader;
	import java.util.Iterator;
	import java.util.Scanner;
	import com.amazonaws.AmazonClientException;
	import com.amazonaws.AmazonServiceException;
	import com.amazonaws.auth.AWSStaticCredentialsProvider;
	import com.amazonaws.auth.BasicAWSCredentials;
	import com.amazonaws.auth.profile.ProfileCredentialsProvider;
	import com.amazonaws.services.appstream.model.Session;
	import com.amazonaws.services.ec2.AmazonEC2;
	import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
	import com.amazonaws.services.ec2.model.*;
	import com.jcraft.jsch.ChannelExec;
	import com.jcraft.jsch.JSch;

	import com.amazonaws.services.ec2.model.KeyPair;
	import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
	import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;

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
								"Ensure the credentials file is at " +
								credentialsFilePath +
								" and properly formatted.", e);
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
			System.out.println("9. Condor_status         10. Describe Instance");
			System.out.println("11. Check Instance Memory  12. List Volumes");
			System.out.println("13. Network Security Menu 99. Quit");
			System.out.println("13. Network Security Menu 99. Quit");
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
				case 9:
					Condor_status();
					break;
				case 10:
					System.out.print("Enter Instance ID to fetch details: ");
					instanceId = idScanner.nextLine();
					describeInstance(instanceId);
					break;
				case 11:
					System.out.print("Enter Instance ID to check memory: ");
					instanceId = idScanner.nextLine();
					checkInstanceMemory(instanceId);
					break;
				case 12:
					listAllVolumes();
					break;

				case 13:
					showNetworkSecurityMenu();
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

		while (!done) {
			DescribeInstancesResult response = ec2.describeInstances(request);

			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
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

			if (response.getNextToken() == null) {
				done = true;
			}
		}
	}

	public static void availableZones() {

		System.out.println("Available zones....");
		try {
			DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
			Iterator<AvailabilityZone> iterator = availabilityZonesResult.getAvailabilityZones().iterator();

			AvailabilityZone zone;
			while (iterator.hasNext()) {
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

	public static void startInstance(String instance_id) {

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

		for (Region region : regions_response.getRegions()) {
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

		} catch (Exception e) {
			System.out.println("Exception: " + e.toString());
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

		} catch (Exception e) {
			System.out.println("Exception: " + e.toString());
		}


	}

	public static void listImages() {
		System.out.println("Listing images....");
		String credentialsFilePath = "C:/Users/robot/.aws/credentials.txt";
		DescribeImagesRequest request = new DescribeImagesRequest();
		ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(credentialsFilePath, "default");

		request.getFilters().add(new Filter().withName("owner-id").withValues("557690623174"));
		request.setRequestCredentialsProvider(credentialsProvider);

		DescribeImagesResult results = ec2.describeImages(request);

		for (Image images : results.getImages()) {
			System.out.printf("[ImageID] %s, [Name] %s, [Owner] %s\n",
					images.getImageId(), images.getName(), images.getOwnerId());
		}

	}

		public static void Condor_status() {
			String user = "ec2-user"; // SSH 사용자 이름
			int port = 22; // SSH 포트 (기본값은 22)
			String privateKeyPath = "C:/Users/robot/.ssh/cloud-test.pem";// 키 파일 경로

			com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch(); // JSch 객체 생성
			com.jcraft.jsch.Session session = null; // JSch의 Session 사용
			String instanceId = "i-0f2dffc1e0feaf59e";

			String host = getPublicDNS(instanceId);
			if (host == null) {
				System.err.println("Failed to retrieve Public DNS Name for instance: " + instanceId);
				return;
			}

			try {
				// 개인 키를 추가합니다.
				jsch.addIdentity(privateKeyPath);

				// 세션 설정
				session = jsch.getSession(user, host, port);

				// 호스트 키 검증 비활성화 (생략 가능)
				session.setConfig("StrictHostKeyChecking", "no");

				// SSH 연결 시작
				System.out.println("Establishing SSH Connection...");
				session.connect();

				// 명령어 실행
				com.jcraft.jsch.ChannelExec channelExec = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
				channelExec.setCommand("condor_status");
				channelExec.setErrStream(System.err);

				InputStream in = channelExec.getInputStream();
				channelExec.connect();

				// 명령어 결과 읽기
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line;
				System.out.println("condor_status Output:");
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}

				// 채널과 세션 닫기
				channelExec.disconnect();
				session.disconnect();
			} catch (Exception e) {
				System.err.println("Error while connecting to the server: " + e.getMessage());
				e.printStackTrace();
			}
		}

		private static String getPublicDNS(String instanceId) {
			try {
				// Create request to describe the instance
				DescribeInstancesRequest request = new DescribeInstancesRequest()
						.withInstanceIds(instanceId);

				// Execute the request
				DescribeInstancesResult response = ec2.describeInstances(request);

				for (Reservation reservation : response.getReservations()) {
					for (Instance instance : reservation.getInstances()) {
						return instance.getPublicDnsName(); // Public DNS Name 반환
					}
				}
			} catch (Exception e) {
				System.err.println("Error fetching Public DNS Name: " + e.getMessage());
				e.printStackTrace();
			}
			return null; // 오류 시 null 반환
		}

		//인스턴스 상세정보 확인 함수
		public static void describeInstance(String instanceId) {
			try {
				// Create request to describe the instance
				DescribeInstancesRequest request = new DescribeInstancesRequest()
						.withInstanceIds(instanceId);

				// Execute the request
				DescribeInstancesResult response = ec2.describeInstances(request);

				// Process and display the results
				for (Reservation reservation : response.getReservations()) {
					for (Instance instance : reservation.getInstances()) {
						System.out.println("==== Instance Detailed Information ====");
						System.out.println("Instance ID: " + instance.getInstanceId());
						System.out.println("Public IPv4 Address: " + instance.getPublicIpAddress());
						System.out.println("Private IPv4 Address: " + instance.getPrivateIpAddress());
						System.out.println("Public DNS Name: " + instance.getPublicDnsName());
						System.out.println("Private DNS Name: " + instance.getPrivateDnsName());
						System.out.println("Instance State: " + instance.getState().getName());
						System.out.println("Instance Type: " + instance.getInstanceType());
						System.out.println("Elastic GPU Associations: " +
								(instance.getElasticGpuAssociations().isEmpty() ? "None" : instance.getElasticGpuAssociations()));
						System.out.println("VPC ID: " + instance.getVpcId());
						System.out.println("Subnet ID: " + instance.getSubnetId());
						System.out.println("IAM Role: " +
								(instance.getIamInstanceProfile() != null ? instance.getIamInstanceProfile().getArn() : "None"));
						System.out.println("Launch Time: " + instance.getLaunchTime());
						System.out.println("Security Groups: " +
								instance.getSecurityGroups().stream()
										.map(GroupIdentifier::getGroupName)
										.toList());
						System.out.println("===============================");
					}
				}
			} catch (AmazonServiceException e) {
				System.err.println("Amazon service error: " + e.getErrorMessage());
			} catch (AmazonClientException e) {
				System.err.println("Amazon client error: " + e.getMessage());
			}
		}

		//인스턴스 메모리 확인
		public static void checkInstanceMemory(String instanceId) {
			String user = "ec2-user"; // SSH 사용자 이름
			String host = getPublicDNS(instanceId); // EC2 퍼블릭 DNS
			int port = 22; // SSH 포트 (기본값은 22)
			String privateKeyPath = "C:/Users/robot/.ssh/cloud-test.pem"; // 키 파일 경로

			com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch(); // JSch 객체 생성
			com.jcraft.jsch.Session session = null; // JSch의 Session 사용

			try {
				// 개인 키를 추가합니다.
				jsch.addIdentity(privateKeyPath);

				// 세션 설정
				session = jsch.getSession(user, host, port);

				// 호스트 키 검증 비활성화 (생략 가능)
				session.setConfig("StrictHostKeyChecking", "no");

				// SSH 연결 시작
				System.out.println("Establishing SSH Connection...");
				session.connect();

				// 메모리 상태 확인을 위한 명령어 실행 (예: free -h)
				com.jcraft.jsch.ChannelExec channelExec = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
				channelExec.setCommand("free -h"); // EC2 인스턴스의 메모리 사용 상태 확인
				channelExec.setErrStream(System.err);

				InputStream in = channelExec.getInputStream();
				channelExec.connect();

				// 명령어 결과 읽기
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line;
				System.out.println("Memory Status Output:");
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}

				// 채널과 세션 닫기
				channelExec.disconnect();
				session.disconnect();
			} catch (Exception e) {
				System.err.println("Error while connecting to the server: " + e.getMessage());
				e.printStackTrace();
			}
		}

		//불륨리스트 조회
		public static void listAllVolumes() {
			try {
				// EBS 볼륨 정보 가져오기
				DescribeVolumesRequest request = new DescribeVolumesRequest();
				DescribeVolumesResult result = ec2.describeVolumes(request);

				// 각 볼륨의 정보를 출력
				System.out.println("Listing all volumes:");
				for (Volume volume : result.getVolumes()) {
					System.out.println("Volume ID: " + volume.getVolumeId());
					System.out.println("Availability Zone: " + volume.getAvailabilityZone());
					System.out.println("Size (GiB): " + volume.getSize());
					System.out.println("Volume Type: " + volume.getVolumeType());
					System.out.println("State: " + volume.getState());
					System.out.println("Create Time: " + volume.getCreateTime());
					System.out.println("Attachment Status: " + (volume.getAttachments().isEmpty() ? "Not Attached" : "Attached"));
					System.out.println("-----------------------------------------------");
				}
			} catch (AmazonServiceException e) {
				System.err.println("Error occurred while retrieving volumes: " + e.getMessage());
				e.printStackTrace();
			} catch (AmazonClientException e) {
				System.err.println("Error occurred while connecting to AWS: " + e.getMessage());
				e.printStackTrace();
			}
		}

		public static void showNetworkSecurityMenu() {
			Scanner scanner = new Scanner(System.in);  // Local scanner inside the method
			while (true) {
				System.out.println("\n 1. Security Groups");
				System.out.println(" 2. Key Pairs");
				System.out.println(" 3. Network Interfaces");
				System.out.println(" 4. Back to menu");
				System.out.print(" Choose Menu : ");
				int choice = scanner.nextInt();

				switch (choice) {
					case 1:
						showSecurityGroups();
						break;
					case 2:
						showKeyPairs();
						break;
					case 3:
						showNetworkInterfaces();
						break;
					case 4:
						return; // 원래 메뉴로 돌아가기
					default:
						System.out.println("잘못된 선택입니다.");
						break;
				}
			}
		}

		public static void showSecurityGroups() {
			try {
				// Create a DescribeSecurityGroupsRequest
				DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();

				// Call the AWS EC2 service to get the list of security groups
				DescribeSecurityGroupsResult response = ec2.describeSecurityGroups(request);

				System.out.println("Name | Security Group ID | Group Name | VPC ID | Description | Owner | Inbound Rules Count | Outbound Rules Count");
				System.out.println("-----------------------------------------------------------------------------------------------------------");

				for (SecurityGroup group : response.getSecurityGroups()) {
					// Print details of each security group
					System.out.println(group.getGroupName() + " | " + group.getGroupId() + " | " + group.getGroupName() + " | "
							+ group.getVpcId() + " | " + group.getDescription() + " | " + group.getOwnerId() + " | "
							+ group.getIpPermissions().size() + " | " + group.getIpPermissionsEgress().size());
				}
			} catch (AmazonServiceException e) {
				System.out.println("Error fetching security groups: " + e.getMessage());
			} catch (AmazonClientException e) {
				System.out.println("Error communicating with AWS: " + e.getMessage());
			}
		}


		public static void showKeyPairs() {
			try {
				// Describe key pairs from EC2
				DescribeKeyPairsRequest request = new DescribeKeyPairsRequest();
				DescribeKeyPairsResult result = ec2.describeKeyPairs(request);

				// Iterate through the key pairs and print the relevant details
				System.out.println("Key Pair List:");
				System.out.println("-------------------------------------------------");
				System.out.println("Name | Fingerprint | Key Pair ID");
				System.out.println("-------------------------------------------------");

				for (KeyPairInfo keyPair : result.getKeyPairs()) {
					// Printing key pair details
					System.out.printf("%s | %s | %s%n", keyPair.getKeyName(), keyPair.getKeyFingerprint(), keyPair.getKeyName());
				}
			} catch (AmazonServiceException e) {
				System.out.println("Error occurred while fetching key pairs: " + e.getMessage());
			}
		}


		public static void showNetworkInterfaces() {
			try {
				// Create a DescribeNetworkInterfacesRequest
				DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();

				// Call the AWS EC2 service to get the list of network interfaces
				DescribeNetworkInterfacesResult response = ec2.describeNetworkInterfaces(request);

				System.out.println("Name | Network Interface ID | Subnet ID | VPC ID | Availability Zone | Security Group Name | Security Group ID | Interface Type | Description | Instance ID | Status | Public IPv4 Address | Primary Private IPv4 Address");
				System.out.println("-------------------------------------------------------------------------------------------------------------------------------");

				for (NetworkInterface networkInterface : response.getNetworkInterfaces()) {
					// Get the association and check if it's null
					String publicIp = "N/A"; // Default value if no public IP exists
					if (networkInterface.getAssociation() != null && networkInterface.getAssociation().getPublicIp() != null) {
						publicIp = networkInterface.getAssociation().getPublicIp(); // Fetch the public IP if available
					}

					// Print details of each network interface, including public IP if available
					System.out.println(networkInterface.getNetworkInterfaceId() + " | "
							+ networkInterface.getNetworkInterfaceId() + " | " + networkInterface.getSubnetId() + " | "
							+ networkInterface.getVpcId() + " | " + networkInterface.getAvailabilityZone() + " | "
							+ networkInterface.getGroups() + " | " + networkInterface.getStatus() + " | "
							+ publicIp + " | "
							+ networkInterface.getPrivateIpAddresses().get(0).getPrivateIpAddress());
				}
			} catch (AmazonServiceException e) {
				System.out.println("Error fetching network interfaces: " + e.getMessage());
			} catch (AmazonClientException e) {
				System.out.println("Error communicating with AWS: " + e.getMessage());
			}
		}


	}



