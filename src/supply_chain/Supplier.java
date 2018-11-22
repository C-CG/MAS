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
import jade.core.AID;

public class Supplier extends Agent 
{
	// AIDs for other Agents
	private AID tickerAgent;
	private AID customerAgent;
	private AID manufacturerAgent;
	
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
					System.out.println("Message Received from Ticker Agent, starting work. - Supplier");
					// Add Behaviour "sellBehaviour"
					myAgent.addBehaviour(new SellBehaviour(myAgent));
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
