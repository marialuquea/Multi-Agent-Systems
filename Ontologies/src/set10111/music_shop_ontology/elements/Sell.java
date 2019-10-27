package set10111.music_shop_ontology.elements;

import jade.content.AgentAction;
import jade.core.AID;

public class Sell implements AgentAction {
	private AID buyer;
	private Item item;
	
	public AID getBuyer() {
		return buyer;
	}
	
	public void setBuyer(AID buyer) {
		this.buyer = buyer;
	}
	
	public Item getItem() {
		return item;
	}
	
	public void setItem(Item item) {
		this.item = item;
	}	
	
}
