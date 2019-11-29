package set10111.elements;


import jade.content.Predicate;
import jade.core.AID;

public class Owns implements Predicate 
{
	private AID supplier;
	private Smartphone smartphone;
	
	public AID getOwner() {
		return supplier;
	}
	
	public void setOwner(AID owner) {
		this.supplier = owner;
	}
	
	public Smartphone getSmartphone() {
		return smartphone;
	}
	
	public void setSmartphone(Smartphone smartphone) {
		this.smartphone = smartphone;
	}
	
}