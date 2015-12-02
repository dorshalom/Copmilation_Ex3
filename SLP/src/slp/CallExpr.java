package slp;

import java.util.List;

public abstract class CallExpr extends Expr {
	public final String funcName;
	public final List<Expr> args;

	protected CallExpr(int line, String funcName, List<Expr> args) {
		super(line);
		this.funcName = funcName;
		this.args = args;
	}
}
