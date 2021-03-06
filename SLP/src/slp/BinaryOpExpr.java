package slp;

public class BinaryOpExpr extends Expr {
	public final Expr leftOp;
	public final BinaryOpsEnum operator;
	public final Expr rightOp;

	public BinaryOpExpr(int line, Expr leftOp, BinaryOpsEnum operator, Expr rightOp) {
		super(line);
		this.leftOp = leftOp;
		this.operator = operator;
		this.rightOp = rightOp;
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

