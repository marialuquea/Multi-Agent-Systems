import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

public class TimerAgent extends Agent{
	int w = 15;
	
	public void setup() {
		//Create a new TickerBehaviour
		addBehaviour(new TickerBehaviour(this, 1000) {
			//call onTick every 1000ms
			protected void onTick() {
				//Count down
				if (w > 0) {
					System.out.println(w + " seconds left.");
					w--;
				} else {
					System.out.println("Bye, bye");
					myAgent.doDelete(); // Delete this agent
				}
			}
		}
		);
	}
}