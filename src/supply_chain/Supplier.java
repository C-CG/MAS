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
import supply_chain.Manufacturer.DayComplete;
import supply_chain.Manufacturer.TickerWaiter;
import supply_chain.ManufacturerTest.BuyBehaviour;
import supply_chain_ontology.SupplyChainOntology;
import supply_chain_ontology.elements.Item;
import supply_chain_ontology.elements.PC;
import supply_chain_ontology.elements.Sell;
import jade.core.AID;

public class Supplier extends Agent 
{
	// Look into what the "codec" does
	private Codec codec = new SLCodec();
	private Ontology ontology = SupplyChainOntology.getInstance();

	// AIDs for other Agents
	private AID tickerAgent;
	private AID customerAgent;
	private AID manufacturerAgent;
	
	int totalPrice = 0;

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
							
							

							// Printing PC name to demo Ontology
							if(it instanceof PC)
							{
								PC pc = (PC)it;

								System.out.println("Supplier Received Manufacturer Order: " + pc.getOrderNumber() + " [ " + pc.getName() + " ]");
								
								// Testing output of details
								System.out.println("Testing Order Details Extraction from Order: " + order.getDetails());
								
								
								doWait(2000);
								
								// Move this into it's own function (testing right now)
								
								int motherboard = 0;
								int cpu = 0;
								int ram = 0;
								int hdd = 0;
								int screen = 0;
								int os = 0;
								int delivery = 1;
								
								
								
								// Only testing Supplier 1
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
								
								System.out.println("Supplier 1: " + delivery + " Day/s " +  "Price: " + "£" + totalPrice);
								
								myAgent.addBehaviour(new DayComplete(myAgent));

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



	private class SellBehaviour extends CyclicBehaviour
	{
		// Doubt I need this (need to copy the sellBehavior from Manufacturer, has unlimited stock so don't need to adjust it. Just need new ontology for it)
		public SellBehaviour(Agent a)
		{
			super(a);
		}

		@Override
		public void action() {

			// Responds to Customer REQUEST messages only
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = receive(mt);

			if (msg !=null)
			{
				// Checking if the message received states a "order", if so do work.

				// Add Behaviours (instead of this print, we need the actual order)
				System.out.println("Supplier Received Order.");
				// Add Behaviour "dayComplete behaviour"
				myAgent.addBehaviour(new DayComplete(myAgent));
			}
			else
			{
				block();
			}

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
