import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.Seconds;


public class Job {
	
	public String cmd = null;
	public List<String> argList;
	public String outputFilePath = "/media/fugu/infOutput/";
	public String jobName = null;
	public String batchName = null;
	
	public int exitStatus;
	public String errorMsg;
	
	public DateTime creationTime = null;
	public DateTime startTime    = null;
	public DateTime stopTime     = null;
	
	// public dateTime timeout;
	 
	public Job(String cmd){
		this.creationTime = new DateTime();
		this.cmd = cmd;
	}
	
	public Job(){
		this.creationTime = new DateTime();
		this.cmd = null;
		this.argList = new ArrayList<String>();
	}
	
	public Job(String cmd, List<String> argList){
		
		this.creationTime = new DateTime();
		
		this.cmd = cmd;
		
		this.argList = new ArrayList<String>();
		this.argList = argList;
	}
	
	public Job(String cmd, List<String> argList,String outputFilePath){
		
		this.creationTime = new DateTime();
		
		this.cmd = cmd;
		
		this.argList = new ArrayList<String>();
		this.argList = argList;
		
		this.outputFilePath = outputFilePath;
	}
	
	public Job(String cmd, List<String> argList,String outputFilePath, String jobName){
		
		this.creationTime = new DateTime();
		
		this.cmd = cmd;
		
		this.argList = new ArrayList<String>();
		this.argList = argList;
		
		this.outputFilePath = outputFilePath;
	
		this.jobName = jobName;
	}

	public String getJob(){
		
		if (this.cmd == null){
			return "";
		}
		
		String jobString = this.cmd;
		if (this.argList != null){
			Iterator<String> iter = this.argList.iterator();
			while(iter.hasNext()){
				jobString += " "+ iter.next();
			}
		}
		
		return jobString;
	}
	
	public void addArg(String arg){
		this.argList.add(arg);
	}
	
	public void print(){
		System.out.println(this.getJob());
	}
	
	public String getJobName(){
		if (this.jobName != null){
			return this.jobName;
		} else {
			return this.cmd;
		}
	}
	
	public double getExecutionTime(){
		
		// if for some reason the start or stop times are null
		// there's nothing to update
		if (this.startTime == null){
			return 0.0;
		}
		
		// figure out if the job is in finished state
		// or should use current 
		DateTime endTime;
		if (this.stopTime == null){
			// if stop time is null just use current time
			endTime = new DateTime();
		} else {
			endTime = this.stopTime;
		}
		
		// figure out how long the job executed for from startTime to stopTime
		// compute both minutes and seconds
		double numMinutes = Minutes.minutesBetween(this.startTime, endTime).getMinutes();
		double numSeconds = Seconds.secondsBetween(this.startTime, endTime).getSeconds();	
		
		// return either mins or fraction there of ...
		if (numSeconds >= 60){
			return numMinutes;
		} else{
			return numSeconds/60;
		}
	}
	
	public static void main(String[] args) {

	}

}
