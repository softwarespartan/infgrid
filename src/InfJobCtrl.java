

import ipworks.IPWorksException;

import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Used to SUBMIT, DELETE, DISPLAY jobs and related info
 * 
 */

public class InfJobCtrl implements Runnable{

	final int port = 50004;
	
	// where to send the parsed data requests
	private final Set<Node> nodeSet;
	
	// the obj that does the heavy lifting
	private final BlockingQueue<Job> jobQueue;
	
	private final JobXmlParser jobParser;
	
	// control bool
	private boolean shouldQuit = false;
	
	// alt default server
	public InfJobCtrl(Set<Node> nodeSet, BlockingQueue<Job> jobQueue){
		this.nodeSet   = nodeSet;
		this.jobQueue  = jobQueue;
		this.jobParser = new JobXmlParser();
	}

	public void run() {
		
		// listen for connections on port
		ServerSocket server = null;

		try {
			
			//initialize the server socket to listen on port 
			server = new ServerSocket(port);

			// the connection
			Socket connection = null;
			
			BufferedReader networkIn = null;

			// do this forever
			while (true){

				try{

					// blocks until gets a poke
					connection = server.accept();

					// get the connection output stream
					Writer out = new OutputStreamWriter(connection.getOutputStream());

					
					// fully initialize the network input from new socket
					networkIn = new BufferedReader(
									new InputStreamReader(
											connection.getInputStream() ) );
						
					// create new string buffer for the xml data
					StringBuffer xmlData = new StringBuffer();
					
					// integer holds a byte's worth of data
					int c;
					
					// read the input stream until nothing left (i.e returns -1)
					// read the data one byte at a time ...
					while((c = networkIn.read()) != -1){
						// append any characters (bytes) to the string buffer
						xmlData.append((char) c);
						
						// check the pipe status
						if (!networkIn.ready()) break;
					}
					
					// make a string outta the string buffer
					String xmlDataString = xmlData.toString().trim();
					
					
					// check for job submission ... 
					if (xmlDataString.startsWith("<jobSpecification")
							&& xmlDataString.endsWith("</jobSpecification>")){
						                               
						this.handleJobSpecification(xmlDataString);
					}
					
					if (xmlDataString.startsWith("printJobQueue")){
						out.write(this.getJobQueueString());
					}
					
					if (xmlDataString.startsWith("nodeStats")){
						out.write(this.getGridStats() +"\n"+ this.getRunningJobsString());
					}
					
					// clear this bitch out ...
					out.flush();
					
					// that's all folks
					connection.close();
					
				} catch (IOException ex){
					
					//nothing to do about it really
				
				} finally {

					if (connection != null)
						try {
							connection.close();
						} catch (IOException e) {
							//e.printStackTrace();
						}
				}	
			}
		}catch (IOException e1) {
			System.err.println("Could not listen for connections on port: "+port);
			e1.printStackTrace();
		} finally {
			if (server != null){
				try {
					server.close();
				} catch (IOException e) {
					System.err.println("Error closing the server listing on port: "+port);
					e.printStackTrace();
				}
			}
		}
	}

	private void handleJobSpecification(String xmlData){
				
		try {
			
			// parse the xml data using job parser
			this.jobParser.parse(xmlData);
			
		} catch (IPWorksException e) {
			System.err.println("Error parsing job xml data!");
			return;
		}
			
		// if the job queue is not null submit the jobs
		if (this.jobQueue != null){
			
			// submit the jobs one by one ...
			for (Job j : this.jobParser.jobList){	
				 
				try {
					
					// put the jobs in the queue, sleeping if ness.
					this.jobQueue.put(j);
					
				} catch (InterruptedException e) {
					
					// dump ... whateves ... tough luck ...
					e.printStackTrace();
					System.err.println("Error submitting job(s) ...");
					return;
				}
			}
		}		
	}
	
	private String getJobQueueString(){
		
		String jobQueueString = "";
		
		int i = 1;
		for (Job j : this.jobQueue){
			jobQueueString += i+". "+j.getJobName()+"\n";
			i++;
		}
		
		return jobQueueString;
	}
	
	private String getRunningJobsString(){
		
		String runningJobsString = "";
		for (Node n : this.nodeSet){
			runningJobsString += n.getRunningJobsString()+"\n";
		}
		
		return runningJobsString;
	}
	
	private String getGridStats(){
		
		int nexe = 0;
		int nP   = 0;
		
		for (Node n: this.nodeSet){
			nexe += n.getNumberOfRunningJobs();
			nP   += n.getMaxNumberOfJobs();
		}
		
		String gridStats = this.nodeSet.size() + " node(s), ";
		gridStats += nP + " processors, ";
		gridStats += nexe + " jobs executing, ";
		gridStats += this.jobQueue.size() + " jobs pending";
		gridStats += "\n";
		
		return gridStats;
	}
	
	public static void main(String[] args) {
		
		BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<Job>();
		(new Thread (new InfJobCtrl(null,jobQueue))).start();
	}
}

