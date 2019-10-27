package set10111.music_shop;

import java.util.ArrayList;
import java.util.HashMap;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import set10111.music_shop_ontology.ECommerceOntology;
import set10111.music_shop_ontology.elements.*;


public class SellerAgent extends Agent 
{
	private Codec codec = new SLCodec();
	private Ontology ontology = ECommerceOntology.getInstance();
	//stock list, with serial number as the key
	private HashMap<Integer,Item> itemsForSale = new HashMap<>(); 
	
	protected void setup()
	{
		System.out.println("setup() in Seller");
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
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
		itemsForSale.put(cd.getSerialNumber(),cd);
		
		addBehaviour(new QueryBehaviour());
		addBehaviour(new SellBehaviour());
		
	}

	private class QueryBehaviour extends CyclicBehaviour
	{
		@Override
		public void action() 
		{
			//This behaviour should only respond to QUERY_IF messages
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF); 
			ACLMessage msg = receive(mt);
			if(msg != null)
			{
				System.out.println("");
				try 
				{
					ContentElement ce = null;
					System.out.println("msg QUERY IF received by seller: "+msg.getContent()); //print out the message content in SL

					// Let JADE convert from String to Java objects
					// Output will be a ContentElement
					ce = getContentManager().extractContent(msg);
					
					if (ce instanceof Owns) 
					{
						Owns owns = (Owns) ce;
						Item item = owns.getItem();
						
						// Extract the CD name and print it to demonstrate use of the ontology
						CD cd = (CD)item;
						System.out.println("cd no.: "+cd.getSerialNumber()+", name: "+cd.getName());
						
						//check if seller has it in stock
						if(itemsForSale.containsKey(cd.getSerialNumber())) 
						{
							System.out.println("Seller (me) has CD in stock!");
							//sell it
						}
						else 
						{
							System.out.println("CD out of stock");
							//send reject proposal message
						}
					}
				}

				catch (CodecException ce) {
					ce.printStackTrace();
				}
				catch (OntologyException oe) {
					oe.printStackTrace();
				}

			}
			else{
				block();
			}
		}
		
	}
	
	private class SellBehaviour extends CyclicBehaviour
	{
		@Override
		public void action() 
		{
			//This behaviour should only respond to REQUEST messages
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST); 
			ACLMessage msg = receive(mt);
			if(msg != null)
			{
				//System.out.println("msg REQUEST received by seller: "+msg);
				try 
				{
					ContentElement ce = null;
					
					//print out the message content in SL
					System.out.println(""); 
					System.out.println("msg REQUEST content: "+msg.getContent()); 
					
					// Let JADE convert from String to Java objects
					// Output will be a ContentElement
					ce = getContentManager().extractContent(msg);
					
					if(ce instanceof Action) 
					{
						Concept action = ((Action)ce).getAction();
						//System.out.println("action: "+action);
						
						if (action instanceof Sell) 
						{
							Sell order = (Sell)action;
							System.out.println("order buyer/item: "+order.getBuyer().getLocalName());
							Item item = order.getItem();
							System.out.println("item no.: "+item.getSerialNumber());
							
							// Extract the CD name and print it to demonstrate use of the ontology
							if(item instanceof CD)
							{
								CD cd = (CD)item;
								//check if seller has it in stock
								if(itemsForSale.containsKey(cd.getSerialNumber())) 
								{
									//System.out.println("itemsForSale: "+itemsForSale);
									System.out.println("Selling CD " + cd.getName()+" to "+order.getBuyer().getLocalName());
									System.out.println("itemsForSale: " + itemsForSale);
								}
								else 
								{
									System.out.println("You tried to order something out of stock!!!! Check first!");
								}

							}
						}

					}
				}

				catch (CodecException ce) {
					ce.printStackTrace();
				}
				catch (OntologyException oe) {
					oe.printStackTrace();
				}

			}
			else{
				block();
			}
		}

	}

}
