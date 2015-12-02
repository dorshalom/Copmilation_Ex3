package slp;

public enum PrimitiveTypesEnum {
	
	VOID("void"),
	INT("int"), 
	BOOLEAN("boolean"), 
	STRING("string");

	public String name;

	private PrimitiveTypesEnum(String n) {
		name = n;
	}
}


