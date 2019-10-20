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

	private int round = 0;
	private Boolean sold = false;
	
	Item item = new Item();


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
			addBehaviour(new TickerBehaviour(this, 1000) // every second print the seconds left
			{   
				protected void onTick() { countdown(); }
			});




			// for each item to sell, add them to list itemsToSell
			for (Object o : args) { itemsToSell.add((String) o); }



			System.out.println("itemsToSell: " + itemsToSell);
			//System.out.println("pwd: "+System.getProperty("user.dir"));

			if (itemsToSell.size() > 0)
			{
				//Add a TickerBheaviour that schedules a request to bidder agents every 20 seconds
				addBehaviour(new TickerBehaviour(this, 20000) 
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

			} //End of:   if (itemsTosell not empty)

		}
		else 
		{
			// Make the agent terminate immediately
			System.out.println("No items to sell");
			doDelete();
		}
	} // End of setup()




	/**
	 * Count down 20 seconds for each item in the auction
	 */
	int t = 20;
	protected void countdown()
	{
		if (t > 0)  // keep the count down going
		{  
			System.out.println(t);
			t--;
		} else  if (sold == true) // if at the end of the round the item was sold
		{
			System.out.println("NOW SELLING " + itemsToSell.get(0));
			
			// next round, next item
			round++;
			System.out.println("round: " + round);
			t = 20;
			sold = false;
		}
		else // the item wasn't sold and the time ended
		{
			// pop first item, push it at the back, keep auction going selling next item
			String item = itemsToSell.get(0);
			itemsToSell.remove(0);
			itemsToSell.add(item);
			System.out.println("NOW SELLING " + itemsToSell.get(0));
			round++;
			System.out.println("round: " + round);
			t = 20;
			//sold = true;
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
		private String itemToSell = itemsToSell.get(0); // first item on the list

		public void action() 
		{
			switch (step) 
			{
			case 0:
				// Send the cfp (call for proposal) to all bidders
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
						System.out.println(itemToSell+" successfully purchased by "+reply.getSender().getName());
						System.out.println("Best price = "+bestPrice);

					}
					if (itemsToSell.size() == 1) // no more things lift to sell
					{ 
						System.out.println("Agent Auctioneer has done its job and is terminating.");
						myAgent.doDelete();
						step = 4; 
					}
					else //sold item, now remove from list of items to sell
					{ 
						sold = true;
						itemsToSell.remove(0);  // remove first item
						System.out.println("itemToSell "+itemToSell+" removed from list.");
						//System.out.println("itemsToSell: "+itemsToSell);
						step = 1;
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
