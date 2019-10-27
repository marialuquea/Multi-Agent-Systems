package set10111.music_shop_ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class ECommerceOntology extends BeanOntology{
	
	private static Ontology theInstance = new ECommerceOntology("my_ontology");
	
	public static Ontology getInstance(){
		return theInstance;
	}
	//singleton pattern
	private ECommerceOntology(String name) {
		super(name);
		try {
			add("set10111.music_shop_ontology.elements");
		} catch (BeanOntologyException e) {
			e.printStackTrace();
		}
	}
}
