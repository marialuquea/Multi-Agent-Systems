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
import set10111.ontology.CommerceOntology;
import set10111.predicates.*;
import set10111.elements.*;
import set10111.elements.concepts.SmartphoneComponent;

public class Supplier extends Agent
{
	private Codec codec = new SLCodec();
	private Ontology ontology = CommerceOntology.getInstance();
	private AID tickerAgent;
	private ArrayList<AID> manufacturers = new ArrayList<>();
	private SupplierOrder order = new SupplierOrder();
	private HashMap<SupplierOrder, Integer> orders = new HashMap<>(); 
	private HashMap<SmartphoneComponent, Integer> supplies;
	private int deliveryDays;

	protected void setup()
	{
		//System.out.println("setup() in "+this.getLocalName());
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
		
		// Get price list and delivery days
	    Object[] args = getArguments();
	    if (args != null && args.length > 1) {
	    supplies = (HashMap<SmartphoneComponent, Integer>) args[0];
	    deliveryDays = (int) args[1];
	    }

		addBehaviour(new TickerWaiter(this));
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
		public TickerWaiter(Agent a) { super(a); }

		@Override
		public void action() 
		{	
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
					MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt); 
			if(msg != null) 
			{
				//System.out.println("orders _s size: "+orders.size());
				//System.out.println("msg received in supplier: "+msg.getContent());

				if(tickerAgent == null) 
					tickerAgent = msg.getSender();

				if(msg.getContent().equals("new day")) 
				{
					// new day - reset values
					
					// decrease day in order to send parts to manufacturer
					for (Entry<SupplierOrder, Integer> entry : orders.entrySet()) 
					{	
						order = entry.getKey();
						orders.put(order, orders.get(order) - 1);
						//System.out.println(entry.getKey().getCustomer().getLocalName() + " = " + entry.getValue());	
					}

					// activities for the day
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					dailyActivity.addSubBehaviour(new FindManufacturer(myAgent));
					dailyActivity.addSubBehaviour(new PriceListRequest(myAgent));
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
		public void action() 
		{
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
			for (Entry<SupplierOrder, Integer> entry : orders.entrySet()) 
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

	// Send Price List to Manufacturer
	private class PriceListRequest extends Behaviour
	{
		public PriceListRequest(Agent a) { super(a); }
		boolean received = false;
		@Override
		public void action()
		{
			MessageTemplate mt = MessageTemplate.and(
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
					MessageTemplate.MatchConversationId("parts-prices"));
			ACLMessage msg = myAgent.receive(mt);
			
			if (msg != null) 
			{
				//System.out.println("Price list request received in "+this.getAgent().getLocalName());
				ACLMessage reply = msg.createReply(); 
	            reply.setPerformative(ACLMessage.INFORM);
	            reply.setConversationId("price-list");
	              
	            // separate hashmaps in 2 lists to send
	            ArrayList<SmartphoneComponent> components = new ArrayList<>();
	            ArrayList<Long> componentQuantities = new ArrayList<>();
	            
	            for (Entry<SmartphoneComponent, Integer> part : supplies.entrySet())
	            {
	            	SmartphoneComponent comp = part.getKey();
	            	long quantity = part.getValue();
	            	components.add(comp);
	            	componentQuantities.add(quantity);
	            }
	            
	            
	            PriceList prices = new PriceList();
	            prices.setSupplier(this.getAgent().getAID());
	            prices.setSpeed(deliveryDays);
	            prices.setKeys(components);
	            prices.setValues(componentQuantities);
	            
				try
				{      
					getContentManager().fillContent(reply, prices); //send the wrapper object
					send(reply);
					received = true;
				}
				catch (CodecException ce) { ce.printStackTrace(); }
				catch (OntologyException oe) { oe.printStackTrace(); } 
			}
			
		}
		public boolean done() {
			return received;
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
						order = (SupplierOrder) available.getAction(); // this is the order requested

						//TODO: if supplier 1 then 1 day, if supplier 2 then 4 days

						orders.put(order, 1); 
						//System.out.println("order received: "+order.getBatteryQuantity());

						order_count++;
						//System.out.println("count: "+order_count);

					}
					catch (CodecException ce) { ce.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); }
				}
				else
					block();
			}
			while (order_count < 2);
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
			for (Entry<SupplierOrder, Integer> entry : orders.entrySet()) 
			{	
				
				SupplierOrder order1;
				int days;
				
				order1 = entry.getKey();
				days = entry.getValue();
				
				if (days == 0)
				{
					//System.out.println("days == 0");
					
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
						// System.out.println("msg sent: "+msg);
					}
					catch (CodecException ce) { ce.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); } 
					
					//delete order from orders hashmap
					orders.remove(entry);
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
