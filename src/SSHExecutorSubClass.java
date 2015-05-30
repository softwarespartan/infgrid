import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.Timer;
import java.util.TimerTask;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ipworksssh.Certificate;
import ipworksssh.IPWorksSSHException;
import ipworksssh.Sexec;
import ipworksssh.SexecConnectedEvent;
import ipworksssh.SexecConnectionStatusEvent;
import ipworksssh.SexecDisconnectedEvent;
import ipworksssh.SexecErrorEvent;
import ipworksssh.SexecEventListener;
import ipworksssh.SexecSSHKeyboardInteractiveEvent;
import ipworksssh.SexecSSHServerAuthenticationEvent;
import ipworksssh.SexecSSHStatusEvent;
import ipworksssh.SexecStderrEvent;
import ipworksssh.SexecStdoutEvent;

import java.util.UUID;

public class SSHExecutorSubClass extends Sexec implements SexecEventListener, Runnable{
	
	private static final long serialVersionUID = 6129010632588650601L;
	private final UUID uuid = UUID.randomUUID();
	
	private static final Semaphore commandLock      = new Semaphore(1);
	private static final Semaphore connectionLock   = new Semaphore(1);
	private static final Timer     commandLockReleaseTimer = new Timer();
	
	private static final TimerTask commandLockRelease = new TimerTask(){
		public void run(){
			SSHExecutorSubClass.commandLock.release();
		}
	};
	
	
	private final BlockingQueue<Job> jobQueue;
	
	private  DateTimeFormatter fmt = DateTimeFormat.forPattern("y-D-H:mm:ss.SSS");
	
	private BufferedWriter stdout=null;
	private BufferedWriter stderr=null;
	
	private boolean shouldWriteToFile = true;
	private boolean shouldWriteStatus = true;
	
	// keep track for status msgs
	private Job job = null;
	
	// control var ..
	private boolean isWorkingOnJob = false;
	
	// soft termination 
	public boolean shouldQuit = false;
	
	// once the thread is done sets true
	private boolean isFinished  = false;
	
	// meta data
	private int numberOfJobsExecuted = 0;
	
	// average run time in minutes 
	private double averageJobExecutionTimeInMins = 0;
	
	public SSHExecutorSubClass(String hostname,
							   String username,
							   String password,
							   String rsaKeyPath,
							   String rsaKeyPassphrase,
							   BlockingQueue<Job> jobQueue
							  ) throws IPWorksSSHException{
		super();
		
		// set the lic
		this.setRuntimeLicense(IPWorksLicense.SSHLicense);
		
		try {
			this.addSexecEventListener(this);
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}
		
		// OK, basics
		this.setSSHHost(hostname);
		this.setSSHUser(username);
		this.setSSHPassword(password);
		
		if (rsaKeyPath != null){
						
			// create new certificate using the rsa key
			Certificate cert = new Certificate();
			
			// set the type of key to rsa
			cert.setStoreType(10);
			
			// set the path to the rsa key
			cert.setStore(rsaKeyPath);
			
			// set the passphrase for the key
			cert.setStorePassword(rsaKeyPassphrase);
			
			// associate the key with the connection
			this.setSSHCert(cert);
			
			// tell the connection to use the rsa key
			this.setSSHAuthMode(3);
		}
		
		// set the job queue to get jobs from
		this.jobQueue = jobQueue;
		
		// wait for long time for connection
		this.setTimeout(999999);
	}
	
	public String getJobName(){
		
		if (this.job == null || this.job.startTime == null || !this.isWorkingOnJob){
			return "";
		}
		return this.job.getJobName();
	}
	
	public boolean isWorking(){
		return this.isWorkingOnJob;
	}
	
	public String getCurrentJobRunTime(){
		
		if (this.job == null || ! this.isWorkingOnJob){
			return "";
		} else {
			return String.format("%2.1f", this.job.getExecutionTime())+" mins";
		}
	}
		
 	public int sendCommand(String cmd) throws IPWorksSSHException{ 
		
		// TRAP AND RETHROW ANY ERRORS HERE
		
// 		try {
// 			
// 			// acquire the connection lock to start an ssh tunnel
//			SSHExecutorSubClass.connectionLock.acquire();
//			
//			// once we have the lock then init the tunnel
//			this.setConnected(true);
//		
// 		} catch (InterruptedException e) {
//			
// 			// um ... problems
// 			e.printStackTrace();
//		
// 		}finally{
// 			
// 			// no matter what release this bitch
// 			// make sure this release is in same 
// 			// try-catch-finally block as acquire
// 			SSHExecutorSubClass.connectionLock.release();
// 		}
		
 		try {
 			
 			// get the lock to start command
 			SSHExecutorSubClass.commandLock.acquire();
 			
 			// initialize connection to target
 			if (! this.isConnected()){ this.setConnected(true); }
 			
 			// setup to release this lock in 1 seconds
 			SSHExecutorSubClass.commandLockReleaseTimer
 				.schedule(
 						new TimerTask(){
		 					public void run(){
		 						SSHExecutorSubClass.commandLock.release();
		 					}
 						}
 				, 100);
 			
	 		// start some work
			this.setCommand(cmd);
			
 		} catch (InterruptedException e){
 			
 			// que es la problema mujer?
 			e.printStackTrace();
 		
 		} finally {
 			
 			// don't release commandLock here
 			// will be done by timer 
 		}
 		
//		try {
//		
//			// acquire the connection lock to tear down ssh tunnel
//			SSHExecutorSubClass.connectionLock.acquire();
//			
//			// close down the tunnel
//			this.setConnected(false);
//		
//		} catch (InterruptedException e) {
//			
//			// problems ...
//			e.printStackTrace();
//			
//		}finally{
//			
//			// don't forget to release this 
// 			SSHExecutorSubClass.connectionLock.release();
// 		}
		
		// return the exit status from the *possibly* executed command 
		return this.getExitStatus();
	}
	
	public int getNumberOfJobsExecuted(){ 
		return this.numberOfJobsExecuted;
	}
	
	public double getAvgerageJobExecutionTime(){
		return this.averageJobExecutionTimeInMins;
	}
	
	private void updateAverageJobExecutionTime(Job someJob){
		
		// update average ...
		this.averageJobExecutionTimeInMins = (someJob.getExecutionTime() 
				+ this.numberOfJobsExecuted*this.averageJobExecutionTimeInMins)/(this.numberOfJobsExecuted+1);
	}
	
	private void updateStats(Job someJob){
		
		// update the avg running time for jobs
		this.updateAverageJobExecutionTime(someJob);
		
		// update the number of jobs executed
		this.numberOfJobsExecuted +=1;
	}
	
	public int getNumberOfFinishedJobs(){
		return this.numberOfJobsExecuted;
	}
	
	private String getOutFileName(Job job){
		
		String outFileName = "";
		outFileName += this.getSSHHost()+".";
		outFileName += fmt.print(new DateTime());
		outFileName += "."+job.getJobName();
		//outFileName += "."+this.uuid.toString();
		//System.out.println(outFileName);
		return outFileName;
	}
	
	private void initOutputFiles(Job job){
		
		if (job.outputFilePath != null){
			
			//System.out.println("init output files ... ");
			
			String outFileName = this.getOutFileName(job);
			
			String outFullPath = new File(job.outputFilePath,outFileName+".stdout").getPath();
			String errFullPath = new File(job.outputFilePath,outFileName+".stdout").getPath(); 
			
			// make output file
			try {
			    this.stdout = new BufferedWriter(new FileWriter(outFullPath));
			    this.stderr = new BufferedWriter(new FileWriter(errFullPath));
			    
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("ERROR:  Could not open stdout and stderr for job "
									+job.getJobName()+" on "
									+this.getSSHHost());
				this.stdout = null;
				this.stderr = null;
				this.shouldWriteToFile = false;
			}
			
		} else {
			this.stdout = null;
			this.stderr = null;
		}
		
	}
	
	private void closeOutputFiles(){
		// close the files if open
		if (this.stdout != null && this.stderr != null){
			//System.out.println("closing ouputfiles ...");
			try {
				this.stdout.close();
				this.stderr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		
	}

	@Override
	public void SSHKeyboardInteractive(SexecSSHKeyboardInteractiveEvent e) {
		e.response=this.getSSHPassword();
	}

	@Override
	public void SSHServerAuthentication(SexecSSHServerAuthenticationEvent e) {
		e.accept = true;
	}

	@Override
	public void SSHStatus(SexecSSHStatusEvent e) {
		//System.out.println(e.message);
	}

	@Override
	public void connected(SexecConnectedEvent e) {
		//System.out.println(e.description);
		if (this.shouldWriteStatus){
			System.out.println(fmt.print(new DateTime())+" - "+this.getSSHHost()+" connected ...");
		}
	}

	@Override
	public void connectionStatus(SexecConnectionStatusEvent e) {
		if (this.shouldWriteStatus){
			System.out.println(this.getSSHHost()+": "+e.description);
		}
	}

	@Override
	public void disconnected(SexecDisconnectedEvent e) {
		//System.out.println(e.description);
		if(this.shouldWriteStatus){
			System.out.println(fmt.print(new DateTime())+" - "+this.getSSHHost()+" disconnected ...");
		}
	}

	@Override
	public void error(SexecErrorEvent e) {
		System.out.println(this.getSSHHost()+": "+e.description);
	}
	
	@Override
	public void stderr(SexecStderrEvent e) {

		try{
			if (this.stderr != null){
				this.stderr.write(new String(e.text));
			} else {
				System.out.write(e.text);
			}
		} catch (IOException error) {
			error.printStackTrace();
		}
	}

	@Override
	public void stdout(SexecStdoutEvent e) {

		try{
			if (this.stdout != null){
				this.stdout.write(new String(e.text));
			} else {
				// check for quiet mode 
				if(this.shouldWriteStatus){
					System.out.write(e.text);
				}
			}
		} catch (IOException error) {
			error.printStackTrace();
		}
	}

	
	public boolean test(){
		
		this.shouldWriteStatus = false;
		try {
			this.sendCommand("hostname");
		} catch (IPWorksSSHException e) {
			e.printStackTrace();
		}
		this.shouldWriteStatus = true;
		
		if (this.getExitStatus()==0){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void run() {
		
		// loop forever ...
		while(!shouldQuit){
			
			// get a job from the job queue
			try {
				this.job = this.jobQueue.take();
				
			} catch (InterruptedException e) {
				
				// if there's a problem just exit
				e.printStackTrace();
				return;
			}
			
			// got a job, now init stdout files for job
			this.initOutputFiles(this.job);
			
			// send the job to the host
			try {
				
				this.isWorkingOnJob = true;
				
				// set the start time for execution
				this.job.startTime = new DateTime();
				
				// do the damn thing ...
				this.sendCommand(this.job.getJob());
				
				// set the stop time
				this.job.stopTime = new DateTime();
				
				// update avg run time and numJobsExecuted
				this.updateStats(this.job);
				
				// comfort signal
				System.out.println(fmt.print(new DateTime())+" - "
			   			+ this.getSSHHost()
			   			+": finished "
			   			+this.job.getJobName()
			   			+" in " + this.job.getExecutionTime()+ " mins");
				
				// that's all folks
				this.job = null;
				this.isWorkingOnJob = false;
				
			} catch (IPWorksSSHException e) {
				
				// if there is a problem with job execution
				// just print the error
				// requeue the job
				// and exit host thread. 
				e.printStackTrace();
				this.isWorkingOnJob = false;
				this.job = null;
				
				// Requeue the job
				try {
					
					// put job back in the job queue
					this.jobQueue.put(this.job);
					this.isWorkingOnJob = false;
					return;
					
				} catch (InterruptedException e1) {
					
					// could not put the job back in the job queue
					e1.printStackTrace();
					this.isWorkingOnJob = false;
					return;
				}
				
			} finally {
				// no matter what, close the output files ...
				this.closeOutputFiles();
				this.isWorkingOnJob = false;
			}
			
			this.isWorkingOnJob = false;
			this.job = null;
		}
	}
}