package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.*;
import java.net.URL;
import redis.clients.jedis.Jedis;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.Map.Entry;


public class EC2Main
{
    public static boolean found = false;
    public static ArrayList<String> transferLocations = new ArrayList<String>();
    public static void main(String[] args) throws IOException {
        // read properties
        Properties prop = new Properties();
        File configuration = new File("src/main/resources/config.properties");
        prop.load(new FileInputStream(configuration));

        // connect to jedis
        Jedis jedis = new Jedis("redis://" + prop.getProperty("redisUser").trim() + ":" + prop.getProperty("redisPass").trim()
                + "@" + prop.getProperty("redisPublicEndpoint").trim());
        jedis.connect();

        // check if connection is successful
        if (jedis.ping().equalsIgnoreCase("pong")) {
            System.out.println("Connection successful");
        }

        Scanner input = new Scanner(System.in); // import scanner

        addTransferLocations();

        System.out.println("\n*** EC2 Instance Type Calculator ***");
        System.out.println("Enter Cloud Provider: ");
        System.out.println("AWS Regions: " +
                "\n\tus-east-2, us-east-1, us-west-1, us-west-2, af-south-1, " +
                "\n\tap-east-1, ap-south-2, ap-southeast-3, ap-southeast-4, " +
                "\n\tap-south-1, ap-northeast-3, ap-northeast-2, ap-southeast-1, " +
                "\n\tap-southeast-2, ap-northeast-1, ca-central-1, eu-central-1, " +
                "\n\teu-west-1, eu-west-2, eu-south-1, eu-west-3, eu-south-2, " +
                "\n\teu-north-1, eu-central-2, il-central-1, me-south-1, " +
                "\n\tme-central-1, sa-east-1, us-gov-east-1, us-gov-west-1");
        System.out.print("Select AWS Region: ");
        String region = input.nextLine().trim();
        System.out.println("Region: " + region);
        String cont = "y";
        while (cont.equalsIgnoreCase("y")) {
        System.out.println("\nOPTIONS:");
        System.out.println("1. Instance Type Price");
        System.out.println("2. Data Transfer Price");
        System.out.print("Select option: ");
        String optionString = input.nextLine();
        int option;
        try {
            option = Integer.parseInt(optionString);
        } catch (Exception e) {
            option = 0;
            System.out.print("Invalid entry, start over? (y/n): ");
            cont = input.nextLine();
        }
        if(option == 1)
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
                try {
                    choice = Integer.parseInt(selection);
                } catch (Exception e) {
                    choice = 0;
                }
                String os = "";
                if (choice == 1) {
                    os = "Windows";
                } else if (choice == 2) {
                    os = "RHEL";
                } else if (choice == 3) {
                    os = "SUSE";
                } else if (choice == 4) {
                    os = "Linux";
                } else if (choice == 5) {
                    os = "Red Hat Enterprise Linux with HA";
                } else if (choice == 6) {
                    os = "Ubuntu Pro";
                } else {
                    System.out.println("Invalid entry, operating system defaulted to Windows");
                    os = "Windows";
                }

                try {
                    String price;
                    if (!(getAWSCost(jedis, region, instanceType, os) == null)) {
                        price = getAWSCost(jedis, region, instanceType, os);
                        System.out.println("Successfully found in database!");
                    } else {
                        System.out.println("\nNot found in database. Receiving from json file.");
                        price = getJSONCost(instanceType, os, region);
                        if (!found) {
                            URL EC2Types = new URL("https://aws.amazon.com/ec2/instance-types/");
                            System.out.println("Instance type with given operating system does not exist, please enter valid instance type.");
                            System.out.println("Also make sure it exists with selected operating system (see " + EC2Types + ").");
                        }
                        addAWSCost(jedis, region, instanceType, os, price);
                    }
                    System.out.println("--> " + instanceType + " with " + os + " operating system price per hour: $" + price);
                    System.out.print("Continue? (y/n): ");
                    cont = input.nextLine();
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                catch(FileNotFoundException e)
                {
                    System.out.println("Error: " + e);
                    System.out.println("Invalid region selected, please start over.");
                    cont = "n";
                }
                catch (Exception e) {
                    System.out.println("Error: " + e);
                    System.out.print("Invalid entry, try again? (y/n): ");
                    cont = input.nextLine();
                }

        }
        else if(option == 2) {
            System.out.print("Search for region? (y/n): ");
            String selection = input.nextLine();
            if(selection.equalsIgnoreCase("y"))
            {
                System.out.print("Enter location keyword: ");
                String keyword = input.nextLine().toLowerCase();
                System.out.println("Locations containing \"" + keyword + "\":");
                boolean contains = false;
                for(String location : transferLocations)
                {
                    if (location.toLowerCase().contains(keyword))
                    {
                        System.out.println(location);
                        contains = true;
                    }
                }
                if(!contains)
                {
                    System.out.println("No locations found");
                }
            }
            System.out.println();
            System.out.print("Enter origin location: ");
            String start = input.nextLine();
            System.out.print("Search for region? (y/n): ");
            selection = input.nextLine();
            if(selection.equalsIgnoreCase("y"))
            {
                System.out.print("Enter location keyword: ");
                String keyword = input.nextLine().toLowerCase();
                System.out.println("Locations containing \"" + keyword + "\":");
                boolean contains = false;
                for(String location : transferLocations)
                {
                    if (location.toLowerCase().contains(keyword))
                    {
                        System.out.println(location);
                        contains = true;
                    }
                }
                if(!contains)
                {
                    System.out.println("No locations found");
                }
            }
            System.out.println();
            System.out.print("Enter destination location: ");
            String finish = input.nextLine();
            /////////////////////////////////////
            try {
                String cost;
                if (!(getTransferCost(jedis, start, finish) == null)) {
                    cost = getTransferCost(jedis, start, finish);
                    System.out.println("Successfully found in database!");
                } else {
                    System.out.println("\nNot found in database. Receiving from json file.");
                    cost = getJSONTransferCost(start, finish, region);
                    if (!found) {
                        System.out.println("Given location combination does not exist.");
                    }
                    addTransferCost(jedis, start, finish, cost);
                }
                System.out.println("--> Outbound Cost: " + cost + " per GB");
                System.out.print("Continue? (y/n): ");
                cont = input.nextLine();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                System.out.println("Error: " + e);
                input.nextLine();
                System.out.print("Invalid location, start over? (y/n): ");
                cont = input.nextLine();
            }
            }
        }
    }

    private static void addTransferLocations() {
        transferLocations.add("AWS GovCloud (US-West)");
        transferLocations.add("AWS GovCloud (US-East)");
        transferLocations.add("Africa (Cape Town)");
        transferLocations.add("Asia Pacific (Hong Kong)");
        transferLocations.add("Asia Pacific (Hyderabad)");
        transferLocations.add("Asia Pacific (Jakarta)");
        transferLocations.add("Asia Pacific (Melbourne)");
        transferLocations.add("Asia Pacific (Mumbai)");
        transferLocations.add("Asia Pacific (Osaka)");
        transferLocations.add("Asia Pacific (Seoul)");
        transferLocations.add("Asia Pacific (Singapore)");
        transferLocations.add("Asia Pacific (Sydney)");
        transferLocations.add("Asia Pacific (Tokyo)");
        transferLocations.add("Canada (Central)");
        transferLocations.add("Europe (Frankfurt)");
        transferLocations.add("Europe (Ireland)");
        transferLocations.add("Europe (London)");
        transferLocations.add("Europe (Milan)");
        transferLocations.add("Europe (Paris)");
        transferLocations.add("Europe (Spain)");
        transferLocations.add("Europe (Stockholm)");
        transferLocations.add("Europe (Zurich)");
        transferLocations.add("Israel (Tel Aviv)");
        transferLocations.add("Middle East (Bahrain)");
        transferLocations.add("Middle East (UAE)");
        transferLocations.add("South America (Sao Paulo)");
        transferLocations.add("US East (Ohio)");
        transferLocations.add("US East (Northern Virginia)");
        transferLocations.add("US East (Verizon) - Nashville");
        transferLocations.add("US East (Verizon) - Tampa");
        transferLocations.add("US West (Los Angeles)");
        transferLocations.add("US West (N. California)");
        transferLocations.add("US West (Oregon)");
        transferLocations.add("Asia Pacific (KDDI) - Osaka");
        transferLocations.add("Asia Pacific (KDDI) - Tokyo");
        transferLocations.add("Asia Pacific (SKT) - Daejeon");
        transferLocations.add("Asia Pacific (SKT) - Seoul");
        transferLocations.add("Canada (BELL) - Toronto");
        transferLocations.add("Europe (Vodafone) - London");
        transferLocations.add("Europe (Vodafone) - Manchester");
        transferLocations.add("US West (Verizon) - Denver");
        transferLocations.add("US West (Verizon) - Las Vegas");
        transferLocations.add("US West (Verizon) - Los Angeles");
        transferLocations.add("US West (Verizon) - Phoenix");
        transferLocations.add("US West (Verizon) - San Francisco Bay Area");
        transferLocations.add("US West (Verizon) - Seattle");
    }

    // add cost to database
    private static void addAWSCost(Jedis jedis, String region, String instanceType, String os, String price) {
    jedis.set(region+","+instanceType+","+os, price);
}
    private static void addTransferCost(Jedis jedis, String start, String finish, String cost) {
        jedis.set(start+","+finish, cost);
    }

    // get instance type cost from database
    public static String getAWSCost(Jedis jedis, String region, String instanceType, String os)
    {
        return jedis.get(region+","+instanceType+","+os);
    }

    // get transfer cost from database
    public static String getTransferCost(Jedis jedis, String start, String finish)
    {
        return jedis.get(start+","+finish);
    }
    // get cost from json file
    public static String getJSONCost(String instanceType, String os, String region) throws IOException, ParseException {
        downloadFile(region); // call method to download json file

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
    public static String getJSONTransferCost(String start, String finish, String region) throws IOException, ParseException {
    downloadFile(region); // call method to download json file

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
        if(value.toString().contains(("\"fromLocation\" : \"")+start+("\"")) && value.toString().contains
                (("\"toLocation\" : \"")+finish+("\"")))
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
    //try just searching for the entire string within sku
    String description = getTransferPriceWithSKU(onDemand, sku);
    return description.split(" ")[0];
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
    // search second part of json file using sku for price
    private static String getTransferPriceWithSKU(JSONObject onDemand, String sku) {
        JSONObject sku1 = (JSONObject) onDemand.get(sku);
        JSONObject sku2 = (JSONObject) sku1.get(sku+".JRTCKXETXF");
        JSONObject priceDimensions = (JSONObject) sku2.get("priceDimensions");
        JSONObject priceDimensions2 = (JSONObject) priceDimensions.get(sku+".JRTCKXETXF" + ".6YS6EN2CT7");
        String price1 = (String) priceDimensions2.get("description");
        return price1;
    }
    // download json file
    private static void downloadFile(String region) throws IOException {
        File f = new File("index.json");
        System.out.print("JSON file download status: ");
        if (!f.exists() || f.isDirectory()) {
            URL url = new URL("https://pricing."+region+ ".amazonaws.com/offers/v1.0/aws/AmazonEC2/current/" + region + "/index.json");
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
}