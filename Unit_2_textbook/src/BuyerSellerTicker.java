import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BuyerSellerTicker extends Agent
{
	public static final int NUM_DAYS = 30;
	
	@Override
	protected void setup()
	{
		//add this agent to the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("ticker-agent");
		sd.setName(getLocalName() + "-ticker-agent");
		dfd.addServices(sd);
		try
		{
			DFService.register(this, dfd);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}
		//wait for the other agents to start
		doWait(5000);
		addBehaviour(new SynchAgentsBehaviour(this));
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
	} // End of takeDown()
	
	public class SynchAgentsBehaviour extends Behaviour
	{
		
	}
}
