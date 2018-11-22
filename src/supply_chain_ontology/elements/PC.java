package supply_chain_ontology.elements;

import java.util.List;

import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;

public class PC extends Item
{
	// PC Variables
	private String name;
	private List<Components> components;

	
	@Slot(mandatory = true)
	public String getName()
	{
		return name;
	}
	
	public void setName(String name) 
	{
		this.name = name;
	}
	
	@Slot(mandatory = true)
	public List<Components> componentList()
	{
		return components;
	}
	
	public void setComponents(List<Components> components)
	{
		this.components = components;
	}
}
