package LIR;

import semanticTypes.SemanticError;
import semanticTypes.SemanticType;
import semanticTypes.TypeTable;
import slp.*;
import slp.Class;
import symbolTable.*;


public class ExprTypeResolver implements PropagatingVisitor<Object, Object> {
	private SymbolTable symTab;
	private TypeTable typTab;
	private String currentThisClass;
	private String currentMethodName;
	
	public ExprTypeResolver(SymbolTable symtab, TypeTable typtab, String currentThisClass, String currentMethodName) {
		this.currentThisClass = currentThisClass;
		this.currentMethodName = currentMethodName;
		this.symTab = symtab;
		this.typTab = typtab;
	}
	
	@Override
	public Object visit(UnaryOpExpr unary, Object d) {
		return (unary.operator == UnaryOpsEnum.UMINUS ? typTab.intType : typTab.booleanType);
	}

	@Override
	public Object visit(BinaryOpExpr binary, Object d) {
		// check left op
		SemanticType leftOpType = (SemanticType) binary.leftOp.accept(this, null);
        if (leftOpType == null) return null;
        // check right op
        SemanticType rightOpType = (SemanticType) binary.rightOp.accept(this, null);
        if (rightOpType == null) return null;
        
		switch(binary.operator.name){
		case "&&": case "||":
	        return typTab.booleanType;
		case ">":case ">=" : case "<": case "<=":
	        return typTab.booleanType;
		case "+":
			if(leftOpType == typTab.stringType && rightOpType == typTab.stringType)
				return typTab.stringType;
			if(leftOpType == typTab.intType &&  rightOpType == typTab.intType)
				return typTab.intType;
		case "-": case "*": case "/": case "%":
	        return typTab.intType;
		case "==": case "!=":
	        return typTab.booleanType;
	    default:
	        return null;
				
		}
	}

	@Override
	public Object visit(Program program, Object d) {
		return null;
	}

	@Override
	public Object visit(Class cl, Object d) {
		return null;
	}

	@Override
	public Object visit(Method method, Object d) {	
		return null;
	}
	
	@Override
	public Object visit(Field expr, Object d) {
		return null;
	}

	@Override
	public Object visit(Formal expr, Object d) {
		return null;
	}

	@Override
	public Object visit(Type expr, Object d) {
		return null;
	}

	@Override
	public Object visit(AssignStmt assignStmt, Object d) {
		return null;
	}

	@Override
	public Object visit(ReturnStmt returnStmt, Object d) {
		return null;
	}

	@Override
	public Object visit(StaticCall staticCall, Object d) {
		MethodSymbol func = null;

		if (staticCall.className != null) { 
			ClassSymbol cl = (ClassSymbol) symTab.findEntryGlobal(staticCall.className);

			try {
				func = cl.getMethodSymbolRec(staticCall.funcName);
			} catch (SemanticError e) {}
	
			return func.type;
		}
		return null;
	}

	@Override
	public Object visit(VirtCall virtCall, Object d) {
		MethodSymbol func = null;

		// when call is local
		if (virtCall.location == null) { 
			func = (MethodSymbol) symTab.findEntryGlobal(virtCall.funcName);
		}
		
		//when call is external [when we have obj.funcName(...)]
		else { 
			SemanticType locationType = (SemanticType) virtCall.location.accept(this, null);
			if (locationType == null) return null;
			try{
				ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(locationType.name);
				func = cs.getMethodSymbolRec(virtCall.funcName);			
			}
			catch (SemanticError se){}
		}

		return func.type;
	}

	@Override
	public Object visit(VarLocation varLoc, Object d) {
		Symbol sym = null;
		
		//external
		if (varLoc.location != null){ 
			SemanticType locationType = (SemanticType) varLoc.location.accept(this, null);
			if (locationType == null) return null;
			try{
				ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(locationType.name);
				sym = cs.getFieldSymbolRec(varLoc.name);					
			}
			catch (SemanticError se){}
		
		// local
		}else{
			sym = symTab.findEntryGlobal(varLoc.name);
			if (sym == null){	// it must be a parameter
				ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(currentThisClass);
				MethodSymbol ms = null;
				try {
					ms = cs.getMethodSymbol(currentMethodName);
				} catch (SemanticError e) {}
				for (ParamSymbol p: ms.params){
					if (p.name.equals(varLoc.name))
						return p.type;
				}
			}
		}		
		
		return sym.type;
	}

	@Override
	public Object visit(ArrayLocation arrayLoc, Object d) {
		SemanticType arrayElement = null;
		SemanticType arrayType = (SemanticType) arrayLoc.array.accept(this, null);

		try{
			arrayElement = typTab.resolveType(arrayType.name.substring(0, arrayType.name.length()-2));
		}catch(SemanticError se){}
		return arrayElement;
	}

	@Override
	public Object visit(CallStmt callStmt, Object d) {
		return null;
	}

	@Override
	public Object visit(StmtList stmtList, Object d) {
		return null;
	}

	@Override
	public Object visit(IfStmt ifStmt, Object d) {
		return null;
	}

	@Override
	public Object visit(WhileStmt whileStmt, Object d) {
		return null;
	}

	@Override
	public Object visit(BreakStmt breakStmt, Object d) {
		return null;
	}

	@Override
	public Object visit(ContinueStmt contStmt, Object d) {
		return null;
	}

	@Override
	public Object visit(LocalVarStmt localVar, Object d) {		
		return null;
	}

	@Override
	public Object visit(ThisExpr expr, Object d) {
		ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(currentThisClass);
		return cs.type;
	}

	@Override
	public Object visit(NewClassExpr newClass, Object d) {
		SemanticType classType = null;
		try{
			classType = typTab.resolveClassType(newClass.name);
		}catch(SemanticError se){}
		return classType;
	}

	@Override
	public Object visit(NewArrayExpr newArray, Object d) {
		SemanticType arrayType = null;
		try{
			arrayType = typTab.resolveArrayType(newArray.type.getName());
		}catch(SemanticError se){}
		return arrayType;
	}

	@Override
	public Object visit(LengthExpr len, Object d) {
		return typTab.intType;
	}

	@Override
	public Object visit(LiteralExpr literal, Object d) {
		switch(literal.type){
		case INTEGER:
			return typTab.intType;
		case FALSE:
			return typTab.booleanType;
		case TRUE:
			return typTab.booleanType;
		case QUOTE:
			return typTab.stringType;
		case NULL:
			return typTab.nullType;
		default:
			return null;
		}
	}
}
