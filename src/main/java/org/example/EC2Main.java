package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.parser.ParseException;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.*;
import redis.clients.jedis.Jedis;
//
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
//
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
                // json work starts here

                URL json = new URL("https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonEC2/current/us-east-1/index.json");
                ObjectMapper mapper = new ObjectMapper();
                Map<String,Object> map = mapper.readValue(json, Map.class);
                JSONObject job = new JSONObject(map);
                parse(job.toString());
                System.out.println(job.get("version"));

//                String strJson = getJSONFromURL("https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonEC2/current/us-east-1/index.json");
//                try {
//                    JSONParser parser = new JSONParser();
//                    Object object = parser.parse(strJson);
//                    JSONObject mainJsonObject = (JSONObject) object;
//
//                    /*************** First Name ****************/
//                    String firstName = (String) mainJsonObject.get("formatVersion");
//                    System.out.println("First Name : " + 1);
//                    }
//
//                catch(Exception ex) {
//                    ex.printStackTrace();
//                }



            //String jsonString = stream(json);
                //jsonString = jsonString.substring(jsonString.indexOf(instanceType),jsonString.indexOf(instanceType)+10);
                //System.out.println(jsonString);
               /* JSONArray jsonArr = new JSONArray(data);

                JSONObject jsnobject = new JSONObject(readlocationFeed);

                JSONArray jsonArray = jsnobject.getJSONArray("locations");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject explrObject = jsonArray.getJSONObject(i);
                }*/
                // json work ends here
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
    public static String stream(URL url) {
        try (InputStream input = url.openStream()) {
            InputStreamReader isr = new InputStreamReader(input);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder json = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                json.append((char) c);
            }
            return json.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String getJSONFromURL(String strUrl) {
        String jsonText = "";

        try {
            URL url = new URL(strUrl);
            InputStream is = url.openStream();

            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                jsonText += line + "\n";
            }

            is.close();
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        return jsonText;
    }
    public static void parse(String string)
    {
        JSONParser parser = new JSONParser();

        try {
            URL oracle = new URL("://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonEC2/current/us-east-1/index.json"); // URL to Parse

            URLConnection yc = oracle.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                JSONArray a = (JSONArray) parser.parse(inputLine);

                // Loop through each item
                for (Object o : a) {
                    JSONObject tutorials = (JSONObject) o;

                    Long id = (Long) tutorials.get("sku");
                    System.out.println("Post ID : " + id);

                    String title = (String) tutorials.get("productFamily");
                    System.out.println("Post Title : " + title);

                    System.out.println("\n");
                }
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}