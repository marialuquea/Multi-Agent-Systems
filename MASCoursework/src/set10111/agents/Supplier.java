package set10111.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import set10111.elements.concepts.*;

public class Supplier extends Agent
{
	private Codec codec = new SLCodec();
	private Ontology ontology = CommerceOntology.getInstance();
	private AID tickerAgent;
	private ArrayList<AID> manufacturers = new ArrayList<>();
	private ArrayList<AID> customers = new ArrayList<>();
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
					
					//System.out.println("\nSupplier orders at the start of the day:");
					// decrease day in order to send parts to manufacturer
					for (Entry<SupplierOrder, Integer> entry : orders.entrySet()) 
					{	
						order = entry.getKey();
						orders.put(order, orders.get(order) - 1);
						//System.out.println(entry.getKey().getOrderID() + " = " + entry.getValue());	
					}
					
					ArrayList<Behaviour> cyclicBehaviours = new ArrayList<>();

					CyclicBehaviour ror = new ReceiveOrderRequests(myAgent);
			        myAgent.addBehaviour(ror);
			        cyclicBehaviours.add(ror);

					// activities for the day
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					dailyActivity.addSubBehaviour(new FindManufacturer(myAgent));
					dailyActivity.addSubBehaviour(new FindCustomers(myAgent));
					dailyActivity.addSubBehaviour(new PriceListRequest(myAgent));
					dailyActivity.addSubBehaviour(new SendParts(myAgent));
					//dailyActivity.addSubBehaviour(new EndDayListener(myAgent));
					myAgent.addBehaviour(dailyActivity);
					
					myAgent.addBehaviour(new EndDayListener(myAgent, cyclicBehaviours));

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
				//System.out.println("order "+entry.getKey().getOrderID()+" - days "+entry.getValue());
				if (entry.getValue() == 0)
					count1 += entry.getKey().getQuantity();;
					
			}
			//System.out.println("count1: "+count1);
			
			// SEND INFO TO SUPPLIER
			ACLMessage info = new ACLMessage(ACLMessage.INFORM);
			info.setLanguage(codec.getName());
			info.setOntology(ontology.getName());
			info.setConversationId("parts");
			info.addReceiver(manufacturers.get(0));
			
			PartsSentToday parts = new PartsSentToday();
			parts.setParts(count1);
			
			try 
			{
				getContentManager().fillContent(info, parts);
				send(info);
				//System.out.println("msg info sent: "+info);
			}
			catch (CodecException ce) { ce.printStackTrace(); }
			catch (OntologyException oe) { oe.printStackTrace(); }
			
			
		}
	}

	private class FindCustomers extends OneShotBehaviour 
	{
		public FindCustomers(Agent a) { super(a); }
		@Override
		public void action() {

			DFAgentDescription sellerTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("customer");
			sellerTemplate.addServices(sd);
			try{
				customers.clear();
				DFAgentDescription[] agentsType1  = DFService.search(myAgent,sellerTemplate);
				for(int i=0; i<agentsType1.length; i++){ customers.add(agentsType1[i].getName()); }
				//System.out.println("customers size "+customers.size());
			}
			catch(FIPAException e) { e.printStackTrace(); }
		}
	}
	
	// DONE Send Price List to Manufacturer 
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
	
	//DONE
	private class ReceiveOrderRequests extends CyclicBehaviour
	{
		public ReceiveOrderRequests(Agent a) { super(a); }

		@Override
		public void action() 
		{
			MessageTemplate mt = MessageTemplate.and(
			          MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
			          MessageTemplate.MatchConversationId("requestingParts"));
			ACLMessage msg = receive(mt);
			if(msg != null)
			{
				try
				{	
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					
					Action available = (Action) ce;
					SupplierOrder order = new SupplierOrder();
					order = (SupplierOrder)available.getAction();
					
					orders.put(order, deliveryDays);
					/*
					System.out.println("\norders in supplier");
					for (Entry<SupplierOrder, Integer> entry : orders.entrySet()) 
					{	
						System.out.println(entry.getKey().getOrderID()+" - "+entry.getValue());
					}
					
					System.out.println("Received supply order in "+this.getAgent().getLocalName());
					*/
				}
				catch (CodecException ce) { ce.printStackTrace(); }
				catch (OntologyException oe) { oe.printStackTrace(); }
			}
			else
				block();
		}
	}

	//DONE
	private class SendParts extends Behaviour
	{
		public SendParts(Agent a) { super(a); }
		
		private int expectingPayment;
		private int step = 0;
		
		@Override
		public void action() 
		{	
			switch (step) {
			case 0: 
				// send parts to manufacturer
				for(Iterator<HashMap.Entry<SupplierOrder, Integer>> it = orders.entrySet().iterator(); it.hasNext(); ) 
				{
				    HashMap.Entry<SupplierOrder, Integer> order = it.next();
				    if(order.getValue() == 0) {
				    	
				    	System.out.println("SUPPLIER SENDING PARTS");
						
						// send order back to manufacturer
						ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
						msg.setConversationId("sendingParts");
						msg.addReceiver(manufacturers.get(0));
						msg.setLanguage(codec.getName());
						msg.setOntology(ontology.getName());
						
						Action request = new Action();
						request.setAction(order.getKey());
						request.setActor(manufacturers.get(0));
						try
						{
							getContentManager().fillContent(msg, request); //send the wrapper object
							send(msg);
							//System.out.println("parts sent: "+msg);
						}
						catch (CodecException ce) { ce.printStackTrace(); }
						catch (OntologyException oe) { oe.printStackTrace(); } 
						
						expectingPayment++;
						
				        it.remove();
				    }
				}
				step++;
				break;
				
			case 1:
				// receive payment from manufacturer
				MessageTemplate mt = MessageTemplate.and(
				          MessageTemplate.MatchPerformative(ACLMessage.INFORM),
				          MessageTemplate.MatchConversationId("supplierPayment"));
				ACLMessage msg = receive(mt);
				
				if(msg != null)
				{
					//System.out.println("payment received: "+msg);
			        try 
			        {
			          ContentElement ce = null;
			          ce = getContentManager().extractContent(msg);
			          
			          if (ce instanceof SendPayment) 
			          {
			            SendPayment payment = (SendPayment) ce; 
			            //System.out.println(payment.toString());
			            expectingPayment--;
			          }
			          else 
			            System.out.println("Unknown predicate " + ce.getClass().getName());
			          
			        }
			        catch (CodecException ce) { ce.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); } 
			        
			    } 
				else 
			        block();
			      
				
				break;
			}
				
			
			
		}
		@Override
		public boolean done() {
			return (expectingPayment == 0);
		}
		
	}
	
	public class EndDayListener extends CyclicBehaviour 
	{
		private List<Behaviour> toRemove;

		public EndDayListener(Agent a, List<Behaviour> toRemove) 
		{
			super(a);
			this.toRemove = toRemove;
		}

		@Override
		public void action() {
			ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
			tick.setContent("done");
			tick.addReceiver(tickerAgent);
			myAgent.send(tick);
			
			//remove behaviours
			for(Behaviour b : toRemove) {
				myAgent.removeBehaviour(b);
			}
			myAgent.removeBehaviour(this);
		}
	}
}


