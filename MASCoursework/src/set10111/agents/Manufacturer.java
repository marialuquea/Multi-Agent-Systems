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
	private Order order = new Order();
	private Smartphone smartphone = new Smartphone();
	private ArrayList<Order> orders = new ArrayList<>();
	private HashMap<String, Integer> warehouse = new HashMap<>();
	private int partsTotal = 0;
	private int partsComingToday = 0;

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
					orders.clear();
					partsComingToday = 0;


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
				// respond to REQUEST messages from customers
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
						order = (Order) available.getAction(); // this is the order requested
						smartphone = order.getSpecification();

						orders.add(order);

						System.out.println("orders received _m: "+orders.size());

						// calculate cost of making offer from supplier 1
						int total = 0;

						if (smartphone.getScreen() == 5)
							total += 100;
						if (smartphone.getScreen() == 7)
							total += 150;

						if (smartphone.getBattery() == 2000)
							total += 70;
						if (smartphone.getBattery() == 3000)
							total += 100;

						if (smartphone.getStorage() == 64)
							total += 25;
						if (smartphone.getStorage() == 256)
							total += 50;

						if (smartphone.getRAM() == 4)
							total += 30;
						if (smartphone.getStorage() == 64)
							total += 25;

						// how much it is going to sell for 
						int sold = order.getPrice() * order.getQuantity();


						//TODO: if sold > x then sell, else refuse

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
				if (step == 1)
				{
					//System.out.println("All orders received, now ordering parts");

					//order PARTS from supplier everyday
					ACLMessage msgParts = new ACLMessage(ACLMessage.REQUEST);
					msgParts.addReceiver(suppliers.get(0));
					msgParts.setLanguage(codec.getName());
					msgParts.setOntology(ontology.getName());
					
					SupplierOrder o = new SupplierOrder();
					o.setBattery(2000, 50);
					o.setRAM(4, 50);
					o.setScreen(5, 50);
					o.setStorage(64, 50);
					o.setSupplier(1);
					
					Action request = new Action();
					request.setAction(order);
					request.setActor(suppliers.get(0));
					try
					{
						getContentManager().fillContent(msgParts, request); //send the wrapper object
						send(msgParts);
					}
					catch (CodecException ce) { ce.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); } 
					
					/*
					//order parts from supplier
					for (Order order : orders)
					{
						ACLMessage msgParts1 = new ACLMessage(ACLMessage.REQUEST);
						msgParts1.addReceiver(suppliers.get(0));
						msgParts1.setLanguage(codec.getName());
						msgParts1.setOntology(ontology.getName());

						System.out.println("battery ordered from manufacturer: "+order.getSpecification().getBattery());
						
						//Smartphone specification
						Smartphone orderParts = new Smartphone();
						orderParts.setBattery(order.getSpecification().getBattery());
						orderParts.setRAM(order.getSpecification().getRAM());
						orderParts.setScreen(order.getSpecification().getScreen());
						orderParts.setStorage(order.getSpecification().getStorage());

						//Order specification
						Order orderPartsO = new Order();
						orderPartsO.setSpecification(orderParts);
						orderPartsO.setCustomer(order.getCustomer());
						orderPartsO.setQuantity(order.getQuantity());

						Action request = new Action();
						request.setAction(order);
						request.setActor(suppliers.get(0));
						try
						{
							getContentManager().fillContent(msgParts, request); //send the wrapper object
							send(msgParts);
						}
						catch (CodecException ce) { ce.printStackTrace(); }
						catch (OntologyException oe) { oe.printStackTrace(); } 
					}
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
							System.out.println(partsComingToday + " order parts from supplier coming in today");
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
								order = (Order) available.getAction(); // this is the order requested
								smartphone = order.getSpecification();
								int q = order.getQuantity();
								//System.out.println("q: "+q);

								partsTotal++;

								partsPerDay++;

								// Place parts in warehouse
								//System.out.println(smartphone.getScreen());

								//SCREENS
								if (smartphone.getScreen() == 5) {
									if (warehouse.get("screen5") == null )
										warehouse.put("screen5", q);
									else {
										int before = warehouse.get("screen5");
										warehouse.remove("screen5");
										warehouse.put("screen5", before+q); 
									}
								}
								else if (smartphone.getScreen() == 7) {
									if (warehouse.get("screen7") == null )
										warehouse.put("screen7", q);
									else {
										int before = warehouse.get("screen7");
										warehouse.remove("screen7");
										warehouse.put("screen7", before+q); 
									}
								}

								// STORAGE
								if (smartphone.getStorage() == 64) {
									if (warehouse.get("storage64") == null )
										warehouse.put("storage64", q);
									else {
										int before = warehouse.get("storage64");
										warehouse.remove("storage64");
										warehouse.put("storage64", before+q); 
									}
								}
								if (smartphone.getStorage() == 256) {
									if (warehouse.get("storage256") == null )
										warehouse.put("storage256", q);
									else {
										int before = warehouse.get("storage256");
										warehouse.remove("storage256");
										warehouse.put("storage256", before+q);
									}
								}

								// RAM
								if (smartphone.getRAM() == 4) {
									if (warehouse.get("ram4") == null )
										warehouse.put("ram4", q);
									else {
										int before = warehouse.get("ram4");
										warehouse.remove("ram4");
										warehouse.put("ram4", before+q);
									}
								}
								if (smartphone.getRAM() == 8) {
									if (warehouse.get("ram8") == null )
										warehouse.put("ram8", q);
									else {
										int before = warehouse.get("ram8");
										warehouse.remove("ram8");
										warehouse.put("ram8", before+q);
									}
								}


								// BATTERY
								if (smartphone.getBattery() == 2000) {
									if (warehouse.get("battery2000") == null )
										warehouse.put("battery2000", q);
									else {
										int before = warehouse.get("battery2000");
										warehouse.remove("battery2000");
										warehouse.put("battery2000", before+q);
									}	
								}
								if (smartphone.getBattery() == 3000) {
									if (warehouse.get("battery3000") == null )
										warehouse.put("battery3000", q);
									else {
										int before = warehouse.get("battery3000");
										warehouse.remove("battery3000");
										warehouse.put("battery3000", before+q);
									}	
								}

								// YEA BOI IT WORKS
								
								/*
								for (String i : warehouse.keySet()) {
									System.out.println(i + " - " + warehouse.get(i));
								}
								*/

								System.out.println("all parts received from supplier and stored in warehouse");
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
				}


			case 4:
				//TODO: assemble phone!! and sell
				if (step == 4)
				{
					
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
