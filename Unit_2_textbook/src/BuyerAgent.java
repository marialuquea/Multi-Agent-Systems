import java.util.ArrayList;
import java.util.HashMap;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BuyerAgent extends Agent
{
	private ArrayList<AID> sellers = new ArrayList<>();
	private ArrayList<String> booksToBuy = new ArrayList<>();
	private HashMap<String,Offer> bestOffers = new HashMap<>();
	private AID tickerAgent;
	private int numQueriesSent;
	
	@Override
	protected void setup()
	{
		System.out.println("setup() in buyer");
		//add this agent to the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("buyer");
		sd.setName(getLocalName() + "-buyer-agent");
		dfd.addServices(sd);
		try
		{
			DFService.register(this, dfd);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}
		//add books to buy
		booksToBuy.add("Java for Dummies");
		booksToBuy.add("JADE: the Inside Story");
		booksToBuy.add("Multi-Agent Systems for Everybody");
		System.out.println("booksToBuy: " + booksToBuy);
		
		addBehaviour(new TickerWaiter(this));
	} // End of setup()

	@Override
	protected void takeDown()
	{
		//Deregister from the yellow pages
		try 
		{
			System.out.println("takeDown in buyer");
			DFService.deregister(this);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}
	} // End of takeDown()
	
	public class TickerWaiter extends CyclicBehaviour
	{
		//behaviour to wait for a new day
		public TickerWaiter(Agent a)
		{
			super(a);
		}
		
		@Override
		public void action()
		{
			System.out.println("TickerWaiter action() in buyer");
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"), MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null)
			{
				System.out.println("msg in TW in buyer: "+msg);
				if (tickerAgent == null)
					tickerAgent = msg.getSender();
				if (msg.getContent().equals("new day"))
				{
					System.out.println("tickerAgent in buyer: "+tickerAgent);
					//spawn new sequential behaviour or a day's activities
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					//sub-behaviours will execute in the order they are added
					dailyActivity.addSubBehaviour(new FindSellers(myAgent));
					dailyActivity.addSubBehaviour(new SendEnquiries(myAgent));
					dailyActivity.addSubBehaviour(new CollectOffers(myAgent));
					dailyActivity.addSubBehaviour(new EndDay(myAgent));
					myAgent.addBehaviour(dailyActivity);
				}
				else
				{
					//termination message to end simulation
					System.out.println("terminating buyer agent");
					myAgent.doDelete();
				}
			}
			else
			{
				block();
			}
		}
	}
	
	public class FindSellers extends OneShotBehaviour
	{
		public FindSellers(Agent a)
		{
			super(a);
		}
		
		@Override
		public void action()
		{
			System.out.println("FindSellers action() in buyer");
			DFAgentDescription sellerTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("seller");
			sellerTemplate.addServices(sd);
			try
			{
				System.out.println("sellers 1: " + sellers);
				sellers.clear();
				DFAgentDescription[] agentsType1 = DFService.search(myAgent,  sellerTemplate);
				for(int i = 0; i<agentsType1.length; i++)
					sellers.add(agentsType1[i].getName()); // this is the AID
				System.out.println("sellers 2: " + sellers);
			}
			catch (FIPAException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public class SendEnquiries extends OneShotBehaviour
	{
		public SendEnquiries(Agent a)
		{
			super(a);
		}
		
		@Override 
		public void action()
		{
			System.out.println("SendEnquiries in buyer");
			//send out a call for proposal for each book
			numQueriesSent = 0;
			System.out.println("booksToBuy: " + booksToBuy);
			for(String bookTitle : booksToBuy)
			{
				System.out.println("bookTitle: " + bookTitle);
				ACLMessage enquiry = new ACLMessage(ACLMessage.CFP);
				enquiry.setContent(bookTitle);
				enquiry.setConversationId(bookTitle);
				for(AID seller:sellers)
				{
					enquiry.addReceiver(seller);
					System.out.println("enquiry to sellers: " + sellers);
					numQueriesSent++;
				}
				System.out.println("enquiry: " + enquiry);
				myAgent.send(enquiry);
			}
		}
	} // End of class SendEnquiries
	
	public class CollectOffers extends Behaviour
	{
		private HashMap<String,Integer> repliesReceived = new HashMap<>();
		private int numRepliesReceived = 0;
		private boolean finished = false;
		
		public CollectOffers(Agent a)
		{
			super(a);
		}
		
		@Override 
		public void action()
		{
			System.out.println("CollectOffers in buyer");
			boolean received = false;
			System.out.println("booksToBuy in CollectOffers buyer: " + booksToBuy);
			for (String bookTitle:booksToBuy)
			{
				System.out.println("bookTitle in CollectOffers buyer: " + bookTitle);
				//receive proposal messages from seller
				MessageTemplate mt = MessageTemplate.MatchConversationId(bookTitle);
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) 
				{
					received = true;
					numRepliesReceived++;
					System.out.println("numRepliesReceived: " + numRepliesReceived);
					if (msg.getPerformative() == ACLMessage.PROPOSE)
					{
						System.out.println("msg proposal from seller: " + msg);
						// the reply is an offer so see whether to update the best offer
						// update if no existing offer
						System.out.println("bestOffers: " + bestOffers);
						System.out.println("bookTitle: " + bookTitle);
						if (!bestOffers.containsKey(bookTitle))
						{
							bestOffers.put(bookTitle, new Offer(msg.getSender(), Integer.parseInt(msg.getContent())));
							System.out.println("passed1");
						}
						else
						{
							//update only if new offer is better than existing offer
							int newOffer = Integer.parseInt(msg.getContent());
							int existingOffer = bestOffers.get(bookTitle).getPrice();
							
							System.out.println("newOffer: " + newOffer);
							System.out.println("existingOffer: " + existingOffer);
							
							if (newOffer < existingOffer)
							{
								bestOffers.remove(bookTitle);
								bestOffers.put(bookTitle, new Offer(msg.getSender(), newOffer));
								System.out.println("bestOffers912: " + bestOffers);
							}
						}
					}
				}
			}
			if (!received)
			{
				System.out.println("offer not received so block in buyer woah woah");
				block();
			}
		}
		
		@Override
		public boolean done()
		{
			System.out.println("done() in buyer");
			return numRepliesReceived == numQueriesSent;
		}
		
		@Override 
		public int onEnd()
		{
			//print the offers
			for (String book : booksToBuy)
			{
				if (bestOffers.containsKey(book))
				{
					Offer o = bestOffers.get(book);
					System.out.println(book + ", " + o.getSeller() + ", " + o.getPrice());
				}
				else 
				{
					System.out.println("No offers for " + book);
				}
			}
			System.out.println("onEnd() in buyer");
			return 0;
		}
	}
	
	public class EndDay extends OneShotBehaviour
	{
		public EndDay(Agent a)
		{
			super(a);
		}
		
		@Override
		public void action()
		{
			System.out.println("EndDay in buyer");
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(tickerAgent);
			msg.setContent("done");
			myAgent.send(msg);
			//send a message to each seller that we have finished
			ACLMessage sellerDone = new ACLMessage(ACLMessage.INFORM);
			sellerDone.setContent("done");
			for(AID seller : sellers)
				sellerDone.addReceiver(seller);
			myAgent.send(sellerDone);
		}
	}
}



























