package slp;

import java.util.ArrayList;
import java.util.List;

public class Class extends ASTNode {
	public final String name;
	public final String superName;
	public final List<Field> fields;
	public final List<Method> methods;

	public Class(int line, String name, List<Field> fields, List<Method> methods) {
		this(line, name, null, fields, methods);
	}
	
	public Class(int line, String name, String superName, List<Field> fields, List<Method> methods) {
		super(line);
		this.name = name;
		this.fields = fields;
		this.methods = methods;
		this.superName = superName;
	}
	
	/** Accepts a visitor object as part of the visitor pattern.
	 * @param visitor A visitor.
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
	
	/** Accepts a propagating visitor parameterized by two types.
	 * 
	 * @param <DownType> The type of the object holding the context.
	 * @param <UpType> The type of the result object.
	 * @param visitor A propagating visitor.
	 * @param context An object holding context information.
	 * @return The result of visiting this node.
	 */
	@Override
	public <DownType, UpType> UpType accept(
			PropagatingVisitor<DownType, UpType> visitor, DownType context) {
		return visitor.visit(this, context);
	}

	public String toString(){
		if (superName != null)
			return name + " extends " + superName;
		else
			return name;
	}
	
	public boolean hasMethodWithName(String name){
		for (Method m: this.methods){
			if (m.name.equals(name)){
				return true;
			}
		}
		return false;
	}
}
