import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ipworks.IPWorksException;
import ipworks.Xmlp;
import ipworks.XmlpCharactersEvent;
import ipworks.XmlpCommentEvent;
import ipworks.XmlpEndElementEvent;
import ipworks.XmlpEndPrefixMappingEvent;
import ipworks.XmlpErrorEvent;
import ipworks.XmlpEvalEntityEvent;
import ipworks.XmlpEventListener;
import ipworks.XmlpIgnorableWhitespaceEvent;
import ipworks.XmlpMetaEvent;
import ipworks.XmlpPIEvent;
import ipworks.XmlpSpecialSectionEvent;
import ipworks.XmlpStartElementEvent;
import ipworks.XmlpStartPrefixMappingEvent;


public class NodeXmlParser implements XmlpEventListener {

	private final Xmlp xmlParser = new Xmlp();
	
	private final BlockingQueue<Job> jobQueue;
	
	private final String xmlFile;
	
	private Set<Node> nodeSet;
	
	private HashMap<String,String> globalValues;
	
	private Set<String> hostnameSet;
	
	private boolean isGlobal         = false;
	private String  currentElement   = "";
	private String  username         = null;
	private String  password         = null;
	private String  rsaKeyPath       = null;
	private String  rsaKeyPassphrase = null;
	private int     maxNumJobs       = 1;
	private boolean isActive         = false;
	
	
	public NodeXmlParser(String xmlFile, BlockingQueue<Job> jobQueue){
		
		// set the file to parse
		this.xmlFile = xmlFile;
		this.jobQueue = jobQueue;
		this.nodeSet = new HashSet<Node>();
		this.globalValues = new HashMap<String,String>();
		this.hostnameSet  = new HashSet<String>();

		try {

			// tell the parser not to validate the input
			try {
				this.xmlParser.setValidate(false);
				this.xmlParser.setRuntimeLicense(IPWorksLicense.IPWorksLicense);
				//System.out.println(this.xmlParser.getRuntimeLicense());
			} catch (IPWorksException e) {
				e.printStackTrace();
			}

			// add ourselves to be notified of events
			// i.e. while parsing the file
			this.xmlParser.addXmlpEventListener(this);

		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}
		
		
		// PARSE THE FILE!!
		try {
			this.xmlParser.parseFile(this.xmlFile);
		} catch (IPWorksException e) {
			e.printStackTrace();
		}
	}
	
	public Set<Node> getNodes(){
		return this.nodeSet;
	}
	
	private void resetNodeValues(){
		
		this.isActive = true;
		
		// clear hostnames
		this.hostnameSet.clear();
		
		// in general look for global value first
		if (this.globalValues.containsKey("username")){
			this.username = this.globalValues.get("username");
		}else{
			this.username = null;
		}
		
		if (this.globalValues.containsKey("password")){
			this.password = this.globalValues.get("password");
		}else{
			this.password = null;
		}
		
		if (this.globalValues.containsKey("rsaKeyPath")){
			this.rsaKeyPath = this.globalValues.get("rsaKeyPath");
		} else {
			this.rsaKeyPath = null;
		}
		
		if (this.globalValues.containsKey("rsaKeyPassphrase")){
			this.rsaKeyPassphrase = this.globalValues.get("rsaKeyPassphrase");
		} else {
			this.rsaKeyPassphrase = null;
		}
		
		if (this.globalValues.containsKey("maxNumJobs")){
			this.maxNumJobs = Integer.parseInt(this.globalValues.get("maxNumJobs"));
		} else {
			this.maxNumJobs = 1;
		}
		
	}
	
	@Override
	public void PI(XmlpPIEvent event) {		
	}

	@Override
	public void characters(XmlpCharactersEvent event) {
		
		String theString = new String(event.text);

		// if global value add to the set 
		// but do not add hostnames since global host names don't make sense.
		if (this.isGlobal && !this.currentElement.equals("hostname")){
			
			this.globalValues.put(this.currentElement, theString);
		
		} else if (this.currentElement.equals("hostname")){
			
			this.hostnameSet.add(theString);
			
		} else if (this.currentElement.equals("username")){
			
			this.username = theString;
			
		} else if (this.currentElement.equals("password")) {
			
			this.password = theString;
			
		} else if (this.currentElement.equals("rsaKeyPath")){
			
			this.rsaKeyPath = theString;
			
		} else if (this.currentElement.equals("rsaKeyPassphrase")){
			
			this.rsaKeyPassphrase = theString;
			
		} else if (this.currentElement.equals("maxNumJobs")){
			
			this.maxNumJobs = Integer.parseInt(theString);
			
		}
		
//		if (this.isGlobal){
//			System.out.println(this.currentElement+":"+theString+ " (GLOBAL)");
//		} else {
//			System.out.println(this.currentElement+":"+theString);
//		}
		
	}

	@Override
	public void comment(XmlpCommentEvent event) {		
	}

	@Override
	public void endElement(XmlpEndElementEvent event) {
		
		if (event.element.equalsIgnoreCase("GLOBAL")){
			this.isGlobal = false;
		}	
		
		if (event.element.equalsIgnoreCase("node")){
			
			// try to make nodes based on info collected.
			
			// essential info needed:
			//   - at least one hostname
			//   - username
			//   - password
			//&& this.password != null
			if (this.isActive 
					&& !this.hostnameSet.isEmpty() 
						&& this.username != null 
							){
				
				for (String hostname: hostnameSet){
					
					/*
					System.out.println("NODE:");
					System.out.println("Hostname: "+               hostname        );
					System.out.println("Username: "+          this.username        );
					System.out.println("Password: "+          this.password        );
					System.out.println("RSA Key Path: "+      this.rsaKeyPath      );
					System.out.println("RSA Key Passphrase: "+this.rsaKeyPassphrase);
					System.out.println("Max Number of Jobs: "+this.maxNumJobs      );
					System.out.println();
					*/
					this.nodeSet.add(new Node(     hostname,
											  this.username,
											  this.password,
											  this.rsaKeyPath,
											  this.rsaKeyPassphrase,
											  this.maxNumJobs,
											  this.jobQueue));
				}
			}
		}
	}

	@Override
	public void endPrefixMapping(XmlpEndPrefixMappingEvent event) {		
	}

	@Override
	public void error(XmlpErrorEvent event) {		
	}

	@Override
	public void evalEntity(XmlpEvalEntityEvent event) {		
	}

	@Override
	public void ignorableWhitespace(XmlpIgnorableWhitespaceEvent event) {		
	}

	@Override
	public void meta(XmlpMetaEvent event) {		
	}

	@Override
	public void specialSection(XmlpSpecialSectionEvent event) {		
	}

	@Override
	public void startElement(XmlpStartElementEvent event) {	
		
		if (event.element.equalsIgnoreCase("GLOBAL")){
			
			this.isGlobal = true;
		
		} else if (event.element.equalsIgnoreCase("node")){
			
			// looking to start a new node
			this.resetNodeValues();
			
		} else if (event.element.equalsIgnoreCase("inactive")){
			
			this.isActive = false;
			
		}
		
		// set the current element
		this.currentElement = event.element;		
	}

	@Override
	public void startPrefixMapping(XmlpStartPrefixMappingEvent event) {		
	}
	
	public static void main(String[] args) {
		
		int numJobs = 100;
		String theJob = "hostname";
		BlockingQueue<Job> jobs = new LinkedBlockingQueue<Job>();
		
		for (int i = 1; i <= numJobs; i ++ ){
			try {
				jobs.put(new Job(theJob));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Set<Node> nodeSet = new NodeXmlParser("./nodes.xml",jobs).getNodes();
		for (Node n: nodeSet){
			n.connect();
		}
	}

}
