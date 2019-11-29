package set10111.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import set10111.ontology.CommerceOntology;
import set10111.elements.*;

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
	private ArrayList<CustomerOrder> orders = new ArrayList<>();
	private HashMap<String, Integer> warehouse = new HashMap<>();
	private int partsComingToday = 0;
	private int day;
	private int phoneAssembledCount = 0;
	private int orderID = 0;

	protected void setup()
	{
		//System.out.println("setup() in Manufacturer");
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
					// find customers and suppliers
					myAgent.addBehaviour(new FindCustomers(myAgent));
					myAgent.addBehaviour(new FindSuppliers(myAgent));

					ArrayList<Behaviour> cyclicBehaviours = new ArrayList<>();

					CyclicBehaviour ror = new ReceiveOrderRequests(myAgent);
					myAgent.addBehaviour(ror);
					cyclicBehaviours.add(ror);

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

	// behaviour to receive customer requests
	private class ReceiveOrderRequests extends CyclicBehaviour
	{
		private int step = 0;
		public ReceiveOrderRequests(Agent a) { super(a); }

		@Override
		public void action() 
		{
			switch (step)
			{
			case 0:
				// receive and respond to order REQUEST messages from customers
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
				ACLMessage msg = receive(mt);
				if(msg != null)
				{
					try
					{	
						ContentElement ce = null;

						// Let JADE convert from String to Java objects
						// Output will be a ContentElement
						ce = getContentManager().extractContent(msg);

						Action available = (Action) ce;
						order = (CustomerOrder) available.getAction(); // this is the order requested
						phone = order.getSpecification();

						order.setId(++orderID);
						System.out.println("Order request: \t "+order.getId());
						
						
						
						//System.out.println("orderID: "+order.getId());
						//System.out.println("orders.size(): "+orders.size());

						// calculate cost of making offer from supplier 1
						int cost = 0;

						if (phone.getScreen() == 5)
							cost += 100;
						if (phone.getScreen() == 7)
							cost += 150;

						if (phone.getBattery() == 2000)
							cost += 70;
						if (phone.getBattery() == 3000)
							cost += 100;

						if (phone.getStorage() == 64)
							cost += 25;
						if (phone.getStorage() == 256)
							cost += 50;

						if (phone.getRAM() == 4)
							cost += 30;
						if (phone.getStorage() == 64)
							cost += 25;
						

						// how much it is going to sell for 
						int sold = order.getPrice() * order.getQuantity();
						order.setProfit(sold - cost);
						System.out.println("sold: "+sold+" - cost: "+cost);
						//System.out.println("profit: "+order.getProfit());
						//System.out.println("penalty costs: "+order.getPenalty());

						//TODO: if sold > x then sell, else refuse
						
						// if late orders, no
						
						// check penalty costs
						
						// if profit >>> accept
						
						// order accepted and added to list of orders
						orders.add(order);
						
						// send accept proposal message to customer
						ACLMessage reply = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						reply.setContent("order accepted");
						reply.addReceiver(order.getCustomer());
						myAgent.send(reply);

						numOrdersReceived++;

						//System.out.println(numOrdersReceived+" - "+customers.size());
						if (numOrdersReceived >= customers.size())
							step = 1;
					}
					catch (CodecException ce) { ce.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); }

				}
				else
					block();


			case 1:
				//order PARTS from supplier everyday
				if (step == 1)
				{
					//System.out.println("All orders received, now ordering parts");

					ACLMessage msgParts = new ACLMessage(ACLMessage.REQUEST);
					msgParts.addReceiver(suppliers.get(0));
					msgParts.setLanguage(codec.getName());
					msgParts.setOntology(ontology.getName());

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
