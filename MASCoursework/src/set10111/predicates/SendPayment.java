package set10111.predicates;

import jade.content.Predicate;
import jade.core.AID;
import set10111.elements.CustomerOrder;
import set10111.elements.SupplierOrder;

public class SendPayment implements Predicate
{
	private AID customer;
	private CustomerOrder order;
	private SupplierOrder supOrder;
	
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
	public SupplierOrder getSupOrder() {
		return supOrder;
	}
	public void setSupOrder(SupplierOrder supOrder) {
		this.supOrder = supOrder;
	}
	
	
}
