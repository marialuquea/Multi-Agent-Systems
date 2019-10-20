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
	} // End of setuo()
	
	
	protected void takeDown()
	{
		//Deregister from the yellow pages
		try
		{
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
			if (msg != null)
			{
				//process the message
				System.out.println("I am " + myAgent.getLocalName());
				System.out.println("Message received from " + msg.getSender());
				System.out.println("The message is: ");
				System.out.println(msg.getContent());
				System.out.println();
			}
			else
			{
				//put the behaviour to sleep until a msg arrives
				block();
			}
		}
	}
}











