import jade.content.Concept;

public class Item implements Concept
{
	private int serialNumber;
	
	public int getSerialNumber()
	{
		return serialNumber;
	}
	
	public void setSerialNumber(int serialNumber)
	{
		this.serialNumber = serialNumber;
	}
}
