package set10111.elements;

import jade.content.AgentAction;
import jade.core.AID;

public class CustomerOrder implements AgentAction
{
	private int id;
	private AID customer; 
	private AID supplier; // the supplier assigned
	private int price; // price set by the customer
	private int penalty; // per-day penalty for late delivery
	private Smartphone smartphone; // smartphone specification
	private int quantity; // how many smartphones ordered
	private int daysDue;
	private boolean accepted;
	private double customerPrice;
	private double cost;

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public AID getCustomer() {
		return customer;
	}
	public void setCustomer(AID seller) {
		this.customer = seller;
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	public int getPenalty() {
		return penalty;
	}
	public void setPenalty(int penalty) {
		this.penalty = penalty;
	}
	public Smartphone getSpecification() {
		return smartphone;
	}
	public void setSpecification(Smartphone specification) {
		this.smartphone = specification;
	}
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	public int getDaysDue() {
		return daysDue;
	}
	public void setDaysDue(int daysDue) {
		this.daysDue = daysDue;
	}
	public boolean isAccepted() {
		return accepted;
	}
	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}
	public double getCustomerPrice() {
		return customerPrice;
	}
	public void setCustomerPrice(double highest) {
		this.customerPrice = highest;
	}
	public AID getSupplier() {
		return supplier;
	}
	public void setSupplier(AID supplier) {
		this.supplier = supplier;
	}
	public double getCost() {
		return cost;
	}
	public void setCost(double bestSupplierCost) {
		this.cost = bestSupplierCost;
	}
	
	@Override
	public String toString() {
		return String.format("(\n\t"
		        + "id: %s, \n\t"
		        + "customer: %s, \n\t"
		        + "supplier: %s, \n\t"
		        + "price: %s, \n\t"
		        + "penalty: %s, \n\t"
		        + "phone: %s \n\t"
		        + "quantity: %s, \n\t"
		        + "days due: %s, \n\t"
		        + "accepted: %s, \n\t"
		        + "customerPrice: %s, \n\t"
		        + "cost: %s,",
		        id, customer, supplier, price,
				penalty, smartphone, quantity, daysDue,
				accepted, customerPrice, cost);
	}
}