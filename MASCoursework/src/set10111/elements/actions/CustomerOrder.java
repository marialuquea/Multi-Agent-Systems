package set10111.elements.actions;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import set10111.elements.concepts.Smartphone;

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
	private int assembled;

	@Slot(mandatory=true)
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	@Slot(mandatory=true)
	public AID getCustomer() {
		return customer;
	}
	public void setCustomer(AID seller) {
		this.customer = seller;
	}
	@Slot(mandatory=true)
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	@Slot(mandatory=true)
	public int getPenalty() {
		return penalty;
	}
	public void setPenalty(int penalty) {
		this.penalty = penalty;
	}
	@Slot(mandatory=true)
	public Smartphone getSpecification() {
		return smartphone;
	}
	public void setSpecification(Smartphone specification) {
		this.smartphone = specification;
	}
	@Slot(mandatory=true)
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	@Slot(mandatory=true)
	public int getDaysDue() {
		return daysDue;
	}
	public void setDaysDue(int daysDue) {
		this.daysDue = daysDue;
	}
	@Slot(mandatory=true)
	public boolean isAccepted() {
		return accepted;
	}
	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}
	@Slot(mandatory=true)
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
	@Slot(mandatory=true)
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
		        + "cost: %s, \n\t"
		        + "assembled: %s,",
		        id, customer, supplier, price,
				penalty, smartphone, quantity, daysDue,
				accepted, customerPrice, cost, assembled);
	}
	@Slot(mandatory=true)
	public int getAssembled() {
		return assembled;
	}
	public void setAssembled(int assembled) {
		this.assembled = assembled;
	}
}