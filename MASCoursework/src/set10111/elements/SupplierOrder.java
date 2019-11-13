package set10111.elements;

import jade.content.AgentAction;

public class SupplierOrder implements AgentAction
{
	private int battery;
	private int ram;
	private int screen;
	private int storage;
	private int batteryQuantity;
	private int ramQuantity;
	private int screenQuantity;
	private int storageQuantity;
	private int supplier;
	private int price;
	
	public int getBattery() {
		return battery;
	}
	public void setBattery(int battery, int batteryQuantity) {
		this.battery = battery;
		this.batteryQuantity = batteryQuantity;
	}
	public int getRAM() {
		return ram;
	}
	public void setRAM(int ram, int ramQuantity) {
		this.ram = ram;
		this.ramQuantity = ramQuantity;
	}
	public int getScreen() {
		return screen;
	}
	public void setScreen(int screen, int screenQuantity) {
		this.screen = screen;
		this.screenQuantity = screenQuantity;
	}
	public int getStorage() {
		return storage;
	}
	public void setStorage(int storage, int storageQuantity) {
		this.storage = storage;
		this.storageQuantity = storageQuantity;
	}
	public int getBatteryQuantity() {
		return batteryQuantity;
	}
	public int getRamQuantity() {
		return ramQuantity;
	}
	public int getScreenQuantity() {
		return screenQuantity;
	}
	public int getStorageQuantity() {
		return storageQuantity;
	}
	public int getSupplier() {
		return supplier;
	}
	public void setSupplier(int supplier) {
		this.supplier = supplier;
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	
}
