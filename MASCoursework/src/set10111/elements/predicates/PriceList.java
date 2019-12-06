package set10111.elements.predicates;

import java.util.ArrayList;

import jade.content.Predicate;
import jade.core.AID;
import set10111.elements.concepts.SmartphoneComponent;

public class PriceList implements Predicate
{
	private AID supplier;
	private int speed;
	private ArrayList<SmartphoneComponent> component;
	private ArrayList<Integer> prices;
	
	public AID getSupplier() {
		return supplier;
	}
	public void setSupplier(AID supplier) {
		this.supplier = supplier;
	}
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	public ArrayList<SmartphoneComponent> getComponents() {
		return component;
	}
	public void setComponents(ArrayList<SmartphoneComponent> components) {
		this.component = components;
	}
	public ArrayList<Integer> getPrices() {
		return prices;
	}
	public void setPrices(ArrayList<Integer> prices) {
		this.prices = prices;
	}
	

}
