package set10111.elements;

import java.util.ArrayList;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;
import set10111.elements.concepts.*;

public class Smartphone implements Concept
{
	private Screen screen;
	private Battery battery;
	private Storage storage;
	private Ram ram;

	@Slot (mandatory = true)
	public Screen getScreen() {
		return screen;
	}
	public void setScreen(Screen screen) {
		this.screen = screen;
	}

	@Slot (mandatory=true)
	public Battery getBattery() {
		return battery;
	}
	public void setBattery(Battery battery) {
		this.battery = battery;
	}

	@Slot (mandatory=true)
	public Storage getStorage() {
		return storage;
	}
	public void setStorage(Storage storage) {
		this.storage = storage;
	}

	@Slot (mandatory=true)
	public Ram getRAM() {
		return ram;
	}
	public void setRAM(Ram ram) {
		this.ram = ram;
	}
	
	public ArrayList<SmartphoneComponent> getComponents() {
		ArrayList<SmartphoneComponent> components = new ArrayList<>();
		components.add(screen);
		components.add(battery);
		components.add(storage);
		components.add(ram);
		return components;
	}
	
	@Override
	public String toString() {
		return String.format("("
		        + "screen: %s, \n\t"
		        + "battery: %s, \n\t"
		        + "storage: %s, \n\t"
		        + "ram: %s),",
		        screen, battery, storage, ram);
	}

}