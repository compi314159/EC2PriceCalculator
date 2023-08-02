package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.parser.ParseException;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import java.io.*;
import java.net.URL;

import java.util.Properties;
import java.util.Scanner;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.*;
import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.Map;
import java.util.Map.Entry;


public class EC2Main
{
    public static boolean found = false;
    public static void main(String[] args) throws IOException
    {
        // read properties
        Properties prop = new Properties();
        File configuration = new File("src/main/resources/config.properties");
        prop.load(new FileInputStream(configuration));

        // connect to jedis
        Jedis jedis = new Jedis("redis://" + prop.getProperty("redisUser").trim() + ":" + prop.getProperty("redisPass").trim()
                + "@" + prop.getProperty("redisPublicEndpoint").trim());
        jedis.connect();

        // check if connection is successful
        if (jedis.ping().equalsIgnoreCase("pong"))
        {
            System.out.println("Connection successful");
        }

        Scanner input = new Scanner(System.in); // import scanner

        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
        PricingClient pricingClient = PricingClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider).build();

        System.out.println("\n*** EC2 Instance Type Price Calculator ***");


        String cont = "y";
        while (cont.equalsIgnoreCase("y"))
        {
            System.out.print("\nEnter EC2 instance type: ");
            String instanceType = input.nextLine().trim();
            System.out.println("\nOperating Systems: ");
            System.out.println("\t1 - Windows");
                System.out.println("\t2 - RHEL");
                System.out.println("\t3 - SUSE");
                System.out.println("\t4 - Linux");
                System.out.println("\t5 - Red Hat Enterprise Linux with HA");
                System.out.println("\t6 - Ubuntu Pro");
                System.out.print("Select operating system: ");
                String selection = input.nextLine();
                int choice;
                try
                {
                    choice = Integer.parseInt(selection);
                }
                catch(Exception e)
                {
                    choice = 0;
                }
                String os = "";
                if(choice == 1)
                {
                    os = "Windows";
                }
                else if(choice == 2)
                {
                    os = "RHEL";
                }
                else if(choice == 3)
                {
                    os = "SUSE";
                }
                else if(choice == 4)
                {
                    os = "Linux";
                }
                else if(choice == 5)
                {
                    os = "Red Hat Enterprise Linux with HA";
                }
                else if(choice == 6)
                {
                    os = "Ubuntu Pro";
                }
                else
                {
                    System.out.println("Invalid entry, operating system defaulted to Windows");
                    os = "Windows";
                }

            try
            {
                String price;
                if (!(getAWSCost(jedis, instanceType, os) == null))
                {
                    price = getAWSCost(jedis, instanceType, os);
                    System.out.println("Successfully found in database!");
                }
                else
                {
                    System.out.println("\nNot found in database. Receiving from json file.");
                    price = getJSONCost(instanceType, os);
                    if (!found)
                    {
                        URL EC2Types = new URL("https://aws.amazon.com/ec2/instance-types/");
                        System.out.println("Instance type with given operating system does not exist, please enter valid instance type.");
                        System.out.println("Also make sure it exists with selected operating system (see " + EC2Types + ").");
                    }
                    addAWSCost(jedis, instanceType, os, price);
                }
                System.out.println("--> " + instanceType + " with " + os + " operating system price per hour: $" + price);
                System.out.print("Continue? (y/n): ");
                cont = input.nextLine();
            }
            catch (ParseException e)
            {
                throw new RuntimeException(e);
            }
            catch (Exception e)
            {
                System.out.println("Error: " + e);
                input.nextLine();
                System.out.print("Invalid entry, try again? (y/n): ");
                cont = input.nextLine();
            }
        }
    }
    // add cost to database
    private static void addAWSCost(Jedis jedis, String instanceType, String os, String price) {
    jedis.set(instanceType+","+os, price);
}

    // get cost from database
    public static String getAWSCost(Jedis jedis, String instanceType, String os)
    {
        return jedis.get(instanceType+","+os);
    }
    // get cost from json file
    public static String getJSONCost(String instanceType, String os) throws IOException, ParseException {
        downloadFile(); // call method to download json file

        Object obj = new JSONParser().parse(new FileReader("index.json"));
        JSONObject json = (JSONObject) obj;
        JSONObject products = (JSONObject) json.get("products");
        System.out.println("\nLoading...products.toString() in progress");
        Map<String, Object> map2 = new ObjectMapper().readValue(products.toString(), Map.class);
        System.out.println("Loading...searching for instance type");
        String sku = "";
        for (Entry<String, Object> entry : map2.entrySet())
        {
            sku = entry.getKey();
            Object value = entry.getValue();
            if(value.toString().contains(instanceType) && value.toString().contains
                    ("Shared") && value.toString().contains(os))
            {
                found = true;
            }
            if(found)
            {
                break;
            }
        }

        JSONObject terms = (JSONObject) json.get("terms");
        JSONObject onDemand = (JSONObject) terms.get("OnDemand");
        return getPriceWithSKU(onDemand, sku);
    }
    // search second part of json file using sku for price
    private static String getPriceWithSKU(JSONObject onDemand, String sku) {
        JSONObject sku1 = (JSONObject) onDemand.get(sku);
        JSONObject sku2 = (JSONObject) sku1.get(sku+".JRTCKXETXF");
        JSONObject priceDimensions = (JSONObject) sku2.get("priceDimensions");
        JSONObject priceDimensions2 = (JSONObject) priceDimensions.get(sku+".JRTCKXETXF" + ".6YS6EN2CT7");
        JSONObject price1 = (JSONObject) priceDimensions2.get("pricePerUnit");
        String price2 = (String) price1.get("USD");
        return price2;
    }
    // download json file
    private static void downloadFile() throws IOException {
        File f = new File("index.json");
        System.out.print("JSON file download status: ");
        if (!f.exists() || f.isDirectory()) {
            URL url = new URL("https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonEC2/current/us-east-1/index.json");
            BufferedReader readr =
                    new BufferedReader(new InputStreamReader(url.openStream()));

            // Enter filename in which you want to download
            BufferedWriter writer =
                    new BufferedWriter(new FileWriter("index.json"));

            // read each line from stream until end
            String line;
            while ((line = readr.readLine()) != null) {
                writer.write(line);
            }
            readr.close();
            writer.close();
            System.out.println("JSON file successfully downloaded.");
        } else {
            System.out.println("JSON file already downloaded.");
        }
    }

    private static void createFile(JSONObject instances) throws IOException {
        File f = new File("new.json");
        System.out.print("\nJSON file download status: ");
        if (!f.exists() || f.isDirectory()) 
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter("new.json"));
            writer.write(instances.toString());
            writer.close();
            System.out.println("new JSON file successfully created.");
        } 
        else 
        {
            System.out.println("new JSON file already created.");
        }
    }


    // get cost from AWS - no longer used - incorrect prices
    public static String getInstantAWSCost(String instanceType, PricingClient pricingClient)
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