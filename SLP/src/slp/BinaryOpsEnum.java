package slp;

public enum BinaryOpsEnum {	
	PLUS("+", "Mathematical"),
	MINUS("-", "Mathematical"),
	MULTIPLY("*", "Mathematical"),
	DIVIDE("/", "Mathematical"),
	MOD("%", "Mathematical"),
	LAND("&&", "Logical"),
	LOR("||", "Logical"),
	LT("<", "Logical"),
	LTE("<=", "Logical"),
	GT(">", "Logical"),
	GTE(">=", "Logical"),
	EQUAL("==", "Logical"),
	NEQUAL("!=", "Logical");	

	public final String name, type;
	
	private BinaryOpsEnum(String name, String type) {
		this.name = name;
		this.type = type;
	}
}