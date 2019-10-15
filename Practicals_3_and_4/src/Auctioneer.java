import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;

import jade.core.AID;

public class Auctioneer extends Agent 
{
	// The title of the book to sell
	private ArrayList<String> itemsToSell = new ArrayList<String>();
	//The list of known seller agents
	private AID[] bidderAgents;

	// long t0 = System.currentTimeMillis();


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


			//count down 15 seconds
			addBehaviour(new TickerBehaviour(this, 1000) 
			{   
				protected void onTick() 
				{
					countdown();
				}
			});
			
			
			for (Object o : args) {
				itemsToSell.add((String) o);
				System.out.println("Trying to sell " + (String) o);
			}
			



			//Add a TickerBheaviour that schedules a request to bidder agents every 15 seconds
			addBehaviour(new TickerBehaviour(this, 15000) 
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
						bidderAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) 
						{
							bidderAgents[i] = result[i].getName();
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Perform the request 
					myAgent.addBehaviour(new RequestPerformer());

				} // end of onTick

			} ); //end of addBehaviour


		}
		else 
		{
			// Make the agent terminate immediately
			System.out.println("No book title specified");
			doDelete();
		}
	} // End of setup()

	int t = 15;

	/**
	 * Count down 15 seconds for each item in the auction
	 */
	protected void countdown()
	{

		if (t > 0)  
		{  
			System.out.println(t + " seconds left");
			t--;
		} else  
		{
			System.out.println("15 seconds of auction ended");
			t = 15;
		}
	}


	/**
	 * Agent clean-up operations
	 */
	protected void takeDown() 
	{
		System.out.println("Auctioneer Agent "+getAID().getName()+" terminating.");
	}


	/**
	 * Inner class RequestPerformer.
	 * This is the behaviour used by auctioneer agents to request bidder
	 * agents the price.
	 */
	private class RequestPerformer extends Behaviour 
	{
		private AID bestBidder; // The agent who provides the best offer
		private int bestPrice; // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		private String itemToSell = itemsToSell.get(0);
		public void action() 
		{

			switch (step) 
			{
			case 0:
				// Send the cfp to all bidders
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < bidderAgents.length; ++i) 
				{
					cfp.addReceiver(bidderAgents[i]);
				}
				cfp.setContent(itemToSell);
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
					if (reply.getPerformative() == ACLMessage.PROPOSE) 
					{
						// This is an offer
						int price = Integer.parseInt(reply.getContent());
						if (bestBidder == null || price > bestPrice) 
						{
							// This is the best offer at present
							bestPrice = price;
							bestBidder = reply.getSender();
						}
					}
					repliesCnt++;

					if (repliesCnt >= bidderAgents.length) 
					{
						// We received all replies
						//System.out.println("15 seconds more gone");
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
				order.setContent(itemToSell); 
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
						System.out.println(itemsToSell+" successfully purchased by "+reply.getSender());
						System.out.println("Best price = "+bestPrice);
						myAgent.doDelete();
					}
					if (itemsToSell.size() == 0) { step = 4; }
					else 
					{ 
						step = 1;
						itemsToSell.remove(0);  // remove first item
					}
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
