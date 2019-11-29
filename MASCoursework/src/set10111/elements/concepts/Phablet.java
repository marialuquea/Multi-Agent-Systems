package set10111.elements.concepts;

import jade.content.onto.annotations.Slot;
import set10111.elements.Smartphone;

public class Phablet extends Smartphone
{
	private BatteryPhablet battery;
	private ScreenPhablet screen;
	
	public Phablet() {
		this.battery = new BatteryPhablet();
		this.screen = new ScreenPhablet();
	}
	
	@Slot(mandatory = true)
	  public BatteryPhablet getBatteryPhablet() {
	    return battery;
	  }
	  public void setBatteryPhablet(BatteryPhablet battery) {
	    this.battery = battery;
	  }
	  @Slot(mandatory = true)
	  public ScreenPhablet getScreenPhablet() {
	    return screen;
	  }
	  public void setScreenPhablet(ScreenPhablet screen) {
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
