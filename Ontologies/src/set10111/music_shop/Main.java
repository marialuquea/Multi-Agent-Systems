package set10111.music_shop;
import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;


public class Main {

	public static void main(String[] args) {
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		try{
			ContainerController myContainer = myRuntime.createMainContainer(myProfile);	
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			
			AgentController sellerAgent = myContainer.createNewAgent("seller", SellerAgent.class.getCanonicalName(), null);
			sellerAgent.start();
			
			AgentController buyerAgent = myContainer.createNewAgent("buyer", CautiousBuyerAgent.class.getCanonicalName(),
					null);
			buyerAgent.start();
			
			AgentController recklessBuyerAgent = myContainer.createNewAgent("reckless buyer", RecklessBuyerAgent.class.getCanonicalName(),
					null);
			recklessBuyerAgent.start();
			
		}
		catch(Exception e){
			System.out.println("Exception starting agent: " + e.toString());
		}


	}

}
