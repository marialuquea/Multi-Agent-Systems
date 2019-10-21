import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class Synchronising_Agents_2_2_main 
{
	public static void main(String[] args)
	{
		//Setup JADE environment
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);

		try 
		{
			//Start the agent controller (rma)
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();

			//Start the sender agents
			AgentController ticker = myContainer.createNewAgent("ticker_Agent", NewDay_TickerAgent.class.getCanonicalName(), null);
			ticker.start();

			AgentController simulation = myContainer.createNewAgent("simulation_Agent", SimpleSimulationAgent.class.getCanonicalName(), null);  
			simulation.start();
		}
		catch (Exception e)
		{
			System.out.println("Exception starting agent: " + e.toString());
		}
	}
}
