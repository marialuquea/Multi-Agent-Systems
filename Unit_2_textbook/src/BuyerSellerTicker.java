import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BuyerSellerTicker extends Agent
{
	public static final int NUM_DAYS = 30;

	@Override
	protected void setup()
	{
		System.out.println("setup() in ticker, register wth DF");
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
		System.out.println("Waiting 5 seconds from TickerAgent for other agents to start");
		doWait(5000);
		addBehaviour(new SynchAgentsBehaviour(this));
	} // End of setup()

	@Override
	protected void takeDown()
	{
		//Deregister from the yellow pages
		try 
		{
			System.out.println("TickerAgent deregistering with DF");
			DFService.deregister(this);
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}
	} // End of takeDown()

	public class SynchAgentsBehaviour extends Behaviour
	{
		private int step = 0;
		private int numFinReceived = 0; //finished messages from other agents
		private int day = 0;
		private ArrayList<AID> simulationAgents = new ArrayList<>();

		public SynchAgentsBehaviour(Agent a)
		{
			super(a);
		}

		@Override
		public void action()
		{
			switch(step)
			{
				case 0:
					//find all agents using directory service
					DFAgentDescription template1 = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("buyer");
					template1.addServices(sd);
					
					DFAgentDescription template2 = new DFAgentDescription();
					ServiceDescription sd2 = new ServiceDescription();
					sd2.setType("seller");
					template2.addServices(sd2);
					
					try
					{
						//System.out.println("SimulationAgents before: " + simulationAgents);
						
						DFAgentDescription[] agentsType1 = DFService.search(myAgent, template1);
						for(int i = 0; i < agentsType1.length; i++)
							simulationAgents.add(agentsType1[i].getName()); //this is the AID
						
						DFAgentDescription[] agentsType2 = DFService.search(myAgent, template2);
						for(int i = 0; i<agentsType2.length; i++)
							simulationAgents.add(agentsType2[i].getName()); // this is the AID 
						
						System.out.println("SimulationAgents: " + simulationAgents);
					}
					catch (FIPAException e)
					{
						e.printStackTrace();
					}
					//send new day message to each agent
					ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
					tick.setContent("new day"); 
					for(AID id : simulationAgents)
						tick.addReceiver(id);
					myAgent.send(tick);
					
					//System.out.println("NewDay msg sent: "+tick);
					
					step++;
					day++;
					
					System.out.println("---------------Day: "+day);
					break;
				case 1:
					//wait to receive a "done" message from all agents
					MessageTemplate mt = MessageTemplate.MatchContent("done");
					ACLMessage msg = myAgent.receive(mt);
					if(msg != null)
					{
						//System.out.println("Done msg received by agents: "+msg.getContent());
						
						numFinReceived++;
						if (numFinReceived >= simulationAgents.size())
						{
							step++;
						}
					}
					else
					{
						block();
					}

			}
		}
		
		@Override
		public boolean done()
		{
			//System.out.println("done() in Ticker");
			return step == 2;
		}
		
		@Override 
		public void reset()
		{
			super.reset();
			step = 0;
			simulationAgents.clear();
			numFinReceived = 0;
			//System.out.println("reset() in Ticker: "+step+", "+simulationAgents+", "+numFinReceived);
		}
		
		@Override 
		public int onEnd()
		{
			// System.out.println("---------End of day---------");
			if(day == NUM_DAYS)
			{
				// send new message to terminate day
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("terminate");
				
				//System.out.println("simulationAgents at termination in ticker: "+simulationAgents);
				
				for(AID agent : simulationAgents)
				{
					msg.addReceiver(agent);
				}
				myAgent.send(msg);
				
				//System.out.println("Termination msg: "+msg);
				
				myAgent.doDelete();
			}
			else
			{
				reset();
				myAgent.addBehaviour(this);
			}
			
			return 0;
		}
	}
}

