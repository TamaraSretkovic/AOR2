package rs.ac.bg.etf.aor.parser;

public interface Instruction {

	long getAddress();
	
	boolean isTaken();
	
	boolean isConditional();
	
	boolean isBackwardBranch();
	
	boolean isBranch();
	
}
