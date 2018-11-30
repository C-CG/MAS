package supply_chain_ontology.elements;

import jade.content.AgentAction;
import jade.core.AID;
import jade.util.leap.ArrayList;

public class Sell implements AgentAction 
{
	private AID customer;
	private Item item;
	private java.util.ArrayList<Integer> details;
	private int dayNum;
	private int dueInDays;
	private int price;
	
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
	
	public int getCurrentDay()
	{
		return dayNum;
	}
	
	public void setCurrentDay(int dayNum)
	{
		this.dayNum = dayNum;
	}
	
	public int getDueInDays()
	{
		return dueInDays;
	}
	
	public void setDueInDays(int dueInDays)
	{
		this.dueInDays = dueInDays;
	}
	
	public int getPrice()
	{
		return price;
	}
	
	public void setPrice(int price)
	{
		this.price = price;
	}
	
	
}
