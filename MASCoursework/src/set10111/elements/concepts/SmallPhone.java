package set10111.elements.concepts;

import jade.content.onto.annotations.Slot;
import set10111.elements.Smartphone;

public class SmallPhone extends Smartphone
{
	private BatterySmall battery;
	private ScreenSmall screen;
	
	public SmallPhone() {
		this.battery = new BatterySmall();
		this.screen = new ScreenSmall();
	}
	
	@Slot(mandatory = true)
	  public BatterySmall getBatterySmall() {
	    return battery;
	  }
	  public void setBatterySmall(BatterySmall battery) {
	    this.battery = battery;
	  }
	  @Slot(mandatory = true)
	  public ScreenSmall getScreenSmall() {
	    return screen;
	  }
	  public void setScreenSmall(ScreenSmall screen) {
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
