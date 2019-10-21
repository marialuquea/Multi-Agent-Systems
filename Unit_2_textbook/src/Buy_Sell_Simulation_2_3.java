import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class Buy_Sell_Simulation_2_3 
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
		
			//Start the Bidder agents
			AgentController ticker = myContainer.createNewAgent("ticker", BuyerSellerTicker.class.getCanonicalName(), null);
			ticker.start();
			
			AgentController buyerAgent = myContainer.createNewAgent("buyer", BuyerAgent.class.getCanonicalName(), null);  
			buyerAgent.start();
			
			//Start the auctioneer agent
			AgentController sellerAgent = myContainer.createNewAgent("seller", SellerAgent.class.getCanonicalName(), null);  
			sellerAgent.start();
		}
		catch (Exception e)
		{
			System.out.println("Exception starting agent: " + e.toString());
		}
	}
}
