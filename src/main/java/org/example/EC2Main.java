package org.example;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.*;

public class EC2Main {
    public static void main(String[] args) throws MalformedURLException {
        Scanner input = new Scanner(System.in);
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();

        PricingClient pricingClient = PricingClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider).build();

        System.out.println("\n*** EC2 Instance Type Price Calculator ***");
        String cont = "y";
        while(cont.equalsIgnoreCase("y")) {
            System.out.print("\nEnter EC2 instance type: ");
            String instanceType = input.nextLine().trim();
            try {

                GetProductsRequest request = GetProductsRequest.builder().serviceCode("AmazonEC2")
                        .filters(Filter.builder().field("instanceType").value(instanceType)
                                .type("TERM_MATCH").build()).maxResults(1).build();

                GetProductsResponse response = pricingClient.getProducts(request);
                // Get price
                String resp = response.toString();
                String terms = resp.substring(resp.indexOf("terms"));
                String price = terms.substring(terms.indexOf("pricePerUnit"));
                System.out.println("--> " + instanceType + " price per hour: " + price.substring(14, price.indexOf("}") + 1));
                System.out.print("Continue? (y/n): ");
                cont = input.nextLine();

            } catch (StringIndexOutOfBoundsException e) {
                System.out.println("Error: " + e);
                URL EC2Types = new URL("https://aws.amazon.com/ec2/instance-types/");
                System.out.print("Please enter valid instance type (see " + EC2Types + "), try again? (y/n): ");
                cont = input.nextLine();
            }
            catch (Exception e)
            {
                System.out.println("Error: " + e);
                System.out.print("Invalid entry, try again? (y/n): ");
                cont = input.nextLine();
            }
        }
    }


}