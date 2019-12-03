package set10111.predicates;

import jade.content.Predicate;
import jade.core.AID;
import set10111.elements.CustomerOrder;

public class SendPayment implements Predicate
{
	private AID customer;
	private CustomerOrder order;
	
	public AID getCustomer() {
		return customer;
	}
	public void setCustomer(AID customer) {
		this.customer = customer;
	}
	public CustomerOrder getOrder() {
		return order;
	}
	public void setOrder(CustomerOrder order) {
		this.order = order;
	}
	
	
}
