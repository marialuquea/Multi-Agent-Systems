import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class TimerAgent extends Agent {
	int w = 15;
	private ArrayList<AID> simpleAgents = new ArrayList<>();
	
	public void setup() {
		
		//Create a new TickerBehaviour to keep doing a certain action every t milliseconds.
		addBehaviour(new TickerBehaviour(this, 1000) {
			//call onTick every 1000ms
			protected void onTick() {
				//Count down
				if (w > 0) {
					System.out.println(w + " seconds left.");
					w--;
				} else {
					System.out.println("Bye, bye");
					/*Every behaviour has an instance variable myAgent that refers
					 * to the agent that is executing the behaviour.*/
					myAgent.doDelete(); // Delete this agent
				}
			}
		});
		
		// a ticker behaviour to search for new simple agents every 60 seconds
		addBehaviour(new TickerBehaviour(this, 60000) {
			protected void onTick() {
				//create a template for the agent service we are looking for
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("Simple-agent");
				template.addServices(sd);
				//query the DF agent
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					simpleAgents.clear(); //we're going to replace this
					for(int i = 0; i<result.length; i++) {
						simpleAgents.add(result[i].getName()); // this is the AID
					}
				}
				catch(FIPAException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
