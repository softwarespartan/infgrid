import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.joda.time.DateTime;
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

public class SSHExecutor extends Thread implements SexecEventListener {

	private Sexec connection = null;
	private BlockingQueue<Job> jobQueue;
	private DateTime date;
	
	private  DateTimeFormatter fmt = DateTimeFormat.forPattern("yDHms");
	
	private BufferedWriter stdout=null;
	private BufferedWriter stderr=null;
	
	private boolean shouldWriteToFile = true;
	
	private Job job = null;

	public SSHExecutor(String hostname, String userName, String password,
					   String rsaKeyPath, String rsaKeyPassphrase,
					   BlockingQueue<Job> jobQueue) throws IPWorksSSHException {

		// init ssh connection object
		this.connection = new Sexec();
		
		// add self as the listener for SSH events
		try {
			this.connection.addSexecEventListener(this);
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}
		
		// generate the RSA SSH certificate
		Certificate cert = new Certificate();
		
		// set the type to RSA
		cert.setStoreType(10);
		
		// set the path to the private key path
		cert.setStore(rsaKeyPath);
		
		// set the passphrase for this key
		cert.setStorePassword(rsaKeyPassphrase);
		
		// assign this cert to the SSH connection
		this.connection.setSSHCert(cert);
		
		// set the authorization mode to public key
		this.connection.setSSHAuthMode(3);
		
		// log in information
		this.connection.setSSHHost(hostname);
		this.connection.setSSHUser(userName);
		this.connection.setSSHPassword(password);

		// where to get the jobs
		this.jobQueue = jobQueue;
		
		// run until completion or error
		this.connection.setTimeout(0);
		
		// test the connection now ...
		//this.connection.setConnected(true);

	}

	public void sendCommand(String cmd) {

		try {

			this.connection.setConnected(true);
			this.connection.execute(cmd);
			this.connection.setConnected(false);

		} catch (IPWorksSSHException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void SSHKeyboardInteractive(SexecSSHKeyboardInteractiveEvent e) {

		System.out.println(e.instructions);

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

		// init the job
		//this.setup(this.job);
		System.out.println("CONNECTED!");
		//System.out.println(e.description);
	}

	@Override
	public void connectionStatus(SexecConnectionStatusEvent e) {

		System.out.println(e.description);
	}

	@Override
	public void disconnected(SexecDisconnectedEvent e) {

		// teardown
		//this.teardown(this.job);
		System.out.println("Disconnecting ...");
		//System.out.println(e.description);
	}

	@Override
	public void error(SexecErrorEvent e) {

		System.out.println(e.description);
	}

	@Override
	public void stderr(SexecStderrEvent e) {
		
		try {
			System.out.write(e.text);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

//		try{
//			if (this.stderr != null){
//				this.stderr.write(new String(e.text));
//			} else {
//				System.out.write(e.text);
//			}
//		} catch (IOException error) {
//			error.printStackTrace();
//		}
	}

	@Override
	public void stdout(SexecStdoutEvent e) {
		
		try {
			System.out.write(e.text);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

//		try{
//			if (this.stdout != null){
//				this.stdout.write(new String(e.text));
//			} else {
//				System.out.write(e.text);
//			}
//		} catch (IOException error) {
//			error.printStackTrace();
//		}

	}
	
	private String getOutFileName(Job job){
		
		String outFileName = "";
		outFileName += this.connection.getSSHHost()+".";
		outFileName += fmt.print(new DateTime())+".";
		outFileName += job.getJobName();
		System.out.println(outFileName);
		return outFileName;
	}
	
	private void initOutputFiles(Job job){
		
		
		
		if (job.outputFilePath != null){
			
			System.out.println(this.getName()+": init output files ... ");
			
			String outFileName = this.getOutFileName(job);
			
			String outFullPath = new File(job.outputFilePath,outFileName+".stdout").getPath();
			String errFullPath = new File(job.outputFilePath,outFileName+".stdout").getPath(); 
			
			// make output file
			try {
			    this.stdout = new BufferedWriter(new FileWriter(outFullPath));
			    this.stderr = new BufferedWriter(new FileWriter(errFullPath));
			    
			} catch (IOException e) {
				System.err.println("ERROR:  Could not open stdout and stderr for job "
									+job.getJobName()+" on "
									+this.connection.getSSHHost());
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
			System.out.println(this.getName()+": closing ouputfiles ...");
			try {
				this.stdout.close();
				this.stderr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void setup(Job job){
		this.initOutputFiles(job);
	}
	
	private void teardown(Job job){
		this.closeOutputFiles();
		this.shouldWriteToFile = true;
	}
	
	public void run(){
		while(true){
			try {
				
				// wait for new job
				this.job = this.jobQueue.take();
				
				// execute the job
				//System.out.println(job.getJob());
				System.out.println(this.getName()+" is taking a job ...");
				
				this.sendCommand(this.job.getJob());
				
			} catch (InterruptedException e) {
				e.printStackTrace();
				try {
					this.connection.SSHLogoff();
					return;
				} catch (IPWorksSSHException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		Job job;
		int numJobs = 5;
		String theJob = "hostname -f";
		BlockingQueue<Job> jobs = new LinkedBlockingQueue<Job>();
		
		for (int i = 1; i <= numJobs; i ++ ){
			try {
				job = new Job(theJob);
				//job.outputFilePath = "./";
				jobs.put(job);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		try {
			new SSHExecutor("marlin.geology.ohio-state.edu", "root", "4Ku-u!a",
							"/Users/abelbrown/.ssh/id_rsa","",
							jobs).start();
		} catch (IPWorksSSHException e) {
			e.printStackTrace();
		}
		//new SSHExecutor("marlin.geology.ohio-state.edu", "root", "4Ku-u!a",jobs).start();
	}

}
