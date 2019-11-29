package set10111.elements.concepts;

public class ComponentType 
{
	private static ComponentType ct = new ComponentType();
	
	public String SCREEN_5 = "SCREEN_5";
	public String SCREEN_7 = "SCREEN_7";
	
	public String BATTERY_2000 = "BATTERY_2000";
	public String BATTERY_3000 = "BATTERY_3000";
	
	public String STORAGE_64 = "STORAGE_64";
	public String STORAGE_256 = "STORAGE_256";
	
	public String RAM_4 = "RAM_4";
	public String RAM_8 = "RAM_8";

	public static ComponentType getInstance(){
		return ct;
	}
	
	//singleton pattern
	private ComponentType() {}
	
}
