package set10111.elements;

import jade.content.AgentAction;
import jade.core.AID;

public class Sell implements AgentAction 
{
	private AID buyer;
	private Smartphone smartphone;
	
	public AID getBuyer() {
		return buyer;
	}
	
	public void setBuyer(AID buyer) {
		this.buyer = buyer;
	}
	
	public Smartphone getSmartphone() {
		return smartphone;
	}
	
	public void setSmartphone(Smartphone smartphone) {
		this.smartphone = smartphone;
	}	
	
}