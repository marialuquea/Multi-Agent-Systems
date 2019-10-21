import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class ReceiverAgent extends Agent 
{
	@Override
	protected void setup()
	{
		System.out.println("setup() in receiver, register with DF");
		//add this agent to the yelloe pages 
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("receiver-agent");
		sd.setName(getLocalName() + "-receiver-agent");
		dfd.addServices(sd);
		try
		{
			DFService.register(this, dfd);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}
		//add the receiver behaviour
		addBehaviour(new ReceiverBehaviour(this));
	} // End of setup()
	
	
	protected void takeDown()
	{
		//Deregister from the yellow pages
		try
		{
			System.out.println("deregister receiver with DF");
			DFService.deregister(this);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}
	} // End of takeDown()
	
	
	public class ReceiverBehaviour extends CyclicBehaviour 
	{
		public ReceiverBehaviour(Agent a )
		{
			super(a);
		}
		
		@Override
		public void action()
		{
			//try to receive a message
			ACLMessage msg = myAgent.receive();
			/*When a message is sent to an agent, the receiving
			 * agent automatically adds it to its message queue
			 * (JADE takes care of this)
			 * To receive a message in a FIFO manner, we call the 
			 * .receive() method*/
			if (msg != null)
			{
				//process the message
				System.out.println("I am " + myAgent.getLocalName());
				System.out.println("Message received from " + msg.getSender().getLocalName());
				System.out.println("The message is: ");
				System.out.println(msg.getContent());
				System.out.println();
			}
			else
			{
				//put the behaviour to sleep until a msg arrives
				block();
				/*only the behaviour that block() was called within is put to sleep,
				 * the other behaviours in the agent’s behaviour queue continue 
				 * executing normally
				 * (if you do actually want to put the whole agent to sleep, 
				 * you can use the blockingReceive() method of Agent
				 * The block method optionally takes an argument, which is the maximum 
				 * number of milliseconds to remain blocked for*/
			}
		}
	}
}











