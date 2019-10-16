import java.util.Hashtable;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Bidder extends Agent{
	
	// The list of items to bid for
	private Hashtable catalogue;
	
	// The GUI by means of which the user can add items to book for in the catalogue
	private BidderGui myGui;	
	
	
	/**
	* Agent initialisations
	*/
	protected void setup()
	{
		System.out.println("Bidder agent "+getAID().getName()+" starting.");
		
		// Create the catalogue
		catalogue = new Hashtable();

		// Create and show the GUI
		myGui = new BidderGui(this);
		myGui.show();	
		
		// Add the behaviour serving requests for offer from buyer agents
		addBehaviour(new OfferRequestsServer());
				
		// Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());

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
	} // End of setup()
	
	
	
	
	
	
	
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
		System.out.println("Bidder "+getAID().getName()+" terminating.");
	 } // End of takeDown()
	
	
	
	
	
	
	 /**
	 This is invoked by the GUI when the bidder adds a new item to bid for
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
	 } // End of updateCatalogue()
	 
	 
	 
	 
	 
	 
	 /**
	 * Inner class OfferRequestsServer.
	 * This is the behaviour used by Bidder agents to serve incoming requests
	 * for offer from auctioneer agents.
	 * If the requested item is in the local catalogue the auctioneer agent replies
	 * with a PROPOSE message specifying the price. Otherwise a REFUSE message is
	 * sent back.
	*/
	private class OfferRequestsServer extends CyclicBehaviour 
	{
		public void action() 
		{
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) 
			{
				// Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();
				Integer price = (Integer) catalogue.get(title);
				if (price != null) 
				{
					// The requested book is available for sale. Reply with the price
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}
				else 
				{
					// The requested book is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else 
			{
				block();
			}
		}
	} // End of inner class OfferRequestsServer
	
	
	
	
	
	
	/**
	* Inner class PurchaseOrdersServer
	*/
	private class PurchaseOrdersServer extends CyclicBehaviour 
	{
		public void action() 
		{
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) 
			{
				// ACCEPT_PROPOSAL Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();
				Integer price = (Integer) catalogue.remove(title);
				if (price != null) 
				{
					reply.setPerformative(ACLMessage.INFORM);
					// System.out.println(title+" sold to agent "+msg.getSender().getName());
				}
				else 
				{
					// The requested book has been sold to another buyer in the meanwhile .
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else 
			{
				block();
			}
		 }
	} // End of inner class PurchaseOrdersServer

}
