package slp;

import java.util.List;

public class Method extends ASTNode {
	public final boolean isStatic;
	public final Type type;
	public final String name;
	public final List<Formal> formalList;
	public final List<Stmt> statementList;

	public Method(int line, boolean isStatic, Type type, String name, List<Formal> formals, List<Stmt> statements) {
		super(line);
		this.isStatic = isStatic;
		this.type = type;
		this.name = name;
		this.formalList = formals;
		this.statementList = statements;
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