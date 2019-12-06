package set10111.elements.actions;

import jade.content.AgentAction;
import jade.core.AID;
import set10111.elements.concepts.Smartphone;

public class SupplierOrder implements AgentAction
{
	private Smartphone smartphone;
	private int quantity;
	private AID supplier;
	private int orderID;
	private double cost;
	
	public Smartphone getSmartphone() {
		return smartphone;
	}
	public void setSmartphone(Smartphone smartphone) {
		this.smartphone = smartphone;
	}
	
	public AID getSupplier() {
		return supplier;
	}
	public void setSupplier(AID supplier) {
		this.supplier = supplier;
	}
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	public int getOrderID() {
		return orderID;
	}
	public void setOrderID(int orderID) {
		this.orderID = orderID;
	}
	
	@Override
	public String toString() {
		return String.format("("
		        + "phone: %s \n\t"
		        + "\t orderID: %s, \n\t"
				+ "\t quantity: %s, \n\t"
				+ "\t supplier: %s, \n\t"
		        + "\t cost: %s )",
		        smartphone, orderID, quantity, supplier, cost);
	}
	public double getCost() {
		return cost;
	}
	public void setCost(double cost) {
		this.cost = cost;
	}
	
	
}
