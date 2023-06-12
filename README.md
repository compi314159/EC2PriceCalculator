# EC2PriceCalculator
Calculates price of AWS EC2 Instance Types
## Details <br>
**Java version:** 20<br>
**AWS SDK libraries version:** 2.17.123<br>
**Maven version:** Maven 3<br>

## Setup
1. Install Java Development Kit (JDK): Visit the [Oracle Java SE Development Kit downloads page](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) and select the appropriate JDK version for your operating system
2. Download the JDK installer and run it and follow on-screen instructions
3. Open/Install your preferred Java IDE ([IntelliJ IDEA Download](https://www.jetbrains.com/idea/download/?var=1&section=windows))
4. Create an [AWS](https://aws.amazon.com/) root user account 
5. Sign in to the [AWS Management Console](https://aws.amazon.com/console/) using the root user account
6. Navigate to the AWS Identity and Access Management (IAM) service and create a new IAM user or use an existing one (https://aws.amazon.com/blogs/security/now-create-and-manage-users-more-easily-with-the-aws-iam-console/)
7. Add AmazonEC2FullAccess and AWSPriceListServiceFullAccess to the IAM user under "Permissions"
8. Install the AWS toolkit in your IDE ([AWS in IntelliJ IDEA](https://www.youtube.com/watch?v=KvBFFDYaqSM))
9. Insert your IAM user credentials (Access Key ID and Secret Key - found under Security Credentials tab in the Management Console) in the config file under "[profile user1]" 
10. Download and run provided JAR file or source code
