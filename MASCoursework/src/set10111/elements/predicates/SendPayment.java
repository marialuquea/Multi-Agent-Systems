package set10111.elements.predicates;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import set10111.elements.actions.CustomerOrder;
import set10111.elements.actions.SupplierOrder;

public class SendPayment implements Predicate
{
	private AID agent;
	private CustomerOrder order;
	private SupplierOrder supOrder;
	
	@Slot(mandatory=true)
	public AID getAgent() {
		return agent;
	}
	public void setAgent(AID customer) {
		this.agent = customer;
	}
	@Slot(mandatory=true)
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
	
	@Override
	public String toString() {
		return String.format("(SendPayment:\n\t"
		        + "agent: %s, \n\t"
		        + "order ID: %s, \n\t"
		        + "supOrder ID: %s),",
		        agent, order.getId(), supOrder.getOrderID());
	}
}
