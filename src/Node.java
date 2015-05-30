import ipworksssh.IPWorksSSHException;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Node {

	private Thread[] connections;
	
	// log in info
	private final String hostname;
	private final String username;
	private final String password;
	
	// number of processors
	private final int maxNumJobs;
	
	// rsa private/public key pairs
	private final String rsaPrivateKeyPath;
	private final String rsaPrivateKeyPassphrase;
	
	// where the jobs (wild things) are ...
	private final BlockingQueue<Job> jobQueue;
	
	private Set<SSHExecutorSubClass> connectionObjs = new HashSet<SSHExecutorSubClass>();
	
	// number of jobs executed
	//	private java.util.concurrent.atomic.AtomicInteger numberOfJobsExecuted;
	
	public Node(String hostname, 
				String username, 
				String password,
				String rsaPrivateKeyPath, 
			    String rsaPrivateKeyPassphrase,
			    int maxNumJobs,
			    BlockingQueue<Job> jobQueue) {
		
		// login info
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		
		// rsa private/public key info
		this.rsaPrivateKeyPath = rsaPrivateKeyPath;
		this.rsaPrivateKeyPassphrase = rsaPrivateKeyPassphrase;
		
		// set the number of jobs this node can take
		this.maxNumJobs = maxNumJobs;
		
		// allocate space for connections
		this.connections = new Thread[this.maxNumJobs];
		
		// finally set the job queue
		this.jobQueue = jobQueue;

	}
	
	public int connect(){
		int numConnections = 0;
		
		SSHExecutorSubClass s;
		
		System.out.print("Initializing "+this.hostname+": ");
		ProgressBar bar = new ProgressBar(this.maxNumJobs);
		
		//loop through and try to connect
		for (int i = 0; i<this.maxNumJobs; i = i+1){
			
			try {

				// new connection object
				s = new SSHExecutorSubClass(
						  this.hostname,
						  this.username,
						  this.password,
						  this.rsaPrivateKeyPath,
						  this.rsaPrivateKeyPassphrase,
						  this.jobQueue);
				
				if(s.test()){
					bar.update(i+1);
					//System.out.print(String.format("%2d", (i+1))+"/"+String.format("%2d",this.maxNumJobs)+" ");
				} else {
					System.out.println("Connection failed!");
				}
				
				// add the thread ... 
				this.connections[i] = (new Thread (s));
				
				this.connectionObjs.add(s);
				
				// fire up the connection ...
				this.connections[i].start();
				
				numConnections++;
				
			} catch (IPWorksSSHException e) {
				e.printStackTrace();
				return numConnections;
			}
			
		}
		
		
		bar.finish();
		//bar.finish();
		return numConnections;
	}
	
	public int numberOfJobsExecuted(){
		int numberOfJobsExecuted = 0;
		
		Iterator<SSHExecutorSubClass> iter = this.connectionObjs.iterator();
		while(iter.hasNext()){
			numberOfJobsExecuted += iter.next().getNumberOfJobsExecuted();
		}
		
		return numberOfJobsExecuted;
	}
	
	public int getMaxNumberOfJobs(){
		return this.maxNumJobs;
	}
	
	public int getNumberOfRunningJobs(){
		int nexe = 0;
		for(SSHExecutorSubClass c: this.connectionObjs){
			if(c.isWorking()){nexe++;}
		}
		return nexe;
	}
	
	public void shutdown(){
		
		// SOFT SHUTDOWN
		
		// let each executor know they should exit
		Iterator<SSHExecutorSubClass> iter = this.connectionObjs.iterator();
		while(iter.hasNext()) {iter.next().shouldQuit = true;}
	}
	
	public void shutdownNow(){
		
		// HARD SHUTDOWN
		
		// interupt each thread from work or sleep
		for (int i = 0; i < this.connections.length; i++){
			this.connections[i].interrupt();
		}
	}
	
	public String getRunningJobsString(){
		
		String runningJobsString = "";
		
		int i=1;
		
		for (SSHExecutorSubClass c : this.connectionObjs){
			
			runningJobsString += String.format("%2d. %-15s ",i,this.hostname);
			
			if (c.isWorking()){
				runningJobsString += String.format(" %-15s %12s", 
						c.getJobName(),c.getCurrentJobRunTime());
			} else {
				runningJobsString += "empty";
			}
			
			runningJobsString += "\n";
			
			i++;
			
		}
		
		return runningJobsString;
	}
	
	public String getHostname() {
		return hostname;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public int getMaxNumJobs() {
		return this.maxNumJobs;
	}

	public String getRsaPrivateKeyPath() {
		return rsaPrivateKeyPath;
	}

	public String getRsaPrivateKeyPassphrase() {
		return rsaPrivateKeyPassphrase;
	}

	public static void main(String[] args) {
		
		// make a blocking queue with some jobs for testing
		
		int numJobs = 10;
		String theJob = "hostname -f";
		BlockingQueue<Job> jobs = new LinkedBlockingQueue<Job>();
		
		for (int i = 1; i <= numJobs; i ++ ){
			try {
				jobs.put(new Job(theJob));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		new Node("marlin.geology.ohio-state.edu",
				   "root",
				   "4Ku-u!a",
				   "/Users/abelbrown/.ssh/id_rsa",
				   "pbssucks",
				   4,
				   jobs).connect();
		
//		new Node("192.168.0.101",
//				   "abel",
//				   "4u$abel",
//				   "/Users/abelbrown/.ssh/id_rsa",
//				   "",
//				   4,
//				   jobs).connect();
//		
//		new Node("192.168.0.102",
//				   "abel",
//				   "4u$abel",
//				   "/Users/abelbrown/.ssh/id_rsa",
//				   "",
//				   4,
//				   jobs).connect();
	}
}
