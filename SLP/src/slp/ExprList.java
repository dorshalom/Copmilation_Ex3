package slp;

import java.util.List;
import java.util.ArrayList;

public class ExprList{
	public final List<Expr> exprs;
	
	public ExprList(Expr expr) {
		this.exprs = new ArrayList<Expr>();
		this.exprs.add(expr);
	}
	
	public ExprList(List<Expr> exprs) {
		this.exprs = exprs;
	}
	
	public void add(Expr expr) {
		exprs.add(expr);
	}
}
