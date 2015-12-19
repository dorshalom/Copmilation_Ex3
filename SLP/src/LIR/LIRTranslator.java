package LIR;

import java.util.HashMap;
import java.util.Map;

import slp.ArrayLocation;
import slp.AssignStmt;
import slp.BinaryOpExpr;
import slp.BreakStmt;
import slp.CallStmt;
import slp.Class;
import slp.ContinueStmt;
import slp.Field;
import slp.Formal;
import slp.IfStmt;
import slp.LengthExpr;
import slp.LiteralExpr;
import slp.LiteralsEnum;
import slp.LocalVarStmt;
import slp.Method;
import slp.NewArrayExpr;
import slp.NewClassExpr;
import slp.Program;
import slp.PropagatingVisitor;
import slp.ReturnStmt;
import slp.StaticCall;
import slp.StmtList;
import slp.ThisExpr;
import slp.Type;
import slp.UnaryOpExpr;
import slp.VarLocation;
import slp.VirtCall;
import slp.WhileStmt;

public class LIRTranslator implements PropagatingVisitor<Integer, LIRUpType> {

	
	// count the number of string literals we have seen
	protected int strLiteralsNumber = 0;
	// map each literal string to the format 'str[i]'
	protected Map<String,String> strLiterals = new HashMap<String,String>();
	
	@Override
	public LIRUpType visit(UnaryOpExpr expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(BinaryOpExpr expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Program expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Class expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Field expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Formal expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Type expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Method expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(AssignStmt stmt, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(ReturnStmt stmt, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(StaticCall expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(VirtCall expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(VarLocation expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(ArrayLocation expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(CallStmt stmt, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(StmtList stmt, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(IfStmt stmt, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(WhileStmt stmt, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(BreakStmt stmt, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(ContinueStmt stmt, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(LocalVarStmt stmt, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(ThisExpr expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(NewClassExpr expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(NewArrayExpr expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(LengthExpr expr, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(LiteralExpr expr, Integer d) {
		String strLiteral = "";
		LiteralsEnum type = expr.type;
		if (type == LiteralsEnum.QUOTE){
			String strVal = ((String) expr.value).replaceAll("\n", "\\\\n");
			if (!strLiterals.containsKey(strVal))
				strLiterals.put(strVal, "str"+(strLiteralsNumber++));
			strLiteral = strLiterals.get(strVal);	
		}	
		if (type == LiteralsEnum.INTEGER){
			strLiteral = expr.value.toString();
		}
		if (type == LiteralsEnum.NULL){
			strLiteral = "0";
		}
		if (type == LiteralsEnum.FALSE){
			strLiteral = "0";
		}
		if (type == LiteralsEnum.TRUE){
			strLiteral = "1";
		}		
		return new LIRUpType("", LIRAstNodeType.LITERAL,strLiteral);
	}
	


}
