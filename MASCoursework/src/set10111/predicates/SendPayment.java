package set10111.predicates;

import jade.content.Predicate;
import jade.core.AID;
import set10111.elements.CustomerOrder;
import set10111.elements.SupplierOrder;

public class SendPayment implements Predicate
{
	private AID agent;
	private CustomerOrder order;
	private SupplierOrder supOrder;
	
	public AID getAgent() {
		return agent;
	}
	public void setAgent(AID customer) {
		this.agent = customer;
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
