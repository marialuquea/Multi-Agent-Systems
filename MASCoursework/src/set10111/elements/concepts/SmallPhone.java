package set10111.elements.concepts;

import jade.content.onto.annotations.Slot;

public class SmallPhone extends Smartphone
{
	private Battery battery;
	private Screen screen;
	
	public SmallPhone(String bat, String screen) {
		if (bat.equals("BATTERY_2000") && screen.equals("SCREEN_5")) {
			this.battery = new Battery();
			this.screen = new Screen();
		}
	}
	
	@Slot(mandatory = true)
	public Battery getBattery() {
	    return battery;
	}
	public void setBattery(Battery battery) {
	    this.battery = battery;
	}
	@Slot(mandatory = true)
	public Screen getScreen() {
	    return screen;
	}
	public void setScreen(Screen screen) {
	    this.screen = screen;
	}
	  
	@Override
	public String toString() {
	  return super.toString() + 
			  String.format("\n\t"
		        + "screen: %s, \n\t"
		        + "battery: %s, \n\t)",
		        screen, battery);
	}
}
