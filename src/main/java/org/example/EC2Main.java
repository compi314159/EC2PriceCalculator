package org.example;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.*;
import redis.clients.jedis.Jedis;

public class EC2Main {
    public static void main(String[] args) throws IOException {
        // read properties
        Properties prop = new Properties();
        File configuration = new File("src/main/resources/config.properties");
        prop.load(new FileInputStream(configuration));

        // connect to jedis
        Jedis jedis = new Jedis("redis://" + prop.getProperty("redisUser")+":" + prop.getProperty("redisPass")
                + "@" + prop.getProperty("redisPublicEndpoint"));
        jedis.connect();

        // check if connection is successful
        if(jedis.ping().equalsIgnoreCase("pong"))
        {
            System.out.println("Connection successful");
        }

        Scanner input = new Scanner(System.in); // import scanner

        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
        PricingClient pricingClient = PricingClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider).build();

        System.out.println("\n*** EC2 Instance Type Price Calculator ***");
        String cont = "y";
        while(cont.equalsIgnoreCase("y")) {
            System.out.print("\nEnter EC2 instance type: ");
            String instanceType = input.nextLine().trim();
            try
            {
                String price;
                if(!(getAWSCost(jedis, instanceType) == null))
                {
                    price = getAWSCost(jedis, instanceType);
                    System.out.println(instanceType + " successfully found in database!");
                }
                else {
                    System.out.println(instanceType + " not found in database. Receiving from aws");
                    price = getInstantAWSCost(instanceType, credentialsProvider, pricingClient, input);
                    addAWSCost(jedis, instanceType, price);
                }
                System.out.println("--> " + instanceType + " price per hour: " + price);
                System.out.print("Continue? (y/n): ");
                cont = input.nextLine();
            }
            catch (StringIndexOutOfBoundsException e)
            {
                System.out.println("Error: " + e);
                URL EC2Types = new URL("https://aws.amazon.com/ec2/instance-types/");
                System.out.println("Please enter valid instance type (see " + EC2Types + ").");
            }
            catch (Exception e) {
                System.out.println("Error: " + e);
                System.out.println("Invalid entry, please try again.");
            }
        }
    }

    // add cost to database
    private static void addAWSCost(Jedis jedis, String instanceType, String price) {
        jedis.set(instanceType, price);
    }

    // get cost from database
    public static String getAWSCost(Jedis jedis, String instanceType)
    {
        return jedis.get(instanceType);
    }

    // get cost from AWS
    public static String getInstantAWSCost(String instanceType, ProfileCredentialsProvider credentialsProvider, PricingClient pricingClient, Scanner input) throws StringIndexOutOfBoundsException, Exception
    {
            GetProductsRequest request = GetProductsRequest.builder().serviceCode("AmazonEC2")
                    .filters(Filter.builder().field("instanceType").value(instanceType)
                            .type("TERM_MATCH").build()).maxResults(1).build();

            GetProductsResponse response = pricingClient.getProducts(request);

            // Get price
            String resp = response.toString();
            String terms = resp.substring(resp.indexOf("terms"));
            String price = terms.substring(terms.indexOf("pricePerUnit"));
            return price.substring(14, price.indexOf("}") + 1);
    }
}