package supply_chain_ontology.elements;

import jade.content.AgentAction;
import jade.core.AID;

public class Sell implements AgentAction 
{
	private AID customer;
	private Item item;
	
	public AID getCustomer()
	{
		return customer;
	}
	
	public void setCustomer(AID customer)
	{
		this.customer = customer;
	}
	
	public Item getItem() 
	{
		return item;
	}
	
	public void setItem(Item item) 
	{
		this.item = item;
	}
}
