package supply_chain_ontology.elements;
import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Components implements Concept 
{
	// Components Variables
	private String ram;
	private String hardDrive;
	private String osLicense;
	private String cpu;
	private String motherboard;
	private Boolean screen;
	
	@Slot(mandatory = true)
	public String getRam()
	{
		return ram;
	}
	
	public void setRam(String ram)
	{
		this.ram = ram;
	}
	
	@Slot(mandatory = true)
	public String getHD()
	{
		return hardDrive;
	}
	
	public void setHD(String hardDrive)
	{
		this.hardDrive = hardDrive;
	}
	
	@Slot(mandatory = true)
	public String getOS()
	{
		return osLicense;
	}
	
	public void setOS(String osLicense)
	{
		this.osLicense = osLicense;
	}
	
	@Slot(mandatory = true)
	public String getCPU()
	{
		return cpu;
	}
	
	public void setCPU(String cpu)
	{
		this.cpu = cpu;
	}
	
	@Slot(mandatory = true)
	public String getMotherboard()
	{
		return motherboard;
	}
	
	public void setMotherboard(String motherboard)
	{
		this.motherboard = motherboard;
	}
	
	//Not Mandatory as Screen may be used for the PC (If it's a Laptop)
	public Boolean getScreen()
	{
		return screen;
	}
	
	public void setScreen(Boolean screen)
	{
		this.screen = screen;
	}
	
	
}
