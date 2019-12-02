package set10111.agents;

import java.util.ArrayList;
import java.util.HashMap;
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
	private int numOrdersReceived = 0;
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
					numOrdersReceived = 0;
					partsComingToday = 0;
					phoneAssembledCount = 0;
					day++;
					System.out.println("Day "+ day);
					//TODO: fix this
					/*
					for (CustomerOrder o : orders) 
					{
						o.setDaysDue(o.getDaysDue() - 1);
						
						if (o.getDaysDue() <= 0) 
						{
							o.setProfit(o.getProfit()-o.getPenalty());
							System.out.println("- "+(o.getProfit()-o.getPenalty()));
						}
						
						System.out.println("order "+o.getId()
							+ ", daysDue: "+o.getDaysDue()
							+ ", penalty: "+o.getPenalty()
							+ ", profit: "+o.getProfit());
					}
					*/
					
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					dailyActivity.addSubBehaviour(new FindCustomers(myAgent));
					dailyActivity.addSubBehaviour(new FindSuppliers(myAgent));
					dailyActivity.addSubBehaviour(new AskSupInfo(myAgent));
					dailyActivity.addSubBehaviour(new ReceivePartsPrices(myAgent));
					dailyActivity.addSubBehaviour(new ReceiveOrderQuery(myAgent));
					dailyActivity.addSubBehaviour(new ReceiveOrderRequests(myAgent));
					dailyActivity.addSubBehaviour(new OrderPartsFromSupplier(myAgent));
					myAgent.addBehaviour(dailyActivity);

					ArrayList<Behaviour> cyclicBehaviours = new ArrayList<>();
					//CyclicBehaviour ror = new ReceiveOrderRequests(myAgent);
					//myAgent.addBehaviour(ror);
					//cyclicBehaviours.add(ror);
					myAgent.addBehaviour(new EndDayListener(myAgent,cyclicBehaviours));

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
			
			// WE NEED THIS WHEN REQUESTING AN ACTION
			Action request = new Action();
			request.setAction(supOrder);
			request.setActor(order.getSupplier());
			
			System.out.println("supplier order \t"+supOrder);
			
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
	
	private class Before extends Behaviour // not in ticker
	{
		private int step = 0;
		

		@Override
		public void action() 
		{
			switch (step)
			{
			case 0:
			case 1:
				//order PARTS from supplier everyday
				if (step == 1)
				{
					//System.out.println("All orders received, now ordering parts");

					ACLMessage msgParts = new ACLMessage(ACLMessage.REQUEST);
					msgParts.addReceiver(suppliers.get(0));
					msgParts.setLanguage(codec.getName());
					msgParts.setOntology(ontology.getName());
					/*
					SupplierOrder o = new SupplierOrder();
					o.setBattery(2000);
					o.setRAM(4);
					o.setScreen(5);
					o.setStorage(64);
					// quantity
					o.setBatteryQuantity(50);
					o.setRamQuantity(50);
					o.setScreenQuantity(50);
					o.setStorageQuantity(50);
					// supplier
					o.setSupplier(1);

					Action request = new Action();
					request.setAction(o);
					request.setActor(suppliers.get(0));
					try
					{
						getContentManager().fillContent(msgParts, request); //send the wrapper object
						send(msgParts);
					}
					catch (CodecException ce) { ce.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); } 



					// other parts
					o.setBattery(3000);
					o.setRAM(8);
					o.setScreen(7);
					o.setStorage(256);
					// quantity
					o.setBatteryQuantity(50);
					o.setRamQuantity(50);
					o.setScreenQuantity(50);
					o.setStorageQuantity(50);
					// supplier
					o.setSupplier(1);

					request.setAction(o);
					request.setActor(suppliers.get(0));
					try
					{
						getContentManager().fillContent(msgParts, request); //send the wrapper object
						send(msgParts);
						//System.out.println("msgParts: "+msgParts);
					}
					catch (CodecException ce) { ce.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); } 
					
					*/

					//System.out.println("step: "+step);
					step = 2;
				}




			case 2:
				// receive information about how many parts the supplier is going to send today
				if (step == 2)
				{
					int messages = 0;
					while (messages <= 1) 
					{
						//System.out.println("messages: "+messages);
						MessageTemplate info = MessageTemplate.MatchConversationId("parts");
						ACLMessage infomsg = myAgent.receive(info);
						if(infomsg != null) 
						{
							partsComingToday = Integer.parseInt(infomsg.getContent(), 10);
							//System.out.println(partsComingToday + " order parts from supplier coming in today");
							messages++;
							break;
						}
						else 
							block();
					}
					step = 3;
				}





			case 3:
				//receive parts from supplier
				if (step == 3)
				{
					/*
					System.out.println("Receiving parts from suppliers");
					int partsPerDay = 0;
					do
					{
						MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
						ACLMessage msg2 = receive(mt2);
						if(msg2 != null)
						{
							//System.out.println("msg received: "+msg2);
							try
							{	
								ContentElement ce = null;
								ce = getContentManager().extractContent(msg2);

								Action available = (Action) ce;
								supOrder = (SupplierOrder) available.getAction(); // this is the order requested

								partsPerDay++;

								// Place parts in warehouse

								//SCREENS
								if (supOrder.getScreen() == 5) {
									if (warehouse.get("screen5") == null)
										warehouse.put("screen5", supOrder.getScreenQuantity());
									else {
										int before = warehouse.get("screen5");
										warehouse.remove("screen5");
										warehouse.put("screen5", before+supOrder.getScreenQuantity()); 
									}
								}
								else if (supOrder.getScreen() == 7) {
									if (warehouse.get("screen7") == null)
										warehouse.put("screen7", supOrder.getScreenQuantity());
									else {
										int before = warehouse.get("screen7");
										warehouse.remove("screen7");
										warehouse.put("screen7", before+supOrder.getScreenQuantity()); 
									}
								}

								// STORAGE
								if (supOrder.getStorage() == 64) {
									if (warehouse.get("storage64") == null)
										warehouse.put("storage64", supOrder.getStorageQuantity());
									else {
										int before = warehouse.get("storage64");
										warehouse.remove("storage64");
										warehouse.put("storage64", before+supOrder.getStorageQuantity()); 
									}
								}
								if (supOrder.getStorage() == 256) {
									if (warehouse.get("storage256") == null )
										warehouse.put("storage256", supOrder.getStorageQuantity());
									else {
										int before = warehouse.get("storage256");
										warehouse.remove("storage256");
										warehouse.put("storage256", before+supOrder.getStorageQuantity());
									}
								}

								// RAM
								if (supOrder.getRAM() == 4) {
									if (warehouse.get("ram4") == null )
										warehouse.put("ram4", supOrder.getRamQuantity());
									else {
										int before = warehouse.get("ram4");
										warehouse.remove("ram4");
										warehouse.put("ram4", before+supOrder.getRamQuantity());
									}
								}
								if (supOrder.getRAM() == 8) {
									if (warehouse.get("ram8") == null )
										warehouse.put("ram8", supOrder.getRamQuantity());
									else {
										int before = warehouse.get("ram8");
										warehouse.remove("ram8");
										warehouse.put("ram8", before+supOrder.getRamQuantity());
									}
								}


								// BATTERY
								if (supOrder.getBattery() == 2000) {
									if (warehouse.get("battery2000") == null )
										warehouse.put("battery2000", supOrder.getBatteryQuantity());
									else {
										int before = warehouse.get("battery2000");
										warehouse.remove("battery2000");
										warehouse.put("battery2000", before+supOrder.getBatteryQuantity());
									}	
								}
								if (supOrder.getBattery() == 3000) {
									if (warehouse.get("battery3000") == null )
										warehouse.put("battery3000", supOrder.getBatteryQuantity());
									else {
										int before = warehouse.get("battery3000");
										warehouse.remove("battery3000");
										warehouse.put("battery3000", before+supOrder.getBatteryQuantity());
									}	
								}
								//System.out.println("all parts received from supplier and stored in warehouse");
							}
							catch (CodecException ce) { ce.printStackTrace(); }
							catch (OntologyException oe) { oe.printStackTrace(); }
							//System.out.println("parts count: "+partsPerDay);
						}
						else
							block();
					}
					while (partsPerDay < partsComingToday);
					//System.out.println("batteries received count: "+partsTotal);

					// YEA BOI IT WORKS
					System.out.println("        ------WAREHOUSE-----");
					for (String i : warehouse.keySet()) {
						System.out.println("        "+i + " \t " + warehouse.get(i));
					}
					System.out.println("        --------------------");
					
					*/
					
					step = 4;
				}


			case 4:
				//TODO: assemble phone!! and sell
				if (step == 4 && day >= 2)
				{
					
					
					int orderCount = 0;
					
					for (int i = orders.size() - 1; i >= 0; i--)
					{
						ArrayList<String> specification = new ArrayList<>();
						specification.clear();
						
						phone = orders.get(i).getSpecification();
						
						
						int count = 0;
						/*
						if ((phone.getBattery() == 2000) && (warehouse.get("battery2000") > orders.get(i).getQuantity())) {
							count++; specification.add("battery2000"); }
						else if ((phone.getBattery() == 3000) && (warehouse.get("battery3000") > orders.get(i).getQuantity())) {
							count++; specification.add("battery3000"); }
						if ((phone.getRAM() == 4) && (warehouse.get("ram4") > orders.get(i).getQuantity())) {
							count++; specification.add("ram4"); }
						else if ((phone.getRAM() == 8) && (warehouse.get("ram8") > orders.get(i).getQuantity())) {
							count++; specification.add("ram8"); }
						if ((phone.getScreen() == 5) && (warehouse.get("screen5") > orders.get(i).getQuantity())) {
							count++; specification.add("screen5"); }
						else if ((phone.getScreen() == 7) && (warehouse.get("screen7") > orders.get(i).getQuantity())) {
							count++; specification.add("screen7"); }
						if ((phone.getStorage() == 64) && (warehouse.get("storage64") > orders.get(i).getQuantity())) {
							count++; specification.add("storage64"); }
						if ((phone.getStorage() == 256) && (warehouse.get("storage256") > orders.get(i).getQuantity())) {
							count++; specification.add("storage256"); }
						//System.out.println("count: "+count);

						*/
						// assemble and remove from order list
						if ((count == 4) && (phoneAssembledCount + orders.get(i).getQuantity() <= 50))
						{
							//System.out.println("order quantity: "+orders.get(i).getQuantity());
							//for (String s : specification)
							//	System.out.println("specification: "+s);
							
							System.out.println("Assemblying order "+orders.get(i).getId());
							
							// send phones to customer
							ACLMessage msgA = new ACLMessage(ACLMessage.AGREE);
							msgA.addReceiver(orders.get(i).getCustomer());
							msgA.setLanguage(codec.getName());
							msgA.setOntology(ontology.getName());
							
							Action request = new Action();
							request.setAction(orders.get(i));
							request.setActor(orders.get(i).getCustomer());
							try
							{
								getContentManager().fillContent(msgA, request); //send the wrapper object
								send(msgA);
								System.out.println("        SENT "+orders.get(i).getId()
										+" - "+orders.get(i).getCustomer().getLocalName());
							}
							catch (CodecException ce) { ce.printStackTrace(); }
							catch (OntologyException oe) { oe.printStackTrace(); } 
							
							
							
							// remove parts from warehouse
							for (String s : specification)
							{
								int before = warehouse.get(s);
								warehouse.remove(s);
								warehouse.put(s, before-orders.get(i).getQuantity());
							}
							phoneAssembledCount += orders.get(i).getQuantity();
							
							
							//remove order from order list
							orderCount++;
							orders.remove(i);
						}

					}
					
					System.out.println(phoneAssembledCount+" phones assembled today");
					System.out.println("orders assembled today: "+orderCount);
					System.out.println("orders left to assemble: "+orders.size());	
				}
				break;
			}

		}

		@Override
		public boolean done() {
			return false;
		}

	}

	
	public class EndDayListener extends CyclicBehaviour 
	{
		private int customersFinished = 0;
		private List<Behaviour> toRemove;

		public EndDayListener(Agent a, List<Behaviour> toRemove) 
		{
			super(a);
			this.toRemove = toRemove;
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchContent("done");
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				customersFinished++;
			}
			else {
				block();
			}
			if(customersFinished == customers.size()) {
				//we are finished
				// System.out.println("orders received from customers: "+numOrdersReceived);

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


}
