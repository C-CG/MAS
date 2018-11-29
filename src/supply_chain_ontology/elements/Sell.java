package supply_chain_ontology.elements;

import jade.content.AgentAction;
import jade.core.AID;
import jade.util.leap.ArrayList;

public class Sell implements AgentAction 
{
	private AID customer;
	private Item item;
	private java.util.ArrayList<Integer> details;
	
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
	
	public java.util.ArrayList<Integer> getDetails()
	{
		return details;
	}
	
	public void setDetails(java.util.ArrayList<Integer> details)
	{
		this.details = details;
	}
	
	
}
