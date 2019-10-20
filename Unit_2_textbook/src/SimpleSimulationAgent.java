import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/*
 * A SIMPLE SIMULATION AGENT THAT SYNCHRONISES ITS DAILY 
 * ACTIVITIES WITH THE NewDay_TickerAgent
 */
public class SimpleSimulationAgent extends Agent
{
	private int day = 0;
	private AID tickerAgent;
	
	@Override
	protected void setup()
	{
		//add this agent to the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("simulation-agent");
		sd.setName(getLocalName() + "-simulation-agent");
		dfd.addServices(sd);
		try
		{
			DFService.register(this, dfd);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}
		addBehaviour(new DailyBehaviour());
	} // End of setup()
	
	@Override
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
	} // ENd of takeDown()
	
	
	public class DailyBehaviour extends CyclicBehaviour
	{
		@Override
		public void action()
		{
			//wait for a new day message
			MessageTemplate mt = MessageTemplate.MatchContent("new day");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null)
			{
				if (tickerAgent == null)
				{
					tickerAgent = msg.getSender(); //AID of the Ticker Agent
				}
				//do computation here
				day++;
				System.out.println(getLocalName() + "day: " + day);
				addBehaviour(new WakerBehaviour(myAgent,5000) 
				{
					protected void onWake()
					{
						//send a "done" message
						ACLMessage dayDone = new ACLMessage(ACLMessage.INFORM);
						dayDone.addReceiver(tickerAgent);
						dayDone.setContent("done");
						myAgent.send(dayDone);
					}
				});
			}
			else
			{
				block(); //suspend this behaviour until we receive a message
			}
		}
	}
}
