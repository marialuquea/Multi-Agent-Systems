import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class SenderAgent extends Agent
{
	private ArrayList<AID> receiverAgents = new ArrayList<>();
	
	@Override
	protected void setup()
	{
		//add this agent to yellow pages 
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("sender-agent");
		sd.setName(getLocalName() + "-sender-agent");
		dfd.addServices(sd);
		try
		{
			DFService.register(this, dfd);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}
		
		//add behaviour to find new receiver agents
		addBehaviour(new SearchYellowPages(this,10000));
		//add a behaviour to send a message to each receiver every 10 seconds
		addBehaviour(new SenderBehaviour(this,10000));
	}
	
	
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
	} // End of method takeDown
	
	
	public class SearchYellowPages extends TickerBehaviour
	{
		public SearchYellowPages(Agent a, long period)
		{
			super(a, period);
		}
		
		@Override
		protected void onTick()
		{
			//create a template for the agent service we are looking for
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("receiver-agent");
			template.addServices(sd);
			//query the DF agent
			try
			{
				DFAgentDescription[] result = DFService.search(myAgent, template);
				receiverAgents.clear(); //we're going to replace this
				for(int i = 0; i < result.length; i++)
				{
					receiverAgents.add(result[i].getName()); // this is the AID
				}
			}
			catch (FIPAException e)
			{
				e.printStackTrace();
			}
		}
	} // End of class SearchYellowPages
	
	
	public class SenderBehaviour extends TickerBehaviour
	{
		public SenderBehaviour(Agent a, long period)
		{
			super(a, period);
		}
		
		@Override
		protected void onTick()
		{
			//send a message to all receiver agents
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM); 
			/* to send a message, create ACL object
			 * ACL object takes in the performative (type: the effect that it performs on the beliefs 
			 * of the receiving agent) of the message
			 * The performative of this message is INFORM: msg provides info about the world
			 */
			msg.setContent("hello from agent " + myAgent.getLocalName());
			//add receivers 
			for(AID receiver : receiverAgents)
			{
				msg.addReceiver(receiver);
			}
			myAgent.send(msg);
			// a msg can be sent to one or more recipients at the same time
			// through repeated calls to addReceiver and send it via .send()
		}
	}// End of class SenderBehaviour
}









