import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class Application 
{
	public static void main(String[] args)
	{
		//Setup JADE environment
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);
		
		try 
		{
			//Start the agent controller (rma)
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
		
			ArrayList<String> read_from_file = read_items();
			
			String[] items = new String[read_from_file.size()];
			items = read_from_file.toArray(items);
			
			
			//Start the Bidder agents
			AgentController bidderAgent1 = myContainer.createNewAgent("bidder1", Bidder.class.getCanonicalName(), null);
			bidderAgent1.start();
			
			AgentController bidderAgent2 = myContainer.createNewAgent("bidder2", Bidder.class.getCanonicalName(), null);  
			bidderAgent2.start();
			
			//Start the auctioneer agent
			AgentController auctioneerAgent = myContainer.createNewAgent("auctioneer", Auctioneer.class.getCanonicalName(), items);  
			auctioneerAgent.start();
		}
		catch (Exception e)
		{
			System.out.println("Exception starting agent: " + e.toString());
		}
	}
	
	
	
	public static ArrayList<String> read_items() 
	{
		ArrayList<String> items_names = new ArrayList<String>();
		String csvFile = System.getProperty("user.dir") + "\\Book1.csv";
        BufferedReader br = null;
        String line = "";

        try {

            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) 
            {
                String[] items = line.split(",");
                String one_item = items[1];
                items_names.add(one_item);
            }
            System.out.println("all_items: " + items_names);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return items_names;
	}

}
