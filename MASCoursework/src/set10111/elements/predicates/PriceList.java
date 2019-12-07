package set10111.elements.predicates;

import java.util.ArrayList;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import set10111.elements.concepts.SmartphoneComponent;

public class PriceList implements Predicate
{
	private AID supplier;
	private int speed;
	private ArrayList<SmartphoneComponent> component;
	private ArrayList<Double> prices;
	
	@Slot(mandatory=true)
	public AID getSupplier() {
		return supplier;
	}
	public void setSupplier(AID supplier) {
		this.supplier = supplier;
	}
	@Slot(mandatory=true)
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	@Slot(mandatory=true)
	public ArrayList<SmartphoneComponent> getComponents() {
		return component;
	}
	public void setComponents(ArrayList<SmartphoneComponent> components) {
		this.component = components;
	}
	@Slot(mandatory=true)
	public ArrayList<Double> getPrices() {
		return prices;
	}
	public void setPrices(ArrayList<Double> prices) {
		this.prices = prices;
	}
	

}
