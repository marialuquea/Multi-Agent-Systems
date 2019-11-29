package set10111.agents;

import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import set10111.elements.SupplierPrices;


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
			
			int numCustomers = 3;
			AgentController customer;
			for(int i=0; i<numCustomers;i++) {
				customer = myContainer.createNewAgent("customer"+ i, Customer.class.getCanonicalName(), null);
				customer.start();
			}
			
			AgentController manufacturer = myContainer.createNewAgent("manufacturer", Manufacturer.class.getCanonicalName(), null);
			manufacturer.start();
			
			AgentController supplier1 = myContainer.createNewAgent("supplier1",
					Supplier.class.getCanonicalName(), new Object[] { SupplierPrices.getPricesSupplier1(), 1});
			supplier1.start();
			
			AgentController supplier2 = myContainer.createNewAgent("supplier2",
					Supplier.class.getCanonicalName(), new Object[] { SupplierPrices.getPricesSupplier2(), 4});
			supplier2.start();
			
			AgentController dayTicker = myContainer.createNewAgent("dayTicker", DayTicker.class.getCanonicalName(), null);
			dayTicker.start();
			
		}
		catch(Exception e){
			System.out.println("Exception starting agent: " + e.toString());
		}
		
	}

}
