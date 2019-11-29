package set10111.elements.concepts;

import jade.content.onto.annotations.Slot;
import napier.ac.uk_ontology.computerComponents.CpuDesktop;
import napier.ac.uk_ontology.computerComponents.MotherboardDesktop;
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
	  public BatteryPhablet getBat() {
	    return battery;
	  }
	  public void setBat(BatteryPhablet battery) {
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
		        + "cpu: %s, \n\t"
		        + "motherboard: %s, \n\t)",
		        cpu, motherboard);
	  }
}
