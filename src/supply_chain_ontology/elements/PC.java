package supply_chain_ontology.elements;

import java.util.List;

import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;

public class PC extends Item
{
	// Used to Identify item name e.g. Laptop or Desktop
	private String name;
	
	@Slot(mandatory = true)
	public String getName()
	{
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
