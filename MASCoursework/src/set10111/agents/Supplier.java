package set10111.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import set10111.ontology.CommerceOntology;
import set10111.elements.*;

public class Supplier extends Agent
{
	private Codec codec = new SLCodec();
	private Ontology ontology = CommerceOntology.getInstance();
	private AID tickerAgent;
	private ArrayList<AID> manufacturers = new ArrayList<>();
	private Order order = new Order();
	private Smartphone smartphone = new Smartphone();
	private HashMap<Order, Integer> orders = new HashMap<>(); 
	private int count = 0;

	protected void setup()
	{
		//System.out.println("setup() in Supplier");
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		//add this agent to the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("supplier");
		sd.setName(getLocalName() + "-supplier-agent");
		dfd.addServices(sd);
		try{
			DFService.register(this, dfd);
		}
		catch(FIPAException e){
			e.printStackTrace();
		}

		addBehaviour(new TickerWaiter(this));
		//addBehaviour(new ReceiveOrderRequests(this));
	}

	@Override
	protected void takeDown() {
		//Deregister from the yellow pages
		try{
			DFService.deregister(this);
		}
		catch(FIPAException e){
			e.printStackTrace();
		}
	}

	//behaviour to wait for a new day
	public class TickerWaiter extends CyclicBehaviour 
	{
		public TickerWaiter(Agent a) {
			super(a);
		}

		@Override
		public void action() 
		{	
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
					MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt); 
			if(msg != null) 
			{
				//System.out.println("msg received in supplier: "+msg.getContent());

				if(tickerAgent == null) 
					tickerAgent = msg.getSender();

				if(msg.getContent().equals("new day")) 
				{
					// new day - reset values
					
					// decrease day in order to send parts to manufacturer
					for (Entry<Order, Integer> entry : orders.entrySet()) 
					{	
						Order order1 = new Order();
						order1 = entry.getKey();
						orders.put(order1, orders.get(order1) - 1);
						//System.out.println(entry.getKey().getCustomer().getLocalName() + " = " + entry.getValue());	
					}



					// activities for the day
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					dailyActivity.addSubBehaviour(new FindManufacturer(myAgent));
					dailyActivity.addSubBehaviour(new ReceiveOrderRequests(myAgent));
					dailyActivity.addSubBehaviour(new SendParts(myAgent));
					dailyActivity.addSubBehaviour(new EndDay(myAgent));
					myAgent.addBehaviour(dailyActivity);

				}
				else 
					myAgent.doDelete();
			}
			else
				block();
		}

	}

	private class FindManufacturer extends OneShotBehaviour 
	{
		public FindManufacturer(Agent a) { super(a); }
		@Override
		public void action() {

			DFAgentDescription sellerTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("manufacturer");
			sellerTemplate.addServices(sd);
			try{
				manufacturers.clear();
				DFAgentDescription[] agentsType1  = DFService.search(myAgent,sellerTemplate);
				for(int i=0; i<agentsType1.length; i++){ manufacturers.add(agentsType1[i].getName()); }
				//System.out.println("manufacturers size "+manufacturers.size());
			}
			catch(FIPAException e) { e.printStackTrace(); }
			
			//let manufacturer know how many orders will be sent to him today
			int count1 = 0;
			for (Entry<Order, Integer> entry : orders.entrySet()) 
			{	
				int days = 0;
				days = entry.getValue();
				if (days == 0)
					count1++;
			}
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setConversationId("parts");
			msg.setContent(String.valueOf(count1));
			msg.addReceiver(manufacturers.get(0));
			myAgent.send(msg);
			//System.out.println("msg info sent: "+msg);
		}
	}

	private class ReceiveOrderRequests extends OneShotBehaviour
	{
		private int order_count = 0;
		
		public ReceiveOrderRequests(Agent a) { super(a); }

		@Override
		public void action() 
		{
			do 
			{
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
				ACLMessage msg = receive(mt);
				if(msg != null)
				{
					try
					{	
						
						ContentElement ce = null;
						ce = getContentManager().extractContent(msg);

						Action available = (Action) ce;
						order = (Order) available.getAction(); // this is the order requested
						smartphone = order.getSpecification();

						count++;
						//System.out.println("supplier count received: "+count);

						//int supplier1_days = 1;
						//int supplier2_days = 4;

						//TODO: if supplier 1 then 1 day, if supplier 2 then 4 days
						// maybe add private int supplier in Order and manufacturer sets it

						orders.put(order, 1); 

						order_count++;
						//System.out.println("count: "+order_count);

					}
					catch (CodecException ce) { ce.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); }
				}
				else
					block();
			}
			while (order_count < 3);
			//System.out.println("while loop finished");
		}
	}

	private class SendParts extends OneShotBehaviour
	{
		public SendParts(Agent a) { super(a); }
		@Override
		public void action() 
		{
			// Send parts that have day 0 to manufacturer
			for (Entry<Order, Integer> entry : orders.entrySet()) 
			{	
				Order order1 = new Order();
				int days = 0;
				
				order1 = entry.getKey();
				days = entry.getValue();
				
				if (days == 0)
				{
					//System.out.println("battery part sent: " + order1.getSpecification().getBattery());	
					
					// send order back to manufacturer
					ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
					msg.addReceiver(manufacturers.get(0));
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
					
					Action request = new Action();
					request.setAction(order1);
					request.setActor(manufacturers.get(0));
					try
					{
						getContentManager().fillContent(msg, request); //send the wrapper object
						send(msg);
						//System.out.println("msg sent: "+msg);
					}
					catch (CodecException ce) { ce.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); } 
					
					//TODO: delete order from orders hashmap
				}
			}
			
		}
		
	}
	
	public class EndDay extends OneShotBehaviour {

		public EndDay(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			//System.out.println("end day supplier");
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(tickerAgent);
			msg.setContent("done");
			myAgent.send(msg);
		}

	}

}