import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class Message_sending_2_1_main {


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
			AgentController sender = myContainer.createNewAgent("sender", SenderAgent.class.getCanonicalName(), null);
			sender.start();

			AgentController receiver = myContainer.createNewAgent("receiver", ReceiverAgent.class.getCanonicalName(), null);  
			receiver.start();
		}
		catch (Exception e)
		{
			System.out.println("Exception starting agent: " + e.toString());
		}
	}


}
