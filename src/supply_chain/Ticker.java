package supply_chain;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;

public class Ticker extends Agent 
{
	// Number of Simulated Days
	public static final int DAYS = 90;

	// Method called when the Agent is launched
	protected void setup()
	{
		// Register the Ticker Agent in the Directory
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());

		// Registers what "service" the Agent will offer
		ServiceDescription sd = new ServiceDescription();
		sd.setType("ticker");
		sd.setName("ticker-agent");

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

		// Waiting for other Agents to start (buffer)
		doWait(5000);
		// Add Behaviour to Sync Agents
		addBehaviour(new SyncAgents(this));
	}

	public class SyncAgents extends Behaviour 
	{
		private int day = 1;
		private int step = 0;
		private int finishedAgents = 0;

		// Array to store both the Agents in the Simulation/Number of Agents
		private ArrayList<AID> simulationAgents = new ArrayList<>();
		/**
		 * @param a	the agent executing the behaviour
		 */
		public SyncAgents(Agent a) {
			super(a);
		}


		@Override
		public void action() 
		{
			switch(step)
			{
			case 0:

				// Find all the Agents using the Directory Service (customer)
				DFAgentDescription template1 = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("customer");
				template1.addServices(sd);
				
				// Find all the Agents using the Directory Service (Manufacturer)
				DFAgentDescription template2 = new DFAgentDescription();
				ServiceDescription sd2 = new ServiceDescription();
				sd2.setType("manufacturer");
				template2.addServices(sd2);
				
				// Find all the Agents using the Directory Service (Manufacturer)
				DFAgentDescription template3 = new DFAgentDescription();
				ServiceDescription sd3 = new ServiceDescription();
				sd3.setType("supplier");
				template3.addServices(sd3);

				try
				{
					DFAgentDescription[] agentsType1 = DFService.search(myAgent, template1);
					for (int i=0; i<agentsType1.length; i++)
					{
						simulationAgents.add(agentsType1[i].getName());
					}

					DFAgentDescription[] agentsType2 = DFService.search(myAgent, template2);
					for (int i=0; i<agentsType2.length; i++)
					{
						simulationAgents.add(agentsType2[i].getName());
					}
					
					// Supplier Agent, not Receiving Message (REQUEST) from Manufacturer. Need to work on Ontology to fix this
					
					DFAgentDescription[] agentsType3 = DFService.search(myAgent, template3);
					for (int i=0; i<agentsType3.length; i++)
					{
						simulationAgents.add(agentsType3[i].getName());
					}
					
				}
				
				catch(FIPAException e)
				{
					e.printStackTrace();
				}

				// Outside of Try/Catch, send "new day" message to all agents (in this case only manufacturer for now)
				ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
				// Set the contents of the Message
				tick.setContent("new day");

				for(AID id : simulationAgents)
				{
					tick.addReceiver(id);
				}

				// Updates for the day/message sent out
				System.out.println("Day: " + day);
				myAgent.send(tick);
				day++;
				step++;
				break;

			case 1:
				// Waiting for confirmation message ("done") to signify work has been complete for the day.
				MessageTemplate mt = MessageTemplate.MatchContent("done");
				ACLMessage msg = myAgent.receive(mt);

				if(msg != null)
				{

					finishedAgents++;
					// If Statement to check that all Agents have finished for the day
					if (finishedAgents >= simulationAgents.size())
					{
						step++;
					}
				}
				else 
				{
					block();
				}
			}

		}

		@Override
		public boolean done() 
		{
			return step == 2;
		}

		@Override
		public void reset()
		{
			super.reset();
			simulationAgents.clear();
			step = 0;
			finishedAgents = 0;
		}

		@Override
		public int onEnd()
		{
			if(day == DAYS)
			{
				myAgent.doDelete();
			}
			else
			{
				//Reset
				reset();
				myAgent.addBehaviour(this);
			}
			return 0;
		}
	}
}
