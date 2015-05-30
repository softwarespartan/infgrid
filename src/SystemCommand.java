import java.io.*;

public class SystemCommand {
	
	private Process process = null;
	private String  command = null;
	private BufferedReader stdOutput = null;
	private BufferedReader stdError  = null;

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public Process getProcess() {
		return process;
	}
	
	public BufferedReader getStdOutput() {
		return stdOutput;
	}

	public BufferedReader getStdError() {
		return stdError;
	}

	public SystemCommand(String command){
		this.command = command; 
		
		try{
			
			Process p = Runtime.getRuntime().exec(this.command);
			
			this.stdOutput = new BufferedReader(new 
	                InputStreamReader(p.getInputStream()));
	
	        this.stdError = new BufferedReader(new 
	                InputStreamReader(p.getErrorStream()));
	        
		}catch(IOException e){
			System.out.println("System command " + this.command +" failed: ");
            e.printStackTrace();
		}
	}
	
	public SystemCommand(String command, String[] env, String dir){
		this.command = command; 
		
		try{
			
			Process p = Runtime.getRuntime().exec(this.command, 
												  env, 
												  new File(dir));
			
			this.stdOutput = new BufferedReader(new 
	                InputStreamReader(p.getInputStream()));
	
	        this.stdError = new BufferedReader(new 
	                InputStreamReader(p.getErrorStream()));
	        
		}catch(IOException e){
			System.out.println("System command " + this.command +" failed: ");
            e.printStackTrace();
		}
	}
	
	public void printStdOutput(){
		String line;
		try{
			while ((line = this.stdOutput.readLine()) != null) {
	            System.out.println(line);
	        }
		}catch(IOException e){
			System.out.println("Could not print the standard output!!!");
            e.printStackTrace();
		}
	}
	
	public void printStdError(){
		String line;
		try{
			while ((line = this.stdError.readLine()) != null) {
	            System.out.println(line);
	        }
		}catch(IOException e){
			System.out.println("Could not print the standard Error!!!");
            e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		SystemCommand cmd1 = new SystemCommand("whoami");
		cmd1.printStdOutput();
		cmd1.printStdError();
		
		SystemCommand cmd2 = new SystemCommand("ps -A");
		cmd2.printStdOutput();
		cmd2.printStdError();
		
		//SystemCommand perlCmd = new SystemCommand("somePerlscript");
		//SystemCommand pythonCmd = new SystemCommand("somePythonscript");
		
	}
}
