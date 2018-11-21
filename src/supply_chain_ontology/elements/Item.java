package supply_chain_ontology.elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Item implements Concept 
{
	private int orderNumber;
	@Slot (mandatory = true)
	public int getOrderNumber()
	{
		return orderNumber;
	}
	
	public void setOrderNumber(int orderNumber)
	{
		this.orderNumber = orderNumber;
	}
}
