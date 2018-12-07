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

	HashMap<Integer, ArrayList<String>> stockLevel = new HashMap<Integer, ArrayList<String>>();
	// order variable to track number of orders
	int orderNum = 1;
	// See if we are able to count the number of certain components from the list (Will be used for stock checking, once order has been delivered)
	int desktopCPUCount = 0;

	public int dayNum = 1;

	protected void setup() 
	{
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

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
							int dueDate = dayNum + dueInDays;
							// Printing PC name to demo Ontology
							if(it instanceof PC)
							{
								PC pc = (PC)it;


								System.out.println("Manufacturer Received Customer Order: " + pc.getOrderNumber() + " [ " + pc.getName() + " ]");

								// Placing Order with Supplier (Needs to be put into a separate behaviour)

								// Preparing the request message
								ACLMessage msg2 = new ACLMessage(ACLMessage.REQUEST);

								// Set receiver to Supplier Agent
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

								// Sending Message to Supplier
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
								orders.add(Integer.toString(dueDate));
								// NEED TO ADD SCREEN


								// Mapping these List Values to a key
								customerOrders.put(pc.getOrderNumber(), orders);

								// Testing output
								//System.out.println("Order Tracking: " + customerOrders);

								addBehaviour(new StockCheck(myAgent));
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

		public StockCheck(Agent a)
		{
			super(a);
		}

		@Override
		public void action() 
		{
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchConversationId("new order"), MessageTemplate.MatchContent("no-order"));
			ACLMessage msg = myAgent.receive(mt);

			int desktopCPUStock = 0;
			int laptopCPUStock = 0;
			int desktopMotherboardStock = 0;
			int laptopMotherboardStock = 0;
			int ram8GbStock = 0;
			int ram16GbStock = 0;
			int hd1TbStock = 0;
			int hd2TbStock = 0;
			int screenStock = 0;
			int windowsOsStock = 0;
			int linuxOsStock = 0;
			// Seeing if time is the problem
			doWait(5000);
			if (msg != null)
			{
				if(supplierAgent == null)
				{
					supplierAgent = msg.getSender();
				}


				if (msg.getContent().equals("no-order"))
				{
					//System.out.println("No Components received today.");
				}
				else if (msg.getConversationId().equals("new order"))
				{
					ContentElement ce = null;

					// JADE converts String to Java Object, Outputting it as a ContentElement
					try {
						ce = getContentManager().extractContent(msg);
					} catch (CodecException | OntologyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if(ce instanceof Action)
					{
						Concept action = ((Action)ce).getAction();

						if (action instanceof Sell)
						{
							Sell order = (Sell)action;

							Item it = order.getItem();
							// Printing PC name to demo Ontology
							if(it instanceof PC)
							{
								PC pc = (PC)it;
								// Add the components from this to a list, then run the StockCheck like desktopCPU count
								ArrayList<String> stock = new ArrayList<String>();

								// Stock List
								//orders.add(pc.getComponents().get(0).getCPU());
								stock.add(pc.getComponents().get(0).getCPU());
								stock.add(pc.getComponents().get(0).getMotherboard());
								stock.add(pc.getComponents().get(0).getRam());
								stock.add(pc.getComponents().get(0).getHD());
								stock.add(pc.getComponents().get(0).getOS());

								// Mapping these List Values to a key
								stockLevel.put(pc.getOrderNumber(), stock);

								orderNum = 1;

								System.out.println("PC Received Order Num: " + pc.getOrderNumber() + " CPU " + pc.getComponents().get(0).getCPU() + " RAM " + pc.getComponents().get(0).getRam());

								// CPU
								if (stockLevel.get(orderNum).get(0).equals("desktopCPU"))
								{
									++desktopCPUStock;
								}
								else if (stockLevel.get(orderNum).get(0).equals("laptopCPU"))
								{
									++laptopCPUStock;
								}

								// Motherboard
								if (stockLevel.get(orderNum).get(1).equals("desktopMotherboard"))
								{
									++desktopMotherboardStock;
								}
								else if (stockLevel.get(orderNum).get(1).equals("laptopMotherboard"))
								{
									++laptopMotherboardStock;
								}

								// RAM
								if (stockLevel.get(orderNum).get(2).equals("8Gb"))
								{
									++ram8GbStock;
								}
								else if (stockLevel.get(orderNum).get(2).equals("16Gb"))
								{
									++ram16GbStock;
								}

								//HDD
								if (stockLevel.get(orderNum).get(3).equals("1Tb"))
								{
									++hd1TbStock;
								}
								if (stockLevel.get(orderNum).get(3).equals("2Tb"))
								{
									++hd2TbStock;
								}

								//Screen (Check if added)

								//OS
								if (stockLevel.get(orderNum).get(4).equals("Windows"))
								{
									++windowsOsStock;
								}
								else if (stockLevel.get(orderNum).get(4).equals("Linux"))
								{
									++linuxOsStock;
								}


								// Order Complete which means the Day is done for the Manufacturer Agent
								++orderNum;

							}
						}

					}
				}
				else
				{
					System.out.println("RANDOM MESSAGE RECEIVED FROM SUPPLIER");
				}


				// After this begin new behaviour (delivery)
				// ^ For Loop that goes through, customerOrders and stockLevel
				// See's if they contain the same stuff, if so send order
				//System.out.println("Customer Orders Received: " + customerOrders);

				// Sets what components we need = 0, may make these global and -- when component has been used
				int needDesktopCPU = 0;
				int needLaptopCPU = 0;
				int needDesktopMotherboard = 0;
				int needLaptopMotherboard = 0;
				int needRam8Gb = 0;
				int needRam16Gb = 0;
				int needHD1Tb = 0;
				int needHD2Tb = 0;
				int needWindowsOs = 0;
				int needLinuxOs = 0;

				ArrayList<String> stock = new ArrayList<String>();
				
				//System.out.println("Customer Order Test: " + customerOrders.get(1));
				for (int i =1; i <= customerOrders.size(); ++i)
				{
					// Works, outputs each order order 1 = i(1), 2 = i(2)
					//System.out.println("I Value: " + i);
					//System.out.println("Customer Order: " + customerOrders.get(i));

					// Need to store the due date of the order
					// If due date == current day then go through all this crap

					String day = String.valueOf(dayNum);

					// If the due date of the Customer order is equal to today then:
					if (customerOrders.get(i).get(5).equals(day))
					{
						System.out.println("ORDER IS DUE: " + customerOrders.get(i));
						// Move the if's to this
						
						// Now Checking if the Components are in stock
						
						// CPU
						if (customerOrders.get(i).get(0).equals("desktopCPU") && desktopCPUStock != 0)
						{
							System.out.println("Desktop CPU in stock and able to ship.");
							--desktopCPUStock;
						}
						else if (customerOrders.get(i).get(0).equals("laptopCPU") && laptopCPUStock != 0)
						{
							System.out.println("Laptop CPU in stock and able to ship.");
							--laptopCPUStock;
						}
						else
						{
							System.out.println("No CPU's not in stock, cannot ship order.");
							break;
						}
						
						// Motherboard
						if(customerOrders.get(i).get(1).equals("desktopMotherboard") && desktopMotherboardStock !=0)
						{
							System.out.println("Desktop Motherboard in stock and able to ship.");
							--desktopMotherboardStock;
						}
						else if(customerOrders.get(i).get(1).equals("laptopMotherboard") && laptopMotherboardStock !=0)
						{
							System.out.println("Laptop Motherboard in stock and able to ship.");
							--laptopMotherboardStock;
						}
						else
						{
							System.out.println("No Motherboards in stock, cannot ship order.");
							break;
						}
						
						// RAM
						if (customerOrders.get(i).get(2).equals("8Gb") && ram8GbStock !=0)
						{
							System.out.println("8Gb ram in stock and able to ship.");
							--ram8GbStock;
						}
						else if (customerOrders.get(i).get(2).equals("16Gb") && ram16GbStock !=0)
						{
							System.out.println("16Gb ram in stock and able to ship.");
							--ram16GbStock;
						}
						else
						{
							System.out.println("No ram in stock, cannot ship order.");
							break;
						}
						
						// HD
						if (customerOrders.get(i).get(3).equals("1Tb") && hd1TbStock !=0)
						{
							System.out.println("1Tb hd in stock and able to ship.");
							--hd1TbStock;
						}
						else if (customerOrders.get(i).get(3).equals("2Tb") && hd2TbStock !=0)
						{
							System.out.println("2Tb hd in stock and able to ship.");
							--hd2TbStock;
						}
						else
						{
							System.out.println("No HD in stock, cannot ship order.");
							break;
						}
						
						// OS
						if (customerOrders.get(i).get(4).equals("Windows") && windowsOsStock !=0)
						{
							System.out.println("Windows OS in stock and able to ship.");
							--windowsOsStock;
						}
						else if (customerOrders.get(i).get(4).equals("Linux") && linuxOsStock !=0)
						{
							System.out.println("Linux OS in stock and able to ship.");
							--linuxOsStock;
						}
						else
						{
							System.out.println("No OS in stock, cannot ship order.");
							break;
						}
						
						System.out.println("ORDER IS ABLE TO BE SENT: " + customerOrders.get(i));
						
						/*
						// CPU
						if (customerOrders.get(i).get(0).equals("desktopCPU"))
						{
							++needDesktopCPU;
						}
						else if (customerOrders.get(i).get(0).equals("laptopCPU"))
						{
							++needLaptopCPU;
						}

						// Motherboard
						if (customerOrders.get(i).get(1).equals("desktopMotherboard"))
						{
							++needDesktopMotherboard;
						}
						else if (customerOrders.get(i).get(1).equals("laptopMotherboard"))
						{
							++needLaptopMotherboard;
						}

						// Ram
						if (customerOrders.get(i).get(2).equals("8Gb"))
						{
							++needRam8Gb;
						}
						else if (customerOrders.get(i).get(2).equals("16Gb"))
						{
							++needRam16Gb;
						}

						// HD
						if (customerOrders.get(i).get(3).equals("1Tb"))
						{
							++needHD1Tb;
						}
						else if (customerOrders.get(i).get(3).equals("2Tb"))
						{
							++needHD2Tb;
						}

						// OS
						if (customerOrders.get(i).get(4).equals("Linux"))
						{
							++needLinuxOs;
						}
						else if (customerOrders.get(i).get(4).equals("Windows"))
						{
							++needWindowsOs;
						}

						
						// Need to figure out

						if (needDesktopCPU == 1 || needLaptopCPU == 1)
						{	
							// Add more if's here, somehow need to remove the values

							if (needDesktopMotherboard == 1 || needLaptopCPU == 1)
							{
								if (needRam8Gb == 1 || needRam16Gb == 1)
								{
									if (needHD1Tb == 1 || needHD2Tb == 1)
									{
										if (needLinuxOs == 1 || needWindowsOs == 1)
										{
											System.out.println("PC IS ABLE TO BE SENT");
										}
									}
								}
							}

						}		
						*/
					}
				}
				// Checking what Components I need
				System.out.println("Needed Components: " + "Desktop CPU: " + needDesktopCPU + " Laptop CPU: " + needLaptopCPU + " Desktop Motherboard: " + needDesktopMotherboard + " Laptop Motherboard: " + needLaptopMotherboard
						);
				// Stock Check
				System.out.println("Stock Check: " + "Desktop CPU: " + desktopCPUStock + " Desktop Motherboard: " + desktopMotherboardStock + " Laptop CPU: " + laptopCPUStock + " Laptop Motherboard: " + laptopMotherboardStock
						+ " Ram 8GB: " + ram8GbStock + " Ram 16GB: " + ram16GbStock + " HDD 1TB: " + hd1TbStock + " HDD 2TB: " + hd2TbStock + " Windows OS: " + windowsOsStock + " Linux OS: " + linuxOsStock);


				addBehaviour(new DayComplete(myAgent));
				myAgent.removeBehaviour(this);
			}
			else
			{
				block();
			}	
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
