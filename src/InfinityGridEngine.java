 import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class InfinityGridEngine {
	
	public static void main(String[] args) {
		
		// init job queue and node set
		BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<Job>();
		Set<Node> nodeSet           = new NodeXmlParser("./nodes.xml",jobQueue).getNodes();
		
		// start job server
		InfJobCtrl ijc   = new InfJobCtrl(nodeSet,jobQueue);
		Thread jobServer = new Thread (ijc);
		jobServer.start();
		
		// do it ..
		System.out.println("Please wait ...");
		for (Node n: nodeSet){
			n.connect();
		}
		
		System.out.println("Ready for jobs ...");
	}
}
