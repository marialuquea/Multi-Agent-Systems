package set10111.elements;

import jade.content.AgentAction;
import jade.core.AID;

public class Order implements AgentAction
{
	private AID customer; 
	private int price; // price set by the customer
	private int penalty; // per-day penalty for late delivery
	private Smartphone specification; // smartphone specification
	private int quantity; // how many smartphones ordered
	private int daysDue;
	private boolean accepted;
	
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
		return specification;
	}
	public void setSpecification(Smartphone specification) {
		this.specification = specification;
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
	
}