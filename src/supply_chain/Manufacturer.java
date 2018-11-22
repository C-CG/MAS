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
import jade.core.AID;

// Importing Ontology/Elements
import supply_chain_ontology.SupplyChainOntology;
import supply_chain_ontology.elements.*;

public class Manufacturer extends Agent 
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
		
		// Creating a new PC to sell (would be automated in the future, based on Customer's wants)
		/* 
		PC pc = new PC();
		pc.setName("Desktop");
		pc.setOrderNumber(0001);
		*/
		
		// Creating new PC to sell (new Ontology)
		PC pc = new PC();
		pc.setName("Desktop");
		pc.setOrderNumber(0001);
		// Adding the Components
		ArrayList<Components> components = new ArrayList<Components>();
		Components c = new Components();
		c.setRam("8gb");
		c.setHD("1Tb");
		c.setOS("Windows");
		c.setCPU("desktopCPU");
		c.setMotherboard("desktopMotherboard");
		c.setScreen(false);
		components.add(c);
		pc.setComponents(components);
		
		// Adding the PC to the HashMap
		computersForSale.put(pc.getOrderNumber(), pc);
		
		// Testing Output
		System.out.println("Computers for sale: " + pc.toString() + " from: " + getAID().getName());
		
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
		public void action() {
			// Responds to Customer REQUEST messages only
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = receive(mt);
			
			// Message Validation
			if(msg !=null)
			{
				try
				{
					ContentElement ce = null;
					System.out.println("Manufacturer Received Customer Order.");
					
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
								
								System.out.println("PC Sold: " + pc.getName());
								
								
								// Change this to the Buy Behaviour 
								myAgent.addBehaviour(new BuyBehaviour(myAgent));
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
			// Send a Message or Request in here to Supplier Agent
			// Run the DayComplete Behaviour in here
			
			// "Buying" components from Supplier, currently just a message. Need to setup the Ontology
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.addReceiver(supplierAgent);

			System.out.println("Manufacturer Placing Order...");
			doWait(2000);
			myAgent.send(msg);
			// Remove Behaviour
			myAgent.addBehaviour(new DayComplete(myAgent));
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
