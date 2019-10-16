import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;

public class BookBuyerAgent extends Agent
{
	
	// The title of the book to buy
	private String targetBookTitle;
	//The list of known seller agents
	private AID[] sellerAgents = { new AID("sellerStefan", AID.ISLOCALNAME), new AID("sellerPaul", AID.ISLOCALNAME) };
		
	//Put agent initialisations here
	protected void setup() 
	{
		
		//Print a welcome message
		System.out.println("Hello! Buyer-agent "+getAID().getName()+" is ready.");
		
		// Get the title of the book to buy as a start-up argument
		Object[] args = getArguments();
		
		if (args != null && args.length > 0) 
		{
			
			targetBookTitle = (String) args[0];
			
		 	System.out.println("Trying to buy "+targetBookTitle);
		 	
		 	//Add a TickerBheaviour that schedules a request to seller agents every minute
		 	addBehaviour(new TickerBehaviour(this, 15000) 
		 	{
		 		
		 		protected void onTick() 
		 		{
		 			// Perform the request 
		 			myAgent.addBehaviour(new RequestPerformer());
		 		}
		 	} );
		}
		else {
			
			// Make the agent terminate immediately
			System.out.println("No book title specified");
			doDelete();
		}
	}
	
	// Put agent clean-up operations here
	@Override
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Buyer-agent "+getAID().getName()+" terminating.");
	}
	
	
	
	
	/**
	 Inner class RequestPerformer.
	 This is the behaviour used by Book-buyer agents to request seller
	 agents the target book.
	*/
	private class RequestPerformer extends Behaviour {
		private AID bestSeller; // The agent who provides the best offer
		private int bestPrice; // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		
		public void action() {
			switch (step) 
			{
				case 0:
					// Send the cfp to all sellers
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP); //create empty call for proposal
					for (AID s:sellerAgents) 
					{
						cfp.addReceiver(s);
					}
					cfp.setContent(targetBookTitle);
					cfp.setConversationId("book-trade");
					cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
					myAgent.send(cfp); // send proposal to all sellers
					
					// Prepare the template to get proposals
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
					step = 1;
					break;
				case 1:
					// Receive all proposals/refusals from seller agents
					ACLMessage reply = myAgent.receive(mt); //make a new message which is the reply to the offers/refusals received 
					if (reply != null) 
					{
						// Reply received
						if (reply.getPerformative() == ACLMessage.PROPOSE) // if type of response is a proposal
						{
							// This is an offer
							int price = Integer.parseInt(reply.getContent());
							if (bestSeller == null || price < bestPrice) 
							{
								// This is the best offer at present
								bestPrice = price;
								bestSeller = reply.getSender();
							}
						}
						repliesCnt++;
						if (repliesCnt >= sellerAgents.length) 
						{
							// We received all replies
							step = 2;
						}
					}
					else 
					{
						block();
						// not moving to next step until a reply is received
					}
					break;
				case 2:
					// Send the purchase order to the seller that provided the best offer
					ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL); // new message accepting proposal
					order.addReceiver(bestSeller);
					order.setContent(targetBookTitle);
					order.setConversationId("book-trade");
					order.setReplyWith("order"+System.currentTimeMillis());
					myAgent.send(order);
					// Prepare the template to get the purchase order reply
					// prepare to listen to incoming messages
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
					step = 3;
					break;
				case 3:
					// Receive the purchase order reply
					reply = myAgent.receive(mt);
					if (reply != null) 
					{
						// Purchase order reply received
						if (reply.getPerformative() == ACLMessage.INFORM) //our offer was accepted
						{
							// Purchase successful. We can terminate
							System.out.println(targetBookTitle+" successfully purchased.");
							System.out.println("Price = "+bestPrice);
							myAgent.doDelete();
						}
						step = 4; //move out of the switch
					}
					else 
					{
						block();
					}
					break;
			}
		}
		
		@Override
		public boolean done() 
		{
			return ((step == 2 && bestSeller == null) || step == 4);
		}
	} // End of inner class RequestPerformer
	
	
}
