package slp;

public class LocalVarStmt extends Stmt {
	public final Type type;
	public final String name;
	public final Expr init;

	public LocalVarStmt(int line, Type type, String name, Expr init) {
		super(line);
		this.type = type;
		this.name = name;
		this.init = init;
	}
	
	public LocalVarStmt(int line, Type type, String name) {
		this(line, type, name, null);
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
}

