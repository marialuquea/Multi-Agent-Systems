package set10111.predicates;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import set10111.elements.CustomerOrder;

public class OrderQuery implements Predicate
{
	private AID manufacturer;
	private CustomerOrder order;
	
	@Slot(mandatory=true)
	public AID getManufacturer() {
		return manufacturer;
	}
	public void setManufacturer(AID manufacturer) {
	    this.manufacturer = manufacturer;
	  }
	  @Slot(mandatory = true)
	  public CustomerOrder getOrder() {
	    return order;
	  }
	  public void setOrder(CustomerOrder order) {
	    this.order = order;
	  } 
}
