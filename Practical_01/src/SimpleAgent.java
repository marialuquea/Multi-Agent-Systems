import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class SimpleAgent extends Agent {

	// This method is called when the agent is launched
	protected void setup() {
		//Print out a welcome message
		System.out.println("Hello! Agent "+getAID().getName()+" is ready.");
		
		// Registering agent with the Directory Facilitator (yellow pages) 
		DFAgentDescription dfd = new DFAgentDescription();
		// registering AID along with a description of the service that the agent provides to others (in this case "Simple agent")
		// good habit: registering all the agents with the DF so that they can be found at runtime by other agents (possibly running on other computers and written by other people)
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Simple-agent");
		sd.setName(getLocalName() + "-Simple-agent");
		dfd.addServices(sd);
		try{
			DFService.register(this, dfd);
		}
		catch(FIPAException e){
			e.printStackTrace();
		}
	}
	
	// de-registering agent with yellow pages when it terminates
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch(FIPAException e) {
			e.printStackTrace();
		}
	}
}
