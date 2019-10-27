/**
 * 
 */
package set10111.music_shop_ontology.elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Track implements Concept {
	private String name;
	private int duration;
	
	@Slot(mandatory = true)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Slot(mandatory = true)
	public int getDuration() {
		return duration;
	}
	
	public void setDuration(int duration) {
		this.duration = duration;
	}
	
}
