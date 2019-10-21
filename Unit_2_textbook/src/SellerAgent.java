import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class SellerAgent extends Agent
{
	private HashMap<String, Integer> booksForSale = new HashMap<>();
	private AID tickerAgent;
	private ArrayList<AID> buyers = new ArrayList<>();
	
	@Override
	protected void setup()
	{
		System.out.println("setup() in seller, register with DF");
		//add this agent to the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("seller");
		sd.setName(getLocalName() + "-seller-agent");
		dfd.addServices(sd);
		try
		{
			DFService.register(this, dfd);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}
		addBehaviour(new TickerWaiter(this));
	} // End of setup()
	
	/*
	 * Behaviour to wait for the next day
	 */
	public class TickerWaiter extends CyclicBehaviour
	{
		public TickerWaiter(Agent a)
		{
			super(a);
		}
		
		@Override
		public void action()
		{
			// receive messages
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"), MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null)
			{
				
				if (tickerAgent == null)
					tickerAgent = msg.getSender(); // find sender of message received
				if (msg.getContent().equals("new day"))
				{
					System.out.println("NewDay msg received in seller: "+msg.getContent());
					
					// DAILY ACTIVITIES
					myAgent.addBehaviour(new BookGenerator());
					myAgent.addBehaviour(new FindBuyers(myAgent));
					CyclicBehaviour os = new OffersServer(myAgent);
					myAgent.addBehaviour(os);
					ArrayList<Behaviour> cyclicBehaviours = new ArrayList<>();
					cyclicBehaviours.add(os);
					myAgent.addBehaviour(new EndDayListener(myAgent, cyclicBehaviours));
				}
				else
				{
					//termination message to end simulation
					// when message == "termination" and not "new day"
					myAgent.doDelete();
				}
			}
			else
			{
				//System.out.println("msg newday/termination not received in seller");
				block();
			}
		}
		
		/*
		 * Add stock to the agent for that day
		 */
		public class BookGenerator extends OneShotBehaviour
		{
			@Override
			public void action()
			{
				booksForSale.clear();
				//select one book for sale per day
				int rand = (int)Math.round((1+2 * Math.random()));
				//price will be between 1 and 50 GBP
				int price = (int)Math.round((1 + 49 * Math.random()));
				switch(rand)
				{
				case 1:
					booksForSale.put("Java for Dummies", price);
					System.out.println("BooksForSale: " + booksForSale);
					break;
				case 2:
					booksForSale.put("JADE: the Inside Story", price);
					System.out.println("BooksForSale: " + booksForSale);
					break;
				case 3:
					booksForSale.put("Multi-Agent Systems for Everybody", price);
					System.out.println("BooksForSale: " + booksForSale);
					break;
				}
			}
		}
		
		
		/*
		 * Find AIDs of all buyer agents
		 */
		public class FindBuyers extends OneShotBehaviour
		{
			public FindBuyers(Agent a)
			{
				super(a);
			}
			
			@Override
			public void action()
			{
				// Register with DF so sellers can be found by buyers
				DFAgentDescription buyerTemplate = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				// Find buyers
				sd.setType("buyer");
				buyerTemplate.addServices(sd);
				try
				{
					buyers.clear();
					DFAgentDescription[] agentsType1 = DFService.search(myAgent, buyerTemplate);
					for(int i = 0; i<agentsType1.length; i++)
						buyers.add(agentsType1[i].getName());
					System.out.println("Buyers: " + buyers);
				}
				catch (FIPAException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	/*
	 * Does most of the work.
	 * listen for calls for proposals from buyers and reply
	 * with either PROPOSE or REFUSE message
	 */
	public class OffersServer extends CyclicBehaviour
	{
		public OffersServer(Agent a)
		{
			super(a);
		}
		
		@Override
		public void action()
		{
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null)
			{
				System.out.println("Proposal received by seller: "+msg.getContent());
				ACLMessage reply = msg.createReply();
				String book = msg.getContent();
				if (booksForSale.containsKey(book))
				{
					//we can send an offer
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(booksForSale.get(book)));
				}
				else
				{
					reply.setPerformative(ACLMessage.REFUSE);
				}
				myAgent.send(reply);
			}
			else 
			{
				block();
			}
		}
	}
	
	/*
	 * At the end of the day, any cylic behaviours will need to be removed
	 * from the agent's behaviour queue so that they can re-added fresh the 
	 * following day.
	 * We add cyclic behaviours to a list, so that we can pass references 
	 * to them to the EndDayListener behaviour.
	 * 
	 * This behaviour waits for each buyer to send it a “done” message 
	 * (and is itself a cyclic behaviour). When it has received “done” from 
	 * each buyer it removes all of the cyclic behaviours (including itself). 
	 * These will be re-created when the TickerWaiter receives a “new day” 
	 * message from the ticker agent.
	 */
	public class EndDayListener extends CyclicBehaviour
	{
		private int buyersFinished = 0;
		private List<Behaviour> toRemove;
		
		public EndDayListener(Agent a, List<Behaviour> toRemove)
		{
			super(a);
			this.toRemove = toRemove;
		}
		
		@Override
		public void action() 
		{
			MessageTemplate mt = MessageTemplate.MatchContent("done");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null)
			{
				System.out.println("Done msg of buyer received by seller: "+msg.getContent());
				buyersFinished++;
			}
			else
			{
				block();
			}
			if (buyersFinished == buyers.size())
			{
				//we are finished 
				ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
				tick.setContent("done");
				tick.addReceiver(tickerAgent);
				myAgent.send(tick);
				
				System.out.println("Done msg sent by seller: "+tick.getContent());
				
				//remove behaviours 
				for(Behaviour b : toRemove)
					myAgent.removeBehaviour(b);
				myAgent.removeBehaviour(this);
			}
		}
	}
	
}
















