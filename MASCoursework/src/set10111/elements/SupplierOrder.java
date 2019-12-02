package set10111.elements;

import jade.content.AgentAction;
import jade.core.AID;

public class SupplierOrder implements AgentAction
{
	private Smartphone smartphone;
	private int quantity;
	private AID supplier;
	private int orderID;
	
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
				+ "\t quantity: %s, \n\t"
		        + "\t supplier: %s )",
		        smartphone, quantity, supplier);
	}
	
	
}
