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

public class Customer extends Agent
{
	private Codec codec = new SLCodec();
	private Ontology ontology = CommerceOntology.getInstance();
	private AID manufacturerAID;
	private AID tickerAgent;
	private Order order = new Order();
	private Smartphone smartphone = new Smartphone();
	private int orderID = 0;

	protected void setup()
	{
		//System.out.println("setup() in Customer");

		// register codec
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		//add this agent to the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("customer");
		sd.setName(getLocalName() + "-customer-agent");
		dfd.addServices(sd);
		try{
			DFService.register(this, dfd);
		}
		catch(FIPAException e){
			e.printStackTrace();
		}

		addBehaviour(new TickerWaiter(this));
		addBehaviour(new OrderConfirmed(this));
		addBehaviour(new ReceiveOrder(this));
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

	// activities to do every day
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
				//System.out.println("msg received in customer: "+msg.getContent());

				if(tickerAgent == null) 
					tickerAgent = msg.getSender();

				if(msg.getContent().equals("new day")) 
				{
					//spawn new sequential behaviour for day's activities
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					//sub-behaviours will execute in the order they are added
					dailyActivity.addSubBehaviour(new FindManufacturer(myAgent));
					dailyActivity.addSubBehaviour(new SendEnquiries(myAgent));
					//dailyActivity.addSubBehaviour(new ReceiveOrder(myAgent));
					dailyActivity.addSubBehaviour(new EndDay(myAgent));
					myAgent.addBehaviour(dailyActivity);
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

	// find manufacturer through DF agent
	private class FindManufacturer extends OneShotBehaviour
	{
		public FindManufacturer(Agent a) {
			super(a);
		}

		@Override
		public void action()
		{
			DFAgentDescription sellerTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("manufacturer");
			sellerTemplate.addServices(sd);
			try
			{
				// returns an array
				DFAgentDescription[] manufacturerAgent  = DFService.search(myAgent,sellerTemplate);
				manufacturerAID = manufacturerAgent[0].getName();
			}
			catch(FIPAException e) {
				e.printStackTrace();
			}
		}
	}

	// send offer
	public class SendEnquiries extends OneShotBehaviour 
	{
		public SendEnquiries(Agent a) { super(a); }

		@Override
		public void action() 
		{
			// Prepare the message
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.addReceiver(manufacturerAID);
			msg.setLanguage(codec.getName());
			msg.setOntology(ontology.getName());

			//Prepare the content - the order
			if (Math.random() < 0.5)
			{
				//small smartphone
				smartphone.setScreen(5);
				smartphone.setBattery(2000);
			}
			else
			{
				//phablet
				smartphone.setScreen(7);
				smartphone.setBattery(3000);
			}

			if (Math.random() < 0.5)
				smartphone.setRAM(4);
			else
				smartphone.setRAM(8);

			if (Math.random() < 0.5)
				smartphone.setStorage(64);
			else
				smartphone.setStorage(256);

			order.setCustomer(myAgent.getAID());
			order.setSpecification(smartphone);
			order.setQuantity((int)Math.floor(1 + 50 * Math.random()));
			order.setPrice((int)Math.floor(100 + 500 * Math.random()));
			order.setDaysDue((int)Math.floor(1 + 10 * Math.random()));
			order.setPenalty(order.getQuantity() + (int)Math.floor(1 + 50 * Math.random()));
			
			Action request = new Action();
			request.setAction(order);
			request.setActor(manufacturerAID); // the agent that you request to perform the action
			try
			{
				getContentManager().fillContent(msg, request); //send the wrapper object
				send(msg);
			}
			catch (CodecException ce) { ce.printStackTrace(); }
			catch (OntologyException oe) { oe.printStackTrace(); } 
		}
	}

	// confirm accepted order
	public class OrderConfirmed extends CyclicBehaviour
	{
		public OrderConfirmed(Agent a) { super(a); }

		@Override
		public void action() 
		{
			MessageTemplate mt = MessageTemplate.MatchContent("order accepted");
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				//System.out.println("order was accepted, msg received in customer");
			}
			else
				block();
		}

	}

	public class ReceiveOrder extends CyclicBehaviour
	{

		public ReceiveOrder(Agent a) { super(a); }

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
			ACLMessage msg = receive(mt);
			if(msg != null)
			{
				try
				{	
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);

					Action available = (Action) ce;
					order = (Order) available.getAction(); // this is the order received
					smartphone = order.getSpecification();


					
					//System.out.println("order received in c: \t"+msg);
					System.out.println("                  Order: "+order.getId()
						+" received by "+order.getCustomer().getLocalName());
				}
				catch (CodecException ce) { ce.printStackTrace(); }
				catch (OntologyException oe) { oe.printStackTrace(); }

			}
			else
				block();
		}
		
	}
	
	//behaviour to go on to the next day of the simulation
	private class EndDay extends OneShotBehaviour {

		public EndDay(Agent a) { super(a); }

		@Override
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(tickerAgent);
			msg.setContent("done");
			myAgent.send(msg);

			//send a message to the manufacturer that we have finished
			ACLMessage manufacturerDone = new ACLMessage(ACLMessage.INFORM);
			manufacturerDone.setContent("done");
			manufacturerDone.addReceiver(manufacturerAID);
			myAgent.send(manufacturerDone);
		}

	}


}
