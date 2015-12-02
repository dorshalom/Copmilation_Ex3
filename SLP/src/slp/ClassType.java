package slp;

public class ClassType extends Type {
	private String name;

	public ClassType(int line, String name) {
		super(line);
		this.name = name;
	}
	
	protected String getTypeName() {
		return name;
	}
}
