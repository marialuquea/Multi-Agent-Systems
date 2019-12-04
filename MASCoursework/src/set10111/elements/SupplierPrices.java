package set10111.elements;

import java.util.ArrayList;
import java.util.HashMap;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import set10111.elements.concepts.*;

public class SupplierPrices implements AgentAction
{
	private AID supplier;
	
	@Slot (mandatory = true)
	public AID getSupplier() {
		return supplier;
	}
	public void setSupplier(AID supplier) {
		this.supplier = supplier;
	}
	
	ComponentType ct = ComponentType.getInstance();
	
	private static final HashMap<SmartphoneComponent, Integer> supplier1 = 
			new HashMap<SmartphoneComponent, Integer>() {{
				put(new Screen("SCREEN_7"), 150);
				put(new Screen("SCREEN_5"), 100);
				put(new Storage("STORAGE_64"), 25);
				put(new Storage("STORAGE_256"), 50);
				put(new Ram("RAM_4"), 30);
				put(new Ram("RAM_8"), 60);
				put(new Battery("BATTERY_3000"), 100);
				put(new Battery("BATTERY_2000"), 70);
			}};
			
	private static final HashMap<SmartphoneComponent, Integer> supplier2 = 
			new HashMap<SmartphoneComponent, Integer>() {{
				put(new Storage("STORAGE_64"), 15);
				put(new Storage("STORAGE_256"), 40);
				put(new Ram("RAM_4"), 20);
				put(new Ram("RAM_8"), 35);
			}};
	
	public static HashMap<SmartphoneComponent, Integer> getPricesSupplier1() {
		return supplier1;
	}
	
	public static HashMap<SmartphoneComponent, Integer> getPricesSupplier2() {
		return supplier2;
	}
	
}
