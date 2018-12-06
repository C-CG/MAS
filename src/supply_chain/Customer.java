package supply_chain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import jade.core.Agent;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.List;
import jade.util.leap.Map;
import supply_chain.Manufacturer.BuyBehaviour;
import jade.core.AID;
//Importing Ontology/Elements
import supply_chain_ontology.SupplyChainOntology;
import supply_chain_ontology.elements.*;

public class Customer extends Agent 
{
	// Look into what the "codec" does
	private Codec codec = new SLCodec();
	private Ontology ontology = SupplyChainOntology.getInstance();
	
	// AID of Manufacturer
	private AID manufacturerAgent;
	private AID tickerAgent;
	
	// Variables
	public int orderNum = 1;
	public int dayNum = 1;
	public int dueInDays;
	HashMap<Integer, Integer> customerOrders = new HashMap<Integer, Integer>();
	
	// WORKS!!!!!! 
	HashMap<Integer, ArrayList<Integer>> customerOrdersTest = new HashMap<Integer, ArrayList<Integer>>();
	
	protected void setup() 
	 { 
		 getContentManager().registerLanguage(codec);
		 getContentManager().registerOntology(ontology);
		 String[] args = (String[])this.getArguments();
		 manufacturerAgent = new AID("manufacturer",AID.ISLOCALNAME);
		 
		 // Register the Agent in the Directory
		 DFAgentDescription dfd = new DFAgentDescription();
		 dfd.setName(getAID());

		 // Registers what "service" the Agent will offer
		 ServiceDescription sd = new ServiceDescription();
		 sd.setType("customer");
		 sd.setName("customer-agent");

		 // Adding Service to Directory
		 dfd.addServices(sd);

		 // Try/Catch for adding Service to Directory
		 try
		 {
			 DFService.register(this, dfd);
		 }
		 catch(FIPAException e)
		 {
			 e.printStackTrace();
		 }
		 
		// Add Behaviours
		 addBehaviour(new TickerWaiter(this));
	 }
	
	public class TickerWaiter extends CyclicBehaviour
	{
		public TickerWaiter(Agent a)
		{
			super(a);
		}

		@Override
		public void action() 
		{
			// Setting up Messaging for Communication/Working
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("end"), MessageTemplate.MatchContent("new day"));
			ACLMessage msg = myAgent.receive(mt);
			
			if (msg != null)
			{
				if(tickerAgent == null)
				{
					tickerAgent = msg.getSender();
				}
				
				if(msg.getContent().equals("new day"))
				{
					// Add Behaviours
					//System.out.println("Message Received from Ticker Agent, starting work. - Customer");
					myAgent.addBehaviour(new BuyBehaviour(myAgent));
					myAgent.addBehaviour(new CheckDelivery(myAgent, dayNum, dueInDays, orderNum));
				}
				else
				{
					System.out.println("Deleting Agent - " + getAID().getName());
					// Simulation has ended, no use for this Agent now
					myAgent.doDelete();
				}
			}
			else
			{
				block();
			}
			
		}
	}
	
	
	
	
	private class BuyBehaviour extends CyclicBehaviour
	{

		public BuyBehaviour(Agent a)
		{
			super(a);
		}

		@Override
		public void action() {
			// Preparing the request message
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

			// Set receiver to Manufacturer Agent
			msg.addReceiver(manufacturerAgent);
			msg.setLanguage(codec.getName());
			msg.setOntology(ontology.getName()); 

			// Preparing order of PC (Random Method)
			Double rand;
			rand = Math.random();
			
			//Testing Floor function
			int price;
			int quantity;
			
			dueInDays = floor(1,10, rand);
			price = floor(600,200,rand);
			quantity = floor(1,10,rand);
			
			// Setting Up PC/Components			
			ArrayList<Components> components = new ArrayList<Components>();
			Components c = new Components();
			PC pc = new PC();
			
			
			
			if(rand < 0.5)
			{
				// Buy a Desktop
				pc.setName("Desktop");
				pc.setOrderNumber(orderNum);
				c.setCPU("desktopCPU");
				c.setMotherboard("desktopMotherboard");
				c.setScreen(false);			
			}
			else
			{
				// Buy a Laptop
				pc.setName("Laptop");
				pc.setOrderNumber(orderNum);
				c.setCPU("laptopCPU");
				c.setMotherboard("laptopMotherboard");
				c.setScreen(true);	
			}
			
			// Generate new Random Number
			rand = Math.random();
			
			if (rand < 0.5)
			{
				c.setRam("8Gb");
			}
			else
			{
				c.setRam("16Gb");
			}
			
			// Generate new Random Number
			rand = Math.random();
			
			if (rand < 0.5)
			{
				c.setHD("1Tb");
			}
			else
			{
				c.setHD("2Tb");
			}
			
			// Generate new Random Number
			rand = Math.random();
			
			if (rand < 0.5)
			{
				c.setOS("Windows");
			}
			else
			{
				c.setOS("Linux");
			}
			
			// Adding components to ArrayList
			components.add(c);
			pc.setComponents(components);
			
			// Testing of Adding Orders to List/HashMap
			ArrayList<Integer> details = new ArrayList<Integer>();
			// Adding Day of Order, Due Date, Price into List
			details.add(dayNum);
			details.add(dueInDays);
			details.add(price);
			
			// Order, sets Buyer and what Item they want
			Sell order = new Sell();
			order.setCustomer(myAgent.getAID());
			order.setItem(pc);
			// need to add orderDetails here
			order.setCurrentDay(dayNum);
			order.setDueInDays(dueInDays);
			order.setPrice(price);

			// Sending Message to Manufacturer
			// IMPORTANT: Set up this way due to FIPA, otherwise we get an exception (crash)
			Action request = new Action();
			request.setAction(order);
			request.setActor(manufacturerAgent); // the agent that you request to perform the action
			try 
			{
				// Output to Console
				System.out.println("Customer Placing Order...");
				doWait(2000);
				System.out.println("Customer Order: " + pc.getOrderNumber() + " [ " + pc.getName() + " " + c.getRam() + " " + c.getHD() + " " + c.getOS() + " ]" + " Due: " + dueInDays + " days" +" Price: £" + price);
				// Maps Order Number to due days e.g. 1 7
				customerOrders.put(orderNum, dueInDays);
				
				// Testing the Set/Get of PC Components
				//System.out.println("Get components: " + pc.getComponents().get(0).getRam());

				
				// Add order details to "Sell order"
				
				
				// Mapping these List Values to a key
				customerOrdersTest.put(orderNum, details);
				//System.out.println("Testing HashMap/ArrayList: " + customerOrdersTest.toString());
				
				// Trying to get Individual Values from each key
				//System.out.println("Testing to get Individual Values from each Key: " + "Day Number: " +  customerOrdersTest.get(1).get(0) + " Due Days: " +  customerOrdersTest.get(1).get(1));
				
				doWait(2000);
				// ++ Counter for Order Number here
				++orderNum;
				// Let JADE convert from Java objects to string
				getContentManager().fillContent(msg, request); //send the wrapper object
				send(msg);
				
			}
			catch (CodecException ce) 
			{
				ce.printStackTrace();
			}
			catch (OntologyException oe) 
			{
				oe.printStackTrace();
			} 
			
			// Order Complete which means the Day is done for the Customer Agent
			addBehaviour(new DayComplete(myAgent));
			// Remove Behaviour
			myAgent.removeBehaviour(this);
			
		}
		
	}
	
	public class DayComplete extends CyclicBehaviour
	{
		
		public DayComplete(Agent a)
		{
			super(a);
		}
		
		@Override
		public void action() 
		{
			// Finished "working" for the Day, relay message to Ticker Agent
			ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
			tick.setContent("done");
			tick.addReceiver(tickerAgent);
			myAgent.send(tick);
			// Remove Behaviour
			myAgent.removeBehaviour(this);
			++dayNum;
			
		}
		
	}
	
	
	public int floor(int num1, int num2, double num3)
	{
		return (int) (num1 + num2 * num3);
	}
	
	// Call this in DayComplete? (Sloppy but would work)
	public class CheckDelivery extends CyclicBehaviour
	{
		public CheckDelivery(Agent a, int dayNum, int dueInDays, int orderNum)
		{
			super(a);
		}

		@Override
		public void action() 
		{
			int dueDate;
			// Gets the due date for the First Order
			//dueDate = 1 + customerOrders.get(1);
			// dueDate test working with the HashMap and ArrayList
			dueDate = customerOrdersTest.get(1).get(0) + customerOrdersTest.get(1).get(1);
		
			/*
			System.out.println("List: " + customerOrders);
			System.out.println("Get Specific Item in List: " + customerOrders.get(1));
			myAgent.removeBehaviour(this);
			*/
			// Works
			//System.out.println("Due Date for Order 1: " + dueDate);
			//System.out.println(dayNum);
			if (dayNum == dueDate)
			{
				System.out.println("Order: 1 should be delivered.");
				myAgent.removeBehaviour(this);
			}
			else
			{
				myAgent.removeBehaviour(this);
			}
			
		}
	}
}
