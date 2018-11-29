package supply_chain;

import java.util.ArrayList;
import java.util.HashMap;
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
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import supply_chain.Customer.DayComplete;
import jade.core.AID;

// Importing Ontology/Elements
import supply_chain_ontology.SupplyChainOntology;
import supply_chain_ontology.elements.*;

public class ManufacturerTest extends Agent 
{
	// Look into what the "codec" does
	private Codec codec = new SLCodec();
	private Ontology ontology = SupplyChainOntology.getInstance();
	// List of Stock (unlimited), using orderNumber as key
	private HashMap<Integer,Item> computersForSale = new HashMap<>();
	
	// AIDs for other Agents
	private AID tickerAgent;
	private AID customerAgent;
	private AID supplierAgent;
	
	// HashMap / list to map an order/components (Used for the building of a PC)
	HashMap<Integer, ArrayList<String>> customerOrders = new HashMap<Integer, ArrayList<String>>();
	// order variable to track number of orders
	int orderNum = 1;
	// See if we are able to count the number of certain components from the list (Will be used for stock checking, once order has been delivered)
	int desktopCPUCount = 0;
	
	public int dayNum = 1;
	
	protected void setup() 
	{
		// Register the Agent in the Directory
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());

		// Registers what "service" the Agent will offer
		ServiceDescription sd = new ServiceDescription();
		sd.setType("manufacturer");
		sd.setName("manufacturer-agent");

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
		
		
		// Look into what these do
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

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
			
			if (msg !=null)
			{
				if(tickerAgent == null)
				{
					tickerAgent = msg.getSender();
				}
				
				// Checking if the message received states a "new day", if so do work.
				if(msg.getContent().equals("new day"))
				{
					// Add Behaviours
					//System.out.println("Message Received from Ticker Agent, starting work. - Manufacturer");
					addBehaviour(new SellBehaviour());
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
	
	private class SellBehaviour extends CyclicBehaviour
	{

		@Override
		public void action() 
		{
			// Set Supplier Agent
			supplierAgent = new AID("supplier",AID.ISLOCALNAME);
			
			// Responds to Customer REQUEST messages only
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = receive(mt);
			
			// Message Validation
			if(msg !=null)
			{
				try
				{
					ContentElement ce = null;
					
					// JADE converts String to Java Object, Outputting it as a ContentElement
					ce = getContentManager().extractContent(msg);
					
					if(ce instanceof Action)
					{
						Concept action = ((Action)ce).getAction();
						
						if (action instanceof Sell)
						{
							Sell order = (Sell)action;
							
							Item it = order.getItem();
							int dueInDays = order.getDueInDays();
							int price = order.getPrice();
							
							System.out.println("Price: " + price + " Due: " + dueInDays);
							// Printing PC name to demo Ontology
							if(it instanceof PC)
							{
								PC pc = (PC)it;
								
								
								System.out.println("Manufacturer Received Customer Order: " + pc.getOrderNumber() + " [ " + pc.getName() + " ]");
								
								// Placing Order with Supplier (Needs to be put into a separate behaviour)
								
								// Preparing the request message
								ACLMessage msg2 = new ACLMessage(ACLMessage.REQUEST);

								// Set receiver to Manufacturer Agent
								msg2.addReceiver(supplierAgent);
								msg2.setLanguage(codec.getName());
								msg2.setOntology(ontology.getName()); 
								
								
								// Order, sets Buyer and what Item they want
								Sell manufacturerOrder = new Sell();
								manufacturerOrder.setCustomer(myAgent.getAID());
								manufacturerOrder.setItem(pc);
								manufacturerOrder.setCurrentDay(dayNum);
								manufacturerOrder.setDueInDays(dueInDays);
								manufacturerOrder.setPrice(price);
								
								// Sending Message to Manufacturer
								// IMPORTANT: Set up this way due to FIPA, otherwise we get an exception (crash)
								Action request = new Action();
								request.setAction(manufacturerOrder);
								request.setActor(supplierAgent); // the agent that you request to perform the action
								
								try 
								{
									// Output to Console
									System.out.println("Manufacturer Placing Order...");
									doWait(2000);
									
									// Let JADE convert from Java objects to string
									getContentManager().fillContent(msg2, request); //send the wrapper object
									send(msg2);
									
								}
								catch (CodecException ce2) 
								{
									ce2.printStackTrace();
								}
								catch (OntologyException oe) 
								{
									oe.printStackTrace();
								} 
								
								
								// Storing of a Customers Order (Need to put this in a seperate class StockCheck)
								// Testing of Adding Orders to List/HashMap
								ArrayList<String> orders = new ArrayList<String>();
								
								// Adding Day of Order, Due Date, Price into List
								orders.add(pc.getComponents().get(0).getCPU());
								orders.add(pc.getComponents().get(0).getMotherboard());
								orders.add(pc.getComponents().get(0).getRam());
								orders.add(pc.getComponents().get(0).getHD());
								orders.add(pc.getComponents().get(0).getOS());
								// NEED TO ADD SCREEN
								
								
								// Mapping these List Values to a key
								customerOrders.put(pc.getOrderNumber(), orders);
								
								// Testing output
								System.out.println("Order Tracking: " + customerOrders);
								
								
								
								//System.out.println(customerOrders.size());
								
								
									if (customerOrders.get(orderNum).get(0).equals("desktopCPU"))
									{
										++desktopCPUCount;
									}
									else
									{
										// nothing
									}
								
								
								System.out.println("Counting Desktop CPU's: " + desktopCPUCount);
								
								
								// Order Complete which means the Day is done for the Manufacturer Agent
								++orderNum;
								addBehaviour(new DayComplete(myAgent));
								// Remove Behaviour
								myAgent.removeBehaviour(this);
							}
						}
					}
				}
				catch(CodecException ce)
				{
					ce.printStackTrace();
				}
				catch(OntologyException oe) 
				{
					oe.printStackTrace();
				}
				
			}
			
		}
		
	}
	
	public class StockCheck extends CyclicBehaviour
	{

		@Override
		public void action() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	
	// Used for Purchasing Compontents from the Supplier/s
	public class BuyBehaviour extends CyclicBehaviour
	{
		public BuyBehaviour(Agent a)
		{
			super(a);
		}
		
		@Override
		public void action() 
		{
			
		}
		
	}
	
	public class DayComplete extends CyclicBehaviour
	{
		
		public DayComplete(Agent a)
		{
			super(a);
		}
		// Need to re-do this behaviour to wait and make it cleaner, not just run here. Run in the TickerWaiter
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
}
