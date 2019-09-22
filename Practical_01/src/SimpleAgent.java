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
		
		DFAgentDescription dfd = new DFAgentDescription () ;
		dfd . setName ( getAID () ) ;
		ServiceDescription sd = new ServiceDescription () ;
		sd . setType ("Simple - agent ") ;
		sd . setName ( getLocalName () + "-Simple - agent ") ;
		dfd . addServices ( sd ) ;
		try{
			DFService . register (this , dfd ) ;
		}
		catch ( FIPAException e ) {
			e . printStackTrace () ;
		}
	}
}
