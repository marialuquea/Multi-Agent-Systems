package set10111.elements;


import jade.content.Predicate;
import jade.core.AID;

public class Owns implements Predicate 
{
	private AID owner;
	private Smartphone smartphone;
	
	public AID getOwner() {
		return owner;
	}
	
	public void setOwner(AID owner) {
		this.owner = owner;
	}
	
	public Smartphone getSmartphone() {
		return smartphone;
	}
	
	public void setSmartphone(Smartphone smartphone) {
		this.smartphone = smartphone;
	}
	
}
