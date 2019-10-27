package set10111.agents;

import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;


public class Main 
{
	public static void main(String[] args) 
	{
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		try
		{
			ContainerController myContainer = myRuntime.createMainContainer(myProfile);	
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			
			int numCustomers = 1;
			AgentController customer;
			for(int i=0; i<numCustomers;i++) {
				customer = myContainer.createNewAgent("customer"+ i, Customer.class.getCanonicalName(), null);
				customer.start();
			}
			
			
			AgentController manufacturer = myContainer.createNewAgent("manufacturer", Manufacturer.class.getCanonicalName(),
					null);
			manufacturer.start();
			
			AgentController supplier = myContainer.createNewAgent("supplier", Supplier.class.getCanonicalName(),
					null);
			supplier.start();
			
			AgentController dayTicker = myContainer.createNewAgent("dayTicker", DayTicker.class.getCanonicalName(),
					null);
			dayTicker.start();
			
		}
		catch(Exception e){
			System.out.println("Exception starting agent: " + e.toString());
		}
		
	}

}
