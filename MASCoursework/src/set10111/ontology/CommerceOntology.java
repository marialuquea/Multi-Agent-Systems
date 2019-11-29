package set10111.ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class CommerceOntology extends BeanOntology
{
	private static Ontology theInstance = new CommerceOntology("my_ontology");

	public static Ontology getInstance(){
		return theInstance;
	}
	//singleton pattern
	private CommerceOntology(String name) {
		super(name);
		try {
			add("set10111.elements");
			add("set10111.agents");
			add("set10111.elements.concepts");
		} catch (BeanOntologyException e) {
			e.printStackTrace();
		}
	}
}
