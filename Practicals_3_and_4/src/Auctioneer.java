import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;

public class Auctioneer extends Agent 
{
	// The title of the book to buy
	private String targetBookTitle;
	//The list of known seller agents
	private AID[] bidderAgents;
	
	long t0 = System.currentTimeMillis();
	
		
	/**
	 * Agent initialisations
	 */
	protected void setup()
	{
		//Print a welcome message
		System.out.println("Auctioneer Agent "+getAID().getName()+" is ready.");
				
		// Get the title of the book to buy as a start-up argument
		Object[] args = getArguments();
		
		if (args != null && args.length > 0) // if argument not empty
		{
			targetBookTitle = (String) args[0]; 
		 	System.out.println("Trying to sell "+targetBookTitle);
		 	
		 	addBehaviour(new TickerBehaviour(this, 1000) 
		 	{
		 		protected void onTick() 
		 		{
		 			System.out.println((System.currentTimeMillis()-t0)/1000 + " seconds");
		 		}
		 	});
		 	
		 	//Add a TickerBheaviour that schedules a request to seller agents every 10 seconds
		 	addBehaviour(new TickerBehaviour(this, 10000) 
		 	{
		 		protected void onTick() 
		 		{
		 			// Update the list of bidder agents / Register with DF agent
		 			DFAgentDescription template = new DFAgentDescription();
		 			ServiceDescription sd = new ServiceDescription();
		 			sd.setType("book-selling");
		 			template.addServices(sd);
		 			try 
		 			{
		 				DFAgentDescription[] result = DFService.search(myAgent, template);
		 				
		 				System.out.println("-------------------------------------");
		 				System.out.println("result: "+result);
		 				//System.out.println("bidderAgents.length: "+bidderAgents.length);
		 				System.out.println("result.length: "+result.length);
		 				System.out.println("-------------------------------------");
		 				
		 				bidderAgents = new AID[result.length];
		 				for (int i = 0; i < result.length; ++i) 
		 				{
		 					bidderAgents[i] = result[i].getName();
		 					System.out.println("bidderAgents.length2: "+bidderAgents.length);
		 					System.out.println("bidderAgents[i]: "+bidderAgents[i]);
		 				}
		 			}
		 			catch (FIPAException fe) {
		 				fe.printStackTrace();
		 			}

		 			
		 			// Perform the request 
		 			myAgent.addBehaviour(new RequestPerformer());
		 		}
		 	} );
		}
		else 
		{
			// Make the agent terminate immediately
			System.out.println("No book title specified");
			doDelete();
		}
	} // End of setup()
	
	
	
	/**
	 * Agent clean-up operations
	 */
	protected void takeDown() 
	{
	// Printout a dismissal message
		System.out.println("Auctioneer Agent "+getAID().getName()+" terminating.");
	}
	
	
	/**
	 Inner class RequestPerformer.
	 This is the behaviour used by auctioneer agents to request bidder
	 agents the price.
	*/
	private class RequestPerformer extends Behaviour 
	{
		private AID bestBidder; // The agent who provides the best offer
		private int bestPrice; // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		public void action() 
		{
			switch (step) 
			{
				case 0:
					// Send the cfp to all sellers
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
					for (int i = 0; i < bidderAgents.length; ++i) 
					{
						cfp.addReceiver(bidderAgents[i]);
					}
					cfp.setContent(targetBookTitle);
					cfp.setConversationId("book-trade");
					cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value (seed)
					myAgent.send(cfp);
					// Prepare the template to get proposals
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), 
											 MessageTemplate.MatchInReplyTo(cfp.getReplyWith())
											);
					step = 1;
					break;
				case 1:
					// Receive all proposals/refusals from bidder agents
					ACLMessage reply = myAgent.receive(mt);
					if (reply != null) // Reply received
					{
						System.out.println("reply: "+reply);
						if (reply.getPerformative() == ACLMessage.PROPOSE) 
						{
							// This is an offer
							int price = Integer.parseInt(reply.getContent());
							if (bestBidder == null || price > bestPrice) 
							{
								System.out.println("new best price: "+price);
								System.out.println("new best bidder: "+reply.getSender());
								// This is the best offer at present
								bestPrice = price;
								bestBidder = reply.getSender();
							}
						}
						repliesCnt++;
						
						System.out.println("repliesCnt: "+repliesCnt);
						System.out.println("bidderAgents.length: "+bidderAgents.length);
						
						if (repliesCnt >= bidderAgents.length) 
						{
							// We received all replies
							System.out.println("All replies received");
							step = 2;
						}
					}
					else // no reply
					{
						block();
					}
					break;
				case 2:
					// Send the purchase order to the seller that provided the best offer
					ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					order.addReceiver(bestBidder);
					order.setContent(targetBookTitle);
					order.setConversationId("book-trade");
					order.setReplyWith("order"+System.currentTimeMillis());
					myAgent.send(order);
					// Prepare the template to get the purchase order reply
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), 
											 MessageTemplate.MatchInReplyTo(order.getReplyWith())
											);
					step = 3;
					break;
				case 3:
					// Receive the purchase order reply
					reply = myAgent.receive(mt);
					if (reply != null) 
					{
						// Purchase order reply received
						if (reply.getPerformative() == ACLMessage.INFORM) 
						{
							// Purchase successful. We can terminate
							System.out.println(targetBookTitle+" successfully purchased by "+reply.getSender());
							System.out.println("Best price = "+bestPrice);
							myAgent.doDelete();
						}
						step = 4;
					}
					else 
					{
						block();
					}
					break;
			}
		}
		public boolean done() 
		{
			return ((step == 2 && bestBidder == null) || step == 4);
		}
	} // End of inner class RequestPerformer
	
}
