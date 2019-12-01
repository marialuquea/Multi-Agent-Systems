package set10111.predicates;

import java.util.ArrayList;

import jade.content.Predicate;
import jade.core.AID;
import set10111.elements.concepts.SmartphoneComponent;

public class PriceList implements Predicate
{
	private AID supplier;
	private int speed;
	private ArrayList<SmartphoneComponent> keys;
	private ArrayList<Long> values;
	
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
	public ArrayList<SmartphoneComponent> getKeys() {
		return keys;
	}
	public void setKeys(ArrayList<SmartphoneComponent> keys) {
		this.keys = keys;
	}
	public ArrayList<Long> getValues() {
		return values;
	}
	public void setValues(ArrayList<Long> values) {
		this.values = values;
	}
	

}
