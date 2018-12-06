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
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import supply_chain.Manufacturer.DayComplete;
import supply_chain.Manufacturer.TickerWaiter;
import supply_chain.ManufacturerTest.BuyBehaviour;
import supply_chain_ontology.SupplyChainOntology;
import supply_chain_ontology.elements.Components;
import supply_chain_ontology.elements.Item;
import supply_chain_ontology.elements.PC;
import supply_chain_ontology.elements.Sell;
import jade.core.AID;

public class SupplierTest extends Agent 
{
	// Look into what the "codec" does
	private Codec codec = new SLCodec();
	private Ontology ontology = SupplyChainOntology.getInstance();

	// AIDs for other Agents
	private AID tickerAgent;
	private AID customerAgent;
	private AID manufacturerAgent;

	int totalPrice = 0;
	ArrayList<Integer> orderDetails;
	int currentDay;
	int price;
	int dueInDays;

	// Used to store all the orders received from the Customer Agent
	HashMap<Integer, ArrayList<String>> customerOrders = new HashMap<Integer, ArrayList<String>>();


	// Customer Order

	protected void setup()
	{
		// Register the Agent in the Directory
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());

		// Registers what "service" the Agent will offer
		ServiceDescription sd = new ServiceDescription();
		sd.setType("supplier");
		sd.setName("supplier-agent");

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
		
		manufacturerAgent = new AID("manufacturer",AID.ISLOCALNAME);
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

					// Add Behaviour
					myAgent.addBehaviour(new ReceiveOrder());
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

	public class ReceiveOrder extends CyclicBehaviour
	{

		@Override
		public void action() 
		{
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

							dueInDays = order.getDueInDays();
							price = order.getPrice();
							currentDay = order.getCurrentDay();
							// Printing PC name to demo Ontology
							if(it instanceof PC)
							{
								PC pc = (PC)it;

								System.out.println("Supplier Received Manufacturer Order: " + pc.getOrderNumber() + " [ " + pc.getName() + " ]");

								// Testing output of details
								//System.out.println("Testing Order Details Extraction from Order: " + orderDetails);

								myAgent.addBehaviour(new SelectSupplier());

								doWait(5000);

								int motherboard = 0;
								int cpu = 0;
								int ram = 0;
								int hdd = 0;
								int screen = 0;
								int os = 0;
								int dueDate = currentDay + dueInDays;
								SelectSupplier supplier = new SelectSupplier();
								int selectedSupplier = supplier.returnSupplier();

								// change to cases
								if (selectedSupplier == 1)
								{
									if (pc.getName().equals("Desktop"))
									{
										motherboard = 75;
										cpu = 150;
										screen = 0;
									}
									else
									{
										motherboard = 125;
										cpu = 200;
										screen = 100;
									}


									if (pc.getComponents().get(0).getHD().equals("1Tb"))
									{
										hdd = 50;
									}
									else
									{
										hdd = 75;
									}

									if (pc.getComponents().get(0).getRam().equals("8Gb"))
									{
										ram = 50;
									}
									else
									{
										ram = 90;
									}

									if (pc.getComponents().get(0).getOS().equals("Windows"))
									{
										os = 75;
									}
									else
									{
										os = 0;
									}

									totalPrice = motherboard + cpu + screen + hdd + ram + os;
								}
								else if (selectedSupplier == 2)
								{
									if (pc.getName().equals("Desktop"))
									{
										motherboard = 60;
										cpu = 130;
										screen = 0;
									}
									else
									{
										motherboard = 115;
										cpu = 175;
										screen = 80;
									}


									if (pc.getComponents().get(0).getHD().equals("1Tb"))
									{
										hdd = 45;
									}
									else
									{
										hdd = 65;
									}

									if (pc.getComponents().get(0).getRam().equals("8Gb"))
									{
										ram = 40;
									}
									else
									{
										ram = 80;
									}

									if (pc.getComponents().get(0).getOS().equals("Windows"))
									{
										os = 75;
									}
									else
									{
										os = 0;
									}

									totalPrice = motherboard + cpu + screen + hdd + ram + os;	
								}
								else if (selectedSupplier == 3)
								{
									if (pc.getName().equals("Desktop"))
									{
										motherboard = 50;
										cpu = 110;
										screen = 0;
									}
									else
									{
										motherboard = 95;
										cpu = 150;
										screen = 60;
									}


									if (pc.getComponents().get(0).getHD().equals("1Tb"))
									{
										hdd = 35;
									}
									else
									{
										hdd = 55;
									}

									if (pc.getComponents().get(0).getRam().equals("8Gb"))
									{
										ram = 30;
									}
									else
									{
										ram = 70;
									}

									if (pc.getComponents().get(0).getOS().equals("Windows"))
									{
										os = 75;
									}
									else
									{
										os = 0;
									}

									totalPrice = motherboard + cpu + screen + hdd + ram + os;
								}

								// Need to create a list to store PC Specs, price and dueDate (-1 ??)
								ArrayList<String> orders = new ArrayList<String>();

								// Converting Int Values to Strings to be stored in the List (orders)
								String due = Integer.toString(dueDate);
								String cost = Integer.toString(totalPrice);
								String day = Integer.toString(currentDay);
								// List Order Details
								orders.add(due);
								orders.add(cost);
								// List Components
								orders.add(pc.getName());
								orders.add(pc.getComponents().get(0).getCPU());
								orders.add(pc.getComponents().get(0).getMotherboard());
								orders.add(pc.getComponents().get(0).getRam());
								orders.add(pc.getComponents().get(0).getHD());
								orders.add(pc.getComponents().get(0).getOS());



								// Mapping these List Values to a key
								customerOrders.put(pc.getOrderNumber(), orders);

								// Testing output (works fine)
								//System.out.println("Order Tracking Supplier: " + customerOrders);

								//System.out.println("Supplier: " + selectedSupplier + " Due Date: " + dueDate  +  " Price: " + "£" + totalPrice);

								// Now need to loop through the list and retrieve the order number/due date
								// if the due date = the current day, then sell order the matching pc to the manufacturer
								// ^ would be in the SellBehaviour class myAgent.addBehaviour(new SellBehaviour(myAgent));
								// for loop with an if inside
								// Testing output

								int orderNum = 1;
								// Create list of components here
								int cpuSent = 0;


								ACLMessage sold = new ACLMessage(ACLMessage.INFORM);
								sold.addReceiver(manufacturerAgent);
								
								
								for (int i=0; i < customerOrders.size(); i++)
								{

									// Add if statement in here
									if (customerOrders.get(orderNum).get(0).equals(day))
									{
										
										//System.out.println("ORDER SENT TO MANUFACTURER " + customerOrders.get(orderNum));
										
										// Create a new PC to store these components in (from customerOrders list)
										PC soldPC = new PC();
										ArrayList<Components> soldComponents = new ArrayList<Components>();
										Components soldC = new Components();

										// Adding the various components to the "Sold PC"
										soldPC.setName(customerOrders.get(orderNum).get(2)); // Name
										soldPC.setOrderNumber(orderNum); // Order Number

										soldC.setCPU(customerOrders.get(orderNum).get(3)); // CPU
										soldC.setMotherboard(customerOrders.get(orderNum).get(4)); // Motherboard
										soldC.setRam(customerOrders.get(orderNum).get(5)); //RAM
										soldC.setHD(customerOrders.get(orderNum).get(6)); //HD
										soldC.setOS(customerOrders.get(orderNum).get(7)); //OS
										// Need to add screen
										soldC.setScreen(false);

										// Adding Components to ArrayList/Setting them
										soldComponents.add(soldC);
										soldPC.setComponents(soldComponents);

										// Set receive to Manufacturer Agent
										sold.setContent("order");
										sold.addReceiver(manufacturerAgent);
										sold.setLanguage(codec.getName());
										sold.setOntology(ontology.getName());
										sold.setConversationId("new order");

										// Order
										Sell soldOrder = new Sell();
										soldOrder.setCustomer(myAgent.getAID());
										soldOrder.setItem(soldPC);
										// Testing to see if this is why it breaks
										soldOrder.setCurrentDay(currentDay);
										soldOrder.setDueInDays(dueInDays);
										soldOrder.setPrice(price);

										// Sending Message to Manufacturer
										Action request = new Action();
										request.setAction(soldOrder);
										request.setActor(manufacturerAgent);
										
										//System.out.println("Testing PC Part (soldPC): " + soldPC.getName());

										try
										{
											getContentManager().fillContent(sold, request);
											//System.out.println("Sending PC to Manufacturer: ID: " + sold.getConversationId());
											send(sold);
											break;

										}

										catch (CodecException ce2) 
										{
											ce2.printStackTrace();
										}
										catch (OntologyException oe) 
										{
											oe.printStackTrace();
										} 
										
									}
									else
									{
										orderNum++;
									}
								}
								doWait(5000);
								sold.setContent("no-order");
								send(sold);
								
								myAgent.addBehaviour(new DayComplete(myAgent));
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


	// Need to change to One Shot, same with DayComplete (One shot means runs once per day, doesn't refresh or need messages)
	public class SelectSupplier extends OneShotBehaviour
	{		
		public int selectedSupplier;
		@Override
		public void action() 
		{
			returnSupplier();
		}

		public int returnSupplier()
		{

			int dueDate = currentDay + dueInDays;

			// if it's not due the next day, always picks Supplier 2. Need to work out how to differentiate them (price).

			if (currentDay + 1 == dueDate || currentDay + 2 == dueDate)
			{
				// Then Supplier 1
				selectedSupplier = 1;
			}
			else if (currentDay + 3 == dueDate || currentDay + 4 == dueDate || currentDay + 5 == dueDate || currentDay + 6 == dueDate )
			{
				selectedSupplier = 2;
			}
			else if (currentDay + 7 == dueDate || currentDay + 8 == dueDate || currentDay + 9 == dueDate || currentDay + 10 == dueDate)
			{
				selectedSupplier = 3;
			}

			return selectedSupplier;

		}
	}



	private class SellBehaviour extends CyclicBehaviour
	{
		// Doubt I need this (need to copy the sellBehavior from Manufacturer, has unlimited stock so don't need to adjust it. Just need new ontology for it)
		public SellBehaviour(Agent a)
		{
			super(a);
		}

		@Override
		public void action() 
		{

			// Preparing the request message
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

			// Set receiver to Manufacturer Agent
			msg.addReceiver(manufacturerAgent);
			msg.setLanguage(codec.getName());
			msg.setOntology(ontology.getName()); 

			// Order, sets Buyer and what Item they want
			Sell order = new Sell();
			order.setCustomer(manufacturerAgent);
			// need to return PC or take it from list
			//order.setItem(pc);
			// Order details used for queries
			order.setCurrentDay(currentDay);
			order.setDueInDays(dueInDays);
			order.setPrice(totalPrice);

			// Sending Message to Manufacturer
			// IMPORTANT: Set up this way due to FIPA, otherwise we get an exception (crash)
			Action request = new Action();
			request.setAction(order);
			request.setActor(manufacturerAgent); // the agent that you request to perform the action
			try 
			{
				// Output to Console
				System.out.println("PC being sent to Manufacturer...");
				doWait(2000);

				System.out.println("TESTING SUPPLIER SELL BEHAVIOUR: " + customerOrders.get(0));

				doWait(2000);

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

			// Order Complete which means the Day is done for the Supplier Agent
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
		// Need to re-do this behaviour to wait and make it cleaner, not just run here. Run in the TickerWaiter
		@Override
		public void action() 
		{
			//reset totalPrice
			totalPrice = 0;

			// Finished "working" for the Day, relay message to Ticker Agent
			ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
			tick.setContent("done");
			tick.addReceiver(tickerAgent);
			myAgent.send(tick);
			// Remove Behaviour
			myAgent.removeBehaviour(this);

		}

	}
}
