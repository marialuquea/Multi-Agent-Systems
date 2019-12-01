package set10111.elements;

import jade.content.AgentAction;
import jade.core.AID;

public class SupplierOrder implements AgentAction
{
	private String battery;
	private String ram;
	private String screen;
	private String storage;
	private int batteryQuantity;
	private int ramQuantity;
	private int screenQuantity;
	private int storageQuantity;
	private AID supplier;
	private int price;
	
	public String getBattery() {
		return battery;
	}
	public void setBattery(String battery) {
		this.battery = battery;
	}
	public int getBatteryQuantity() {
		return batteryQuantity;
	}
	public void setBatteryQuantity(int batteryQuantity) {
		this.batteryQuantity = batteryQuantity;
	}
	
	
	public String getRAM() {
		return ram;
	}
	public void setRAM(String ram) {
		this.ram = ram;
	}
	public int getRamQuantity() {
		return ramQuantity;
	}
	public void setRamQuantity(int ramQuantity) {
		this.ramQuantity = ramQuantity;
	}
	
	
	public String getScreen() {
		return screen;
	}
	public void setScreen(String screen) {
		this.screen = screen;
	}
	public int getScreenQuantity() {
		return screenQuantity;
	}
	public void setScreenQuantity(int screenQuantity) {
		this.screenQuantity = screenQuantity;
	}
	
	
	public String getStorage() {
		return storage;
	}
	public void setStorage(String storage) {
		this.storage = storage;
	}
	public int getStorageQuantity() {
		return storageQuantity;
	}
	public void setStorageQuantity(int storageQuantity) {
		this.storageQuantity = storageQuantity;
	}
	
	public AID getSupplier() {
		return supplier;
	}
	public void setSupplier(AID supplier) {
		this.supplier = supplier;
	}
	
	
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	
	
	
	
	
}
