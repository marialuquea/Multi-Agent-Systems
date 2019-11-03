package set10111.agents;

import java.util.ArrayList;
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
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
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
	Order order = new Order();
	Smartphone smartphone = new Smartphone();

	protected void setup()
	{
		System.out.println("setup() in Manufacturer");
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
				if(tickerAgent == null) 
					tickerAgent = msg.getSender();

				if(msg.getContent().equals("new day")) 
				{
					customers.clear();
					numOrdersReceived = 0;
					
					// find customers and suppliers
					myAgent.addBehaviour(new FindCustomers(myAgent));
					myAgent.addBehaviour(new FindSuppliers(myAgent));
					
					ArrayList<Behaviour> cyclicBehaviours = new ArrayList<>();
					
					//receive order requests
					CyclicBehaviour ror = new ReceiveOrderRequests(myAgent);
					myAgent.addBehaviour(ror);
					cyclicBehaviours.add(ror);

					myAgent.addBehaviour(new EndDayListener(myAgent,cyclicBehaviours));
					
				}
				else {
					//termination message to end simulation
					myAgent.doDelete();
				}
			}
			else{
				block();
			}
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
				System.out.println("suppliers size "+suppliers.size());
			}
			catch(FIPAException e) { e.printStackTrace(); }
		}
	}

	// behaviour to receive customer requests
	private class ReceiveOrderRequests extends CyclicBehaviour
	{
		private int step = 0;
		
		public ReceiveOrderRequests(Agent a) {
			super(a);
		}

		@Override
		public void action() 
		{
			switch (step)
			{
			case 0:
				// respond to REQUEST messages
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

						System.out.println("order received from "+order.getCustomer().getLocalName()+": "+order.getQuantity()+" units");

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
					System.out.println("All orders received, now ordering parts");
					
					//TODO: order parts from supplier
					
					
					
				}
				
			case 2:
				
				
				
			case 3:
				break;
			}
			
		}

	}
	
	public class EndDayListener extends CyclicBehaviour {
		private int customersFinished = 0;
		private List<Behaviour> toRemove;

		public EndDayListener(Agent a, List<Behaviour> toRemove) {
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
