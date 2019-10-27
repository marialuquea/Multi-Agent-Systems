package set10111.music_shop;

import java.util.ArrayList;

import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import set10111.music_shop_ontology.ECommerceOntology;
import set10111.music_shop_ontology.elements.*;

public class CautiousBuyerAgent extends Agent 
{
	private Codec codec = new SLCodec();
	private Ontology ontology = ECommerceOntology.getInstance();
	private AID sellerAID;
	
	protected void setup()
	{
		System.out.println("setup() in Cautions Buyer");
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		String[] args = (String[])this.getArguments();
		sellerAID = new AID("seller",AID.ISLOCALNAME);
		addBehaviour(new QueryBuyerBehaviour(this,10000));
	}
	
	private class QueryBuyerBehaviour extends TickerBehaviour
	{
		public QueryBuyerBehaviour(Agent a, long period) 
		{
			super(a, period);
		}
		
		private boolean finished = false;
		
		protected void onTick() 
		{
			// Prepare the Query-IF message
			ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
			msg.addReceiver(sellerAID); // sellerAID is the AID of the Seller agent
			msg.setLanguage(codec.getName());
			msg.setOntology(ontology.getName()); 
			
			// Prepare the content. 
			CD cd = new CD();
			cd.setName("Synchronicity");
			cd.setSerialNumber(123);
			ArrayList<Track> tracks = new ArrayList<Track>();
			Track t = new Track();
			t.setName("Every breath you take");
			t.setDuration(230);
			tracks.add(t);
			t = new Track();
			t.setName("King of pain");
			t.setDuration(500);
			tracks.add(t);
			cd.setTracks(tracks);
			
			Owns owns = new Owns();
			owns.setOwner(sellerAID);
			owns.setItem(cd);
			
			try {
			 // Let JADE convert from Java objects to string
			 getContentManager().fillContent(msg, owns);
			 send(msg);
			}
			catch (CodecException ce) {
			 ce.printStackTrace();
			}
			catch (OntologyException oe) {
			 oe.printStackTrace();
			} 
			finished = true;
		}

	}
}
