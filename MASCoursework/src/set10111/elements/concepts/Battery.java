package set10111.elements.concepts;

import jade.content.onto.annotations.Slot;

public class Battery extends SmartphoneComponent
{
private String specification;
	
	public Battery() {}
	
	public Battery(String specification) { 
		this.specification = specification;
	}
	
	@Slot(mandatory=true)
	public String getSpecification() {
		return specification;
	}
	public void setSpecification(String specification) {
		this.specification = specification;
	}
	
	@Override
	public String toString() {
		return "Battery: " + this.specification;
	}
	
}
