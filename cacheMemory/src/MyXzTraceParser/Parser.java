package rs.ac.bg.etf.aor.parser;

public interface Parser {

	public boolean hasNext();
	
	public Instruction getNext();
	
}
