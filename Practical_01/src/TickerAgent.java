import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;

public class TickerAgent extends Agent{
	// Get the current time - this will be the time that the agent was launched at
	long t0 = System.currentTimeMillis();
	
	Behaviour loop;
	protected void setup(){
		loop = new TickerBehaviour(this, 1000) {
			protected void onTick() {
				
				if (System.currentTimeMillis()-t0 < 10000) {
					System.out.println((System.currentTimeMillis()-t0)/1000 + " seconds");
				} else {
					System.out.println("Deleting agent...");
					myAgent.doDelete(); // Delete this agent
				}
			}
		};
	addBehaviour(loop);
	}
}
