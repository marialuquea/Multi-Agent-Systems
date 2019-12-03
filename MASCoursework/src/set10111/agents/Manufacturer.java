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
import jade.content.onto.UngroundedException;
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
import set10111.elements.concepts.Screen;
import set10111.elements.concepts.SmartphoneComponent;
import set10111.elements.concepts.Storage;

public class Manufacturer extends Agent
{
	private Codec codec = new SLCodec();
	private Ontology ontology = CommerceOntology.getInstance();
	private ArrayList<AID> customers = new ArrayList<>();
	private ArrayList<AID> suppliers = new ArrayList<>();
	private ArrayList<CustomerOrder> readyToAssemble = new ArrayList<>();
	private AID tickerAgent;
	private CustomerOrder order = new CustomerOrder();
	private SupplierOrder supOrder = new SupplierOrder();
	private Smartphone phone = new Smartphone();
	private HashMap<Integer, CustomerOrder> orders = new HashMap<>();
	private HashMap<String, Integer> warehouse = new HashMap<>();
	private HashMap<String, Integer> supplier1prices = new HashMap<>();
	private HashMap<String, Integer> supplier2prices = new HashMap<>();
	private int supplier1deliveryDays;
	private int supplier2deliveryDays;
	private int partsComingToday = 0;
	private int day;
	private int phoneAssembledCount = 0;
	private int orderID = 0;
	private long dailyProfit = 0;
	private long totalProfit = 0;

	protected void setup()
	{
		//System.out.println("setup() in "+this.getLocalName());
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		//add this agent to the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("manufacturer");
		sd.setName(getLocalName() + "-manufacturer-agent");
		dfd.addServices(sd);
		try{
			DFService.register(this, dfd);
		}
		catch(FIPAException e){
			e.printStackTrace();
		}
		
		addBehaviour(new TickerWaiter(this));
		addBehaviour(new ReceiveSupplies(this));
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
	private class TickerWaiter extends CyclicBehaviour 
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
				if(tickerAgent == null) 
					tickerAgent = msg.getSender();

				if(msg.getContent().equals("new day")) 
				{
					// reset values for new day
					customers.clear();
					partsComingToday = 0;
					phoneAssembledCount = 0;
					day++;
					dailyProfit = 0;
					System.out.println("Day "+ day);
					
					for (Entry<Integer, CustomerOrder> entry : orders.entrySet()) 
					{	
						order = entry.getValue();
						order.setDaysDue(order.getDaysDue() - 1);
					}
					
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					dailyActivity.addSubBehaviour(new FindCustomers(myAgent));
					dailyActivity.addSubBehaviour(new FindSuppliers(myAgent));
					dailyActivity.addSubBehaviour(new AskSupInfo(myAgent));
					dailyActivity.addSubBehaviour(new ReceivePartsPrices(myAgent));
					dailyActivity.addSubBehaviour(new ReceiveOrderQuery(myAgent));
					dailyActivity.addSubBehaviour(new ReceiveOrderRequests(myAgent));
					dailyActivity.addSubBehaviour(new OrderPartsFromSupplier(myAgent));
					dailyActivity.addSubBehaviour(new ReceiveSuppliesInfo(myAgent));
					dailyActivity.addSubBehaviour(new AssembleCustomerOrder(myAgent));
					dailyActivity.addSubBehaviour(new EndDay());
					myAgent.addBehaviour(dailyActivity);


				}
				else 
					myAgent.doDelete();
			}
			else
				block();
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

	private class FindSuppliers extends OneShotBehaviour 
	{
		public FindSuppliers(Agent a) { super(a); }
		@Override
		public void action() {

			DFAgentDescription sellerTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("supplier");
			sellerTemplate.addServices(sd);
			try{
				suppliers.clear();
				DFAgentDescription[] agentsType1  = DFService.search(myAgent,sellerTemplate);
				for(int i=0; i<agentsType1.length; i++){ suppliers.add(agentsType1[i].getName()); }
				//System.out.println("suppliers size "+suppliers.size());
			}
			catch(FIPAException e) { e.printStackTrace(); }
		}
	}

	private class AskSupInfo extends OneShotBehaviour
	{
		public AskSupInfo(Agent a) { super(a); }
		
		@Override
		public void action() 
		{
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setLanguage(codec.getName());
			msg.setOntology(ontology.getName());
			msg.setConversationId("parts-prices");
			for (AID sup : suppliers) 
			{
				msg.addReceiver(sup);
				myAgent.send(msg);
				//System.out.println("REQUEST price list from "+sup.getLocalName()+" done");
				msg.removeReceiver(sup);
			}
		}
	}
	
	private class ReceivePartsPrices extends Behaviour
	{
		public ReceivePartsPrices(Agent a) { super(a); }
		int received = 0;
		
		public void action()
		{
			MessageTemplate mt = MessageTemplate.and(
					MessageTemplate.MatchPerformative(ACLMessage.INFORM),
		            MessageTemplate.MatchConversationId("price-list"));
			ACLMessage msg = receive(mt);
			
			if (msg != null)
			{
				try 
				{
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					
					if (ce instanceof PriceList) 
					{
						PriceList supPrices = (PriceList) ce;
						
						HashMap<String, Integer> priceList = new HashMap<>();
						ArrayList<SmartphoneComponent> keys = supPrices.getKeys();
						ArrayList<Long> values = supPrices.getValues();
						
						for(int i = 0; i < keys.size(); i++)
						{
							String sc = (String)keys.get(i).toString();
							//System.out.println("added to price list:\t"+sc);
							int price = values.get(i).intValue();
							priceList.put(sc, price);
						}
						
						AID supplier = supPrices.getSupplier();
						int speed = supPrices.getSpeed();
						if (supplier.getLocalName().equals("supplier1")) {
							supplier1prices = priceList;
							supplier1deliveryDays = speed;
						}
						if (supplier.getLocalName().equals("supplier2")) {
							supplier2prices = priceList;
							supplier2deliveryDays = speed;
						}
						received++;
					}
				}
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
			}
			else
				block();
			
		}
		public boolean done() {
			return (received >= 2);
		}
	}
	
	// receive OrderQuery from customer
	private class ReceiveOrderQuery extends Behaviour
	{
		public ReceiveOrderQuery(Agent a) { super(a); }
		private int received = 0;
		
		public void action()
		{
			MessageTemplate mt = MessageTemplate.and(
					MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF),
					MessageTemplate.MatchConversationId("customer-order-query"));
			ACLMessage msg = receive(mt);
			if (msg != null)
			{
				try 
				{
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					if (ce instanceof OrderQuery)
					{
						OrderQuery orderAccepted = (OrderQuery) ce;
						CustomerOrder order = orderAccepted.getOrder();
						order.setCustomer(msg.getSender());
						
						// choose best supplier for this order
						AID bestSupplier = null;
						double maxProfit, expectedProfit, bestSupplierCost;
						maxProfit = expectedProfit = bestSupplierCost = 0;
						int lateDeliveryFee, daysLate = 0;
						
						for (AID supplier : suppliers) 
						{
							// calculate cost of order
							double totalCost = 0;
							for (SmartphoneComponent c : order.getSpecification().getComponents())
							{
								if (supplier.getLocalName().equals("supplier1")) 
									totalCost += supplier1prices.get(c.toString());
								// supplier 2 only has storage and ram so buy some parts from supplier 2 
								// and others from supplier 1
								if (supplier.getLocalName().equals("supplier2")) {
									if (c.toString().contains("Storage") || c.toString().contains("Ram")) {
										totalCost += supplier2prices.get(c.toString());
									}
									else 
										totalCost += supplier1prices.get(c.toString());
								}
							}
							totalCost *= order.getQuantity();
							if (supplier.getLocalName().equals("supplier2"))
								daysLate = supplier1deliveryDays - order.getDaysDue(); // will give negative number if no penalty fee is to be paid
							else 
								daysLate = supplier2deliveryDays - order.getDaysDue();
								
							if (daysLate > 0) 
								lateDeliveryFee = daysLate * order.getPenalty();
							else
								lateDeliveryFee = 0;
							
							expectedProfit = (order.getPrice() * order.getQuantity()) 
									- totalCost - lateDeliveryFee;
							
							// Decide if this supplier is better than the others
							if ((bestSupplier == null && expectedProfit > 0)
									|| (expectedProfit > maxProfit)) {
								// if there is no best supplier, get the first that grants some profit
								bestSupplier = supplier;
								maxProfit = expectedProfit;
								bestSupplierCost = totalCost;
							}
						}
						
						//Send reply to customer
						ACLMessage reply = msg.createReply();
						reply.setConversationId("customerOrder-answer");
						if (bestSupplier != null) // if profit is positive
						{
							order.setSupplier(bestSupplier);
							order.setCost(bestSupplierCost);
							order.setAccepted(true);
							orders.put(order.getId(), order);
							
							reply.setPerformative(ACLMessage.CONFIRM);
						}
						else
							reply.setPerformative(ACLMessage.DISCONFIRM);
					
						myAgent.send(reply);
						//System.out.println("Manufacturer replied to all customer orders");
						received++;
					}
				} 
				catch (CodecException e) { e.printStackTrace(); } 
				catch (OntologyException e) { e.printStackTrace(); }
			}
			else
				block();
		}
		
		@Override
		public boolean done() {
			return received == customers.size();
		}
		
	}

	// behaviour to receive customer requests that were accepted
	private class ReceiveOrderRequests extends Behaviour
	{
		public ReceiveOrderRequests(Agent a) { super(a); }
		
		private boolean done = false;
		
		@Override
		public void action() 
		{
				// receive order REQUEST messages from customers
				MessageTemplate mt = MessageTemplate.and(
						MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
						MessageTemplate.MatchConversationId("FinalOrderRequest"));
				ACLMessage msg = receive(mt);
				if(msg != null)
				{
					try
					{	
						ContentElement ce = null;
						ce = getContentManager().extractContent(msg);

						Action available = (Action) ce;
						order = (CustomerOrder) available.getAction(); // this is the order requested
						order = orders.get(order.getId());
						phone = order.getSpecification();
						
						//System.out.println("Order request: \t "+order);
						done = true;
					}
					catch (CodecException ce) { ce.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); }
				}
				else
					block();
		}

		@Override
		public boolean done() {
			return done;
		}
	}
	
	// DONE
	private class OrderPartsFromSupplier extends Behaviour
	{
		public OrderPartsFromSupplier(Agent a) { super(a); }
		private boolean done = false;
		@Override
		public void action() 
		{
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setLanguage(codec.getName());
			msg.setOntology(ontology.getName());
			msg.setConversationId("requestingParts");
			msg.addReceiver(order.getSupplier());
			
			supOrder.setSmartphone(order.getSpecification());
			supOrder.setQuantity(order.getQuantity());
			supOrder.setSupplier(order.getSupplier());
			supOrder.setOrderID(order.getId());
			
			// WE NEED THIS WHEN REQUESTING AN ACTION
			Action request = new Action();
			request.setAction(supOrder);
			request.setActor(order.getSupplier());
			
			try
			{
				getContentManager().fillContent(msg, request);
				send(msg);
				done = true;
			}
			catch (CodecException ce) { ce.printStackTrace(); }
			catch (OntologyException oe) { oe.printStackTrace(); }
			
		}

		@Override
		public boolean done() {
			return done;
		}
		
	}
	
	//how many parts are coming in today
	private class ReceiveSuppliesInfo extends OneShotBehaviour
	{
		public ReceiveSuppliesInfo(Agent a) { super(a); }
		
		@Override
		public void action() 
		{
			int messages = 0;
			while (messages <= 1) 
			{
				//System.out.println("messages: "+messages);
				MessageTemplate mt = MessageTemplate.MatchConversationId("parts");
				ACLMessage infomsg = myAgent.receive(mt);
				if(infomsg != null) 
				{
					if (infomsg.getSender().getLocalName().equals("supplier1")) {
						partsComingToday += Integer.parseInt(infomsg.getContent(), 10);
						//System.out.println(partsComingToday + " parts from supplier1 coming in today");
						messages++;
					}
					if (infomsg.getSender().getLocalName().equals("supplier2")) {
						partsComingToday += Integer.parseInt(infomsg.getContent(), 10);
						//System.out.println(partsComingToday + " parts from supplier2 coming in today");
						messages++;
					}
				}
				else 
					block();
			}
			
		}
		
	}


	private class ReceiveSupplies extends CyclicBehaviour
	{
		public ReceiveSupplies(Agent a) { super(a); }
		
		@Override
		public void action() 
		{
			MessageTemplate mt = MessageTemplate.and(
					MessageTemplate.MatchPerformative(ACLMessage.CONFIRM), 
					MessageTemplate.MatchConversationId("sending-parts"));
			ACLMessage msg = receive(mt);
			
			if (msg != null)
			{
				try
				{
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					
					Action available = (Action) ce;
					
					//the supplier order
					supOrder = (SupplierOrder)available.getAction();
					// find customer order from that supplier order id
					order = orders.get(supOrder.getOrderID());
					// parts received for this order so add to ready to assemble list
					readyToAssemble.add(order);
					/*
					for (CustomerOrder o : readyToAssemble)
						System.out.println("readyToAssemble"+o);
					*/
					ArrayList<String> components = new ArrayList<>();
					components.add(supOrder.getSmartphone().getBattery().toString());
					components.add(supOrder.getSmartphone().getRAM().toString());
					components.add(supOrder.getSmartphone().getScreen().toString());
					components.add(supOrder.getSmartphone().getStorage().toString());
					
					for (String s : components)
					{
						if (warehouse.get(s) == null)
							warehouse.put(s, supOrder.getQuantity());
						else
							warehouse.put(s, (warehouse.get(s) + supOrder.getQuantity()));
					}
					
					System.out.println("\n\t\t-----WAREHOUSE-----");
					for (HashMap.Entry<String, Integer> entry : warehouse.entrySet())
					    System.out.println("\t\t"+entry.getKey()+"\t"+entry.getValue());
					System.out.println("\t\t-------------------\n");
					
					
				}
				catch (CodecException ce) { ce.printStackTrace(); }
				catch (OntologyException oe) { oe.printStackTrace(); }
			}
			
		}
		
	}
	
	public class AssembleCustomerOrder extends Behaviour
	{
		public AssembleCustomerOrder(Agent a) {super(a);}
		private int step = 0;
		private boolean done = false;
		private int pendingPayment = 0;

		@Override
		public void action() 
		{
			switch(step) {
			case 0:
				//TODO: only assemble 50 per day
				
				ArrayList<CustomerOrder> done = new ArrayList<>();
				
				for (CustomerOrder o : readyToAssemble)
				{
					//System.out.println("2");
					ArrayList<String> components = new ArrayList<>();
					components.add(o.getSpecification().getBattery().toString());
					components.add(o.getSpecification().getRAM().toString());
					components.add(o.getSpecification().getScreen().toString());
					components.add(o.getSpecification().getStorage().toString());
					
					//Remove components from warehouse
					for (String comp : components)
					{
						warehouse.put(comp, warehouse.get(comp) - o.getQuantity());
						//System.out.println(warehouse.get(comp));
					}
					
					//Send order back to customer
					ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
					msg.setConversationId("orderSent");
					msg.addReceiver(o.getCustomer());
					
					Action finalOrder = new Action();
					finalOrder.setAction(o);
					finalOrder.setActor(o.getCustomer());
					
					try
					{
						getContentManager().fillContent(msg, finalOrder);
						send(msg);
						pendingPayment++;
						
						done.add(o);
					}
					catch (CodecException ce) { ce.printStackTrace(); } 
					catch (OntologyException oe) { oe.printStackTrace(); }
				}
				
				for (CustomerOrder o : done)
					readyToAssemble.remove(o);
				done.clear();
				
				step++;
				break;
				
			case 1:
				//System.out.println("6");
				MessageTemplate mt = MessageTemplate.and(
			            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
			            MessageTemplate.MatchConversationId("customer-payment"));
			    ACLMessage receivePayment = receive(mt);
			    
			    if (receivePayment != null)
			    {
			    	try
			    	{
			    		//System.out.println("7");
			    		ContentElement ce = null;
			    		ce = getContentManager().extractContent(receivePayment);
			    		
			    		SendPayment payment = (SendPayment) ce;
			    		order = payment.getOrder();
			    		
			    		//System.out.println(order);
			    		//System.out.println("variables");
			    		//System.out.println(order.getDaysDue());
			    		//System.out.println(order.getPenalty());
			    		
			    		// penalty if order was sent late
			    		int penalty = 0;
			    		if (order.getDaysDue() < 0)
			    		{
			    			int due = Math.abs(order.getDaysDue());
			    			penalty = due * order.getPenalty();
			    		}
			    			
			    		// calculate profit for this order
			    		int profit = (order.getPrice()* order.getQuantity())
			    				- (int)order.getCost()
			    				- penalty;
			    		
			    		order.setProfit(profit);
			    		
			    		System.out.println(String.format("profit for order %s: %s ", 
			    				order.getId(),
			    				order.getProfit()));
			    		
			    		dailyProfit += order.getProfit();
			    		totalProfit += order.getProfit();
			    		
			    		pendingPayment--;
			    		
			    		orders.remove(order.getId());
			    		
			    	}
			    	catch (CodecException ce) { ce.printStackTrace(); } 
					catch (OntologyException oe) { oe.printStackTrace(); }
			    }
			    else
			    	block();
			    break;
			    
			}
			if (pendingPayment == 0)
				done = true;
		}

		@Override
		public boolean done() {
			return done;
		}
		
	}


	
	public class EndDay extends OneShotBehaviour 
	{
		@Override
		public void action() {
			// print to console
			System.out.println("dailyProfit: "+dailyProfit);
			System.out.println("totalProfit: "+totalProfit);
			
			// send done message to tickerAgent
			ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
			tick.setContent("done");
			tick.addReceiver(tickerAgent);
			myAgent.send(tick);
		}
	}


}
