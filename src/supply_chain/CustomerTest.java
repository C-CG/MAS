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
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import supply_chain.Manufacturer.BuyBehaviour;
import jade.core.AID;

//Importing Ontology/Elements
import supply_chain_ontology.SupplyChainOntology;
import supply_chain_ontology.elements.*;

public class CustomerTest extends Agent 
{
	// Look into what the "codec" does
	private Codec codec = new SLCodec();
	private Ontology ontology = SupplyChainOntology.getInstance();
	
	// AID of Manufacturer
	private AID manufacturerAgent;
	private AID tickerAgent;
	
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
					System.out.println("Message Received from Ticker Agent, starting work. - Customer");
					myAgent.addBehaviour(new BuyBehaviour(myAgent));
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

			// Preparing order of PC (this is where the random algorithm would go, static for now)
			PC pc = new PC();
			pc.setName("Desktop");
			// Need to make this increment per day (static atm)
			pc.setOrderNumber(0001);

			// Order, sets Buyer and what Item they want
			Sell order = new Sell();
			order.setCustomer(myAgent.getAID());
			order.setItem(pc);

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
			
		}
		
	}
}
