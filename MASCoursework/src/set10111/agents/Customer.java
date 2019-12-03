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
import jade.core.behaviours.*;
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

public class Customer extends Agent
{
	private Codec codec = new SLCodec();
	private Ontology ontology = CommerceOntology.getInstance();
	private AID manufacturerAID;
	private AID tickerAgent;
	private CustomerOrder order = new CustomerOrder();
	Smartphone smartphone = new Smartphone();
	private int day = 0;

	protected void setup()
	{
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
					day++;
					//spawn new sequential behaviour for day's activities
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					//sub-behaviours will execute in the order they are added
					dailyActivity.addSubBehaviour(new FindManufacturer(myAgent));
					dailyActivity.addSubBehaviour(new SendEnquiries(myAgent));
					dailyActivity.addSubBehaviour(new OrderConfirmed(myAgent));
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

	// send offer inquiry
	public class SendEnquiries extends OneShotBehaviour 
	{
		public SendEnquiries(Agent a) { super(a); }

		@Override
		public void action() 
		{
			// Prepare the message
			ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
			msg.addReceiver(manufacturerAID);
			msg.setLanguage(codec.getName());
			msg.setOntology(ontology.getName());
			msg.setConversationId("customer-order-query");
			
			Ram ram;
			Storage storage;
			Battery battery;
			Screen screen;
			
			ComponentType ct = ComponentType.getInstance();

			//Prepare the content - the order
			if (Math.random() < 0.5) {
				battery = new Battery(ct.BATTERY_2000);
				screen = new Screen(ct.SCREEN_5);
			}
			else {
				battery = new Battery(ct.BATTERY_3000);
				screen = new Screen(ct.SCREEN_7);
			}

			if (Math.random() < 0.5)
				ram = new Ram(ct.RAM_4);
			else
				ram = new Ram(ct.RAM_8);

			if (Math.random() < 0.5)
				storage = new Storage(ct.STORAGE_64);
			else
				storage = new Storage(ct.STORAGE_256);
			
			smartphone.setRAM(ram);
			smartphone.setStorage(storage);
			smartphone.setBattery(battery);
			smartphone.setScreen(screen);
			
			//System.out.println(smartphone.toString());
			
			order.setCustomer(myAgent.getAID());
			order.setSpecification(smartphone);
			order.setQuantity((int)Math.floor(1 + 50 * Math.random()));
			order.setPrice((int)Math.floor(100 + 500 * Math.random()));
			order.setDaysDue((int)Math.floor(1 + 10 * Math.random()));
			order.setPenalty(order.getQuantity() + (int)Math.floor(1 + 50 * Math.random()));
			//orderID++;
			
			if (this.getAgent().getLocalName().equals("customer0"))
				order.setId((3*day)-2);
			if (this.getAgent().getLocalName().equals("customer1"))
				order.setId((3*day)-1);
			if (this.getAgent().getLocalName().equals("customer2"))
				order.setId(3*day);
	        
			OrderQuery orderQuery = new OrderQuery();
			orderQuery.setManufacturer(manufacturerAID);
			orderQuery.setOrder(order);
			
			try {
		        getContentManager().fillContent(msg, orderQuery);
		        send(msg);
		        //System.out.println(msg);
		        //System.out.println("order from "+this.getAgent().getLocalName()+" queried");
		    }
	       catch (CodecException ce) { ce.printStackTrace(); }
	       catch (OntologyException oe) { oe.printStackTrace(); } 
		}
	}

	// confirm accepted or not order and request actual order if accepted
	public class OrderConfirmed extends Behaviour
	{
		public OrderConfirmed(Agent a) { super(a); }
		
		private Boolean accepted = false;

		@Override
		public void action() 
		{
			MessageTemplate mt = MessageTemplate.and(
					MessageTemplate.MatchConversationId("customerOrder-answer"), 
					MessageTemplate.or(
							MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
							MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)));
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) 
			{
				//System.out.println("msg received in customer\n"+msg);
				if(msg.getPerformative() == ACLMessage.CONFIRM) 
				{
					ACLMessage orderReq = new ACLMessage(ACLMessage.REQUEST);
			        orderReq.setLanguage(codec.getName());
			        orderReq.setOntology(ontology.getName()); 
			        orderReq.setConversationId("FinalOrderRequest");
			        orderReq.addReceiver(manufacturerAID);
			        
					Action request = new Action();
					request.setAction(order);
					request.setActor(manufacturerAID); // the agent that you request to perform the action
					try
					{
						getContentManager().fillContent(orderReq, request); //send the wrapper object
						send(orderReq);
					}
					catch (CodecException ce) { ce.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); } 
				}
				else 
					System.out.println(myAgent.getLocalName() + "'s order was not accepted.");			
				accepted = true;
			}
			else
				block();
		}
		@Override
	    public boolean done() {
	      return accepted;
	    }

	}

	// DONE
	public class ReceiveOrder extends CyclicBehaviour
	{

		public ReceiveOrder(Agent a) { super(a); }

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.and(
					MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
					MessageTemplate.MatchConversationId("orderSent"));
			ACLMessage msg = receive(mt);
			if(msg != null)
			{
				try
				{	
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);

					Action available = (Action) ce;
					order = (CustomerOrder) available.getAction(); 
					smartphone = order.getSpecification();

					//System.out.println("order received in "+this.getAgent().getLocalName()+": "+order);
					
					//Send payment
					ACLMessage pay = new ACLMessage(ACLMessage.INFORM);
					pay.setLanguage(codec.getName());
					pay.setOntology(ontology.getName());
					pay.setConversationId("customer-payment");
					pay.addReceiver(manufacturerAID);
					
					SendPayment payment = new SendPayment();
					payment.setCustomer(this.getAgent().getAID());
					payment.setOrder(order);
					
					getContentManager().fillContent(pay, payment);
					send(pay);
					
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
