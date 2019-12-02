package set10111.elements;

import jade.content.AgentAction;
import jade.core.AID;

public class SupplierOrder implements AgentAction
{
	private String battery;
	private String ram;
	private String screen;
	private String storage;
	private int quantity;
	private AID supplier;
	
	public String getBattery() {
		return battery;
	}
	public void setBattery(String battery) {
		this.battery = battery;
	}
	
	public String getRAM() {
		return ram;
	}
	public void setRAM(String ram) {
		this.ram = ram;
	}
	public String getScreen() {
		return screen;
	}
	public void setScreen(String screen) {
		this.screen = screen;
	}
	
	public String getStorage() {
		return storage;
	}
	public void setStorage(String storage) {
		this.storage = storage;
	}
	
	public AID getSupplier() {
		return supplier;
	}
	public void setSupplier(AID supplier) {
		this.supplier = supplier;
	}
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
