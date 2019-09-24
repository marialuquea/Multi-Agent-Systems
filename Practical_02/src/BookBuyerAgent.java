import jade.core.Agent;

public class BookBuyerAgent extends Agent{
	protected void setup() {
		System.out.println("Hello! Buyer-agent "+getAID().getName()+" is ready.");
	}
}
