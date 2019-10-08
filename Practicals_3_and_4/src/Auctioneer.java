import java.util.Hashtable;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Auctioneer extends Agent{
	
	// The catalogue of books for sale (maps the title of a book to its price)
	private Hashtable catalogue;
	
	// The GUI by means of which the user can add books in the catalogue
	private AuctioneerGui myGui;	
	
	/**
	Agent initialisations
	*/
	protected void setup()
	{
		System.out.println("Auctioneer agent "+getAID().getName()+" starting.");
		
		// Create the catalogue
		catalogue = new Hashtable();

		// Create and show the GUI
		myGui = new AuctioneerGui(this);
		myGui.show();		

		// Register the book-selling service in the yellow pages (DF Agent) 
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-selling");
		sd.setName("JADE-book-trading");
		dfd.addServices(sd);
		try 
		{
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) 
		{
			fe.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	/**
	Agent clean-up operations
	*/
	protected void takeDown() 
	{
		// De-register from the yellow pages
		try 
		{
			DFService.deregister(this);
		}
		catch (FIPAException fe) 
		{
			fe.printStackTrace();
		}
		
		// Close the GUI
		myGui.dispose();
		
		// Printout a dismissal message
		System.out.println("Seller-agent "+getAID().getName()+" terminating.");
	 }
	
	
	
	
	
	
	 /**
	 This is invoked by the GUI when the user adds a new book for sale
	 */
	 public void updateCatalogue(final String title, final int price) 
	 {
		 addBehaviour(new OneShotBehaviour() 
		 {
			 public void action() 
			 {
				 catalogue.put(title, new Integer(price));
			 }
		 } );
	 }

}
