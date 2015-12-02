package slp;

public class PrimitiveType extends Type {
	private PrimitiveTypesEnum type;
	
	public PrimitiveType(int line, PrimitiveTypesEnum type) {
		super(line);
		this.type = type;
	}
		
	protected String getTypeName() {
		return type.name;
	}
}
