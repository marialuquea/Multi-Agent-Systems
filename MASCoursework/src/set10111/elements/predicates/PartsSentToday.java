package set10111.elements.predicates;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

public class PartsSentToday implements Predicate
{
	private int parts;

	@Slot(mandatory=true)
	public int getParts() {
		return parts;
	}

	public void setParts(int parts) {
		this.parts = parts;
	}
	
	
}
