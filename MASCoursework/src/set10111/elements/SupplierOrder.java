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
	public void setBattery(int battery) {
		this.battery = battery;
	}
	public int getBatteryQuantity() {
		return batteryQuantity;
	}
	public void setBatteryQuantity(int batteryQuantity) {
		this.batteryQuantity = batteryQuantity;
	}
	
	
	public int getRAM() {
		return ram;
	}
	public void setRAM(int ram) {
		this.ram = ram;
	}
	public int getRamQuantity() {
		return ramQuantity;
	}
	public void setRamQuantity(int ramQuantity) {
		this.ramQuantity = ramQuantity;
	}
	
	
	public int getScreen() {
		return screen;
	}
	public void setScreen(int screen) {
		this.screen = screen;
	}
	public int getScreenQuantity() {
		return screenQuantity;
	}
	public void setScreenQuantity(int screenQuantity) {
		this.screenQuantity = screenQuantity;
	}
	
	
	public int getStorage() {
		return storage;
	}
	public void setStorage(int storage) {
		this.storage = storage;
	}
	public int getStorageQuantity() {
		return storageQuantity;
	}
	public void setStorageQuantity(int storageQuantity) {
		this.storageQuantity = storageQuantity;
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
