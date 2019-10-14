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
			
			String[] books = {"Java", "Maria"};
			
			//Start the Bidder agents
			AgentController bidderAgent1 = myContainer.createNewAgent("bidder1", Bidder.class.getCanonicalName(), null);
			bidderAgent1.start();
			
			AgentController bidderAgent2 = myContainer.createNewAgent("bidder2", Bidder.class.getCanonicalName(), null);  
			bidderAgent2.start();
			
			//Start the auctioneer agent
			AgentController auctioneerAgent = myContainer.createNewAgent("auctioneer", Auctioneer.class.getCanonicalName(), books);  
			auctioneerAgent.start();
		}
		catch (Exception e)
		{
			System.out.println("Exception starting agent: " + e.toString());
		}
	}

}
