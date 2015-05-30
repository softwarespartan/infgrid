
public class ProgressBar {
	
	private boolean shouldClear = false;
	private int denominator;
	private int barLength = 0;

	static final int PROGRESSBAR_LENGTH = 20;

	public ProgressBar(int denominator) {
		this.denominator = denominator;
	}

	public void update(int numerator){
		
		if (shouldClear){
			this.clearBar();
		}
		
	    int percent = (int) (((double) numerator / (double) denominator) * 100);

	    String bar = "[";
	    int lines = round((PROGRESSBAR_LENGTH * numerator) / denominator);
	    int blanks = PROGRESSBAR_LENGTH - lines;

	    for (int i = 0; i < lines; i++)
	        bar += "|";

	    for (int i = 0; i < blanks; i++)
	        bar += " ";

	    bar += "] " + percent + "%";

	    System.out.print(bar);
	    
	    this.barLength = bar.length();
	    
	    this.shouldClear = true;
	}

	private int round(double dbl) {
	    int noDecimal = (int) dbl;
	    double decimal = dbl - noDecimal;

	    if (decimal >= 0.5)
	        return noDecimal + 1;
	    else
	        return noDecimal;
	}
	
	private void clearBar(){
		for (int i = 1; i <= this.barLength; i++){
			System.out.print("\b");
		}
	}
	
	public void finish(){
		this.clearBar();
		System.out.println();
	}
}