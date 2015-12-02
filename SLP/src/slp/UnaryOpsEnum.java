package slp;

public enum UnaryOpsEnum {	
	UMINUS("-", "Mathematical"),
	LNEG("!", "Logical");
		
	public final String name, type;
	
	private UnaryOpsEnum(String name, String type) {
		this.name = name;
		this.type = type;
	}
}