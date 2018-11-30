package supply_chain;
import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class Main {

	public static void main(String[] args) {
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		try{
			ContainerController myContainer = myRuntime.createMainContainer(myProfile);	
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			
			AgentController ManufacturerAgent = myContainer.createNewAgent("Manufacturer", ManufacturerTest.class.getCanonicalName(), null);
			ManufacturerAgent.start();
			
			AgentController CustomerAgent = myContainer.createNewAgent("Customer", Customer.class.getCanonicalName(), null);
			CustomerAgent.start();
			
			AgentController SupplierAgent = myContainer.createNewAgent("Supplier", Supplier.class.getCanonicalName(), null);
			SupplierAgent.start();
			
			AgentController TickerAgent = myContainer.createNewAgent("Ticker", Ticker.class.getCanonicalName(), null);
			TickerAgent.start();
							
		}
		catch(Exception e){
			System.out.println("Exception starting agent: " + e.toString());
		}


	}
}
