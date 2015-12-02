package slp;

public enum LiteralsEnum {

	INTEGER("integer"), 
	QUOTE("quote"),
	TRUE("true"),
	FALSE("false"),
	NULL("null");

	public final String name;

	private LiteralsEnum(String name) {
		this.name = name;
	}
}