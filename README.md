# EC2PriceCalculator
Calculates price of AWS EC2 Instance Types

## Details <br>
**Java version:** 20<br>
**AWS SDK libraries version:** 2.17.123<br>
**Maven version:** Maven 3<br>

## Setup
### Java
1. Install Java Development Kit (JDK): Visit the [Oracle Java SE Development Kit downloads page](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) and select the appropriate JDK version for your operating system
2. Download the JDK installer and run it and follow on-screen instructions
3. Open/Install your preferred Java IDE ([IntelliJ IDEA Download](https://www.jetbrains.com/idea/download/?var=1&section=windows))
### AWS
1. Create an [AWS](https://aws.amazon.com/) root user account 
2. Sign in to the [AWS Management Console](https://aws.amazon.com/console/) using the root user account
3. Navigate to the AWS Identity and Access Management (IAM) service and create a new IAM user or use an existing one (https://aws.amazon.com/blogs/security/now-create-and-manage-users-more-easily-with-the-aws-iam-console/)
4. Add AmazonEC2FullAccess and AWSPriceListServiceFullAccess to the IAM user under "Permissions"
5. Install the AWS toolkit in your IDE ([AWS in IntelliJ IDEA](https://www.youtube.com/watch?v=KvBFFDYaqSM))
6. Insert your IAM user credentials (Access Key ID and Secret Key - found under Security Credentials tab in the Management Console) in the config file under "[profile user1]"
### Redis Database
1. Create an account on [Redis Cloud](https://redis.com/try-free/)
2. Create a new database and select "Amazon Web Services" as the Cloud Vendor
3. Once the database has been created, click into it to view details
4. Under the Configuration tab, scroll down to "Security" to view your username and password - you may change your password if you would like<br><br>
  ![ec2db](https://github.com/compi314159/EC2PriceCalculator/assets/71290813/1846f628-8a6f-47d8-a7b6-00aff062980f)
5. Enter these credentials in the config.properties file in the EC2PriceCalculator project - they will automatically be added to the main java class

## Usage
* Run the program
* Enter EC2 Instance Type when prompted
* If an error is thrown, it will be displayed along with necessary fixes
* Prompt will appear to find the price of another instance type - input "y" for yes or "n" for no when prompted
