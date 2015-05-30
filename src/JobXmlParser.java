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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class JobXmlParser implements XmlpEventListener{

	private final Xmlp xmlParser = new Xmlp();
	public final List<Job> jobList;
	private HashMap<String,String> globalValues;
	private boolean isGlobal;
	private String currentElement;
	private Job job;
	private int count=1;
	
	public JobXmlParser(){
		
		this.jobList = new ArrayList<Job>();
		this.globalValues = new HashMap<String,String>();
		
		try {

			// tell the parser not to validate the input
			try {
				this.xmlParser.setValidate(false);
				this.xmlParser.setRuntimeLicense(IPWorksLicense.IPWorksLicense);
			} catch (IPWorksException e) {
				e.printStackTrace();
			}

			// add ourselves to be notified of events
			// i.e. while parsing the file
			this.xmlParser.addXmlpEventListener(this);

		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}
		
		
//		// PARSE THE FILE!!
//		try {
//			this.xmlParser.parseFile(this.xmlFile);
//		} catch (IPWorksException e) {
//			e.printStackTrace();
//		}
	}
	
	public void parse (String xmlData) throws IPWorksException{

		// remove any old jobs
		this.jobList.clear();
		
		// parse the data into job objects
		this.xmlParser.input(xmlData);
	}

	@Override
	public void PI(XmlpPIEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void comment(XmlpCommentEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void endPrefixMapping(XmlpEndPrefixMappingEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(XmlpErrorEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void evalEntity(XmlpEvalEntityEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ignorableWhitespace(XmlpIgnorableWhitespaceEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void meta(XmlpMetaEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void specialSection(XmlpSpecialSectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void startPrefixMapping(XmlpStartPrefixMappingEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void characters(XmlpCharactersEvent event) {
		
		String theString = new String(event.text);		
		
		if(this.isGlobal){
			if (this.currentElement.equals("command")){
				this.globalValues.put(this.currentElement, theString);
			}
		} else if (this.currentElement.equalsIgnoreCase("arg")){
			
			this.job.argList.add(theString);
			
		} else if (this.currentElement.equals("command")){
			
			this.job.cmd = theString;
			
		} else if (this.currentElement.equalsIgnoreCase("count")){
			
			this.count = Integer.parseInt(theString);
			
		} else if (this.currentElement.equalsIgnoreCase("name")){
			
			this.job.jobName = theString;
		}
	}

	@Override
	public void endElement(XmlpEndElementEvent event) {
				
		if (event.element.equalsIgnoreCase("GLOBAL")){
			
			this.isGlobal = false;

		} else if (event.element.equalsIgnoreCase("job")){
			
			if (this.job.cmd != null){
				for (int i = 1; i <= this.count; i++){
					this.jobList.add(this.job);
					//this.job.print();
				}
			}
		}
	}

	@Override
	public void startElement(XmlpStartElementEvent event) {
		
		if (event.element.equalsIgnoreCase("GLOBAL")){
			
			this.isGlobal = true;
		
		} else if (event.element.equalsIgnoreCase("Job")){
			
			// looking to start a new job
			this.resetJobValues();
			
		} 
		
		// set the current element
		this.currentElement = event.element;	
	}

	private void resetJobValues(){
		
		this.count = 1;
		
		// reset the current job
		this.job = new Job();
		
		// assign global values if they exists
		if (this.globalValues.containsKey("command")){
			this.job.cmd = this.globalValues.get("command");
		}
	}
	
	public void printJobs(){
		
		// fast iteration
		for (Job j: this.jobList){
			j.print();
		}
	}
	
	public static void main(String[] args) throws IPWorksException {
		
		String xmlData 
			= "<jobSpecification name=\"testJob\"> <job> <name>HostIdentification</name> <command>hostname</command> <arg>-s</arg> <arg>-f</arg></job></jobSpecification>";
		
		JobXmlParser jxp = new JobXmlParser();
		jxp.parse(xmlData);
		jxp.parse(xmlData);
		jxp.printJobs();
		
		//new JobXmlParser("./jobs.xml");
		
//		BlockingQueue<Job> jobs = new JobXmlParser("./jobs.xml").getJobsAsBlockingQueue();
//		System.out.println(jobs.size());
//		Set<Node> nodeSet = new NodeXmlParser("./nodes.xml",jobs).getNodes();
//		for (Node n: nodeSet){
//			n.connect();
//		}
	}

}
