package slp;

public class IfStmt extends Stmt {
	public final Expr condition;
	public final Stmt thenStmt;
	public final Stmt elseStmt;

	public IfStmt(int line, Expr condition, Stmt then, Stmt els) {
		super(line);
		this.condition = condition;
		this.thenStmt = then;
		this.elseStmt = els;
	}

	public IfStmt(int line, Expr condition, Stmt then) {
		this(line, condition, then, null);
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

