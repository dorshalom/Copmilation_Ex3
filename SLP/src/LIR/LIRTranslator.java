package LIR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import slp.*;
import slp.Class;
import semanticTypes.*;
import symbolTable.*;


public class LIRTranslator implements PropagatingVisitor<Integer, LIRUpType> {

	
	// count the number of string literals we have seen
	protected int strLiteralsNumber = 0;
	// map each literal string to the format 'str[i]'
	protected Map<String,String> strLiterals = new HashMap<String,String>();
	// list of methods' lir code
	protected List<String> methodsCodeList = new ArrayList<String>();
	// main method's lir code
	protected String mainMethodCode="";
	// dispatch table lir code
	protected List<String> classDispatchTableCodeList = new ArrayList<String>();
	// dispatch table map - maps class names to a list of <method, belongsToClassName>
	protected Map<String, HashMap<String,String>> dispatchTableMap = new HashMap<String,HashMap<String,String>>();
	
	private ASTNode root;
	private SymbolTable symTab;
	private TypeTable typTab;
	
	private String currentThisClass;

	
	public LIRTranslator(ASTNode root, SymbolTable symTab, TypeTable typTab){
		this.root = root;
		this.symTab = symTab;
		this.typTab = typTab;
		
	}
	

	@Override
	public LIRUpType visit(Program program, Integer d) {
		// visit all classes in the program
		for(Class cl: program.classes){
			// TODO: this check is redundant. we won't have a "Library" class in our program ever.
			if (!cl.name.equals("Library")){ //skip library class
				symTab.enterScope();
				cl.accept(this, 0);
				symTab.exitScope();
			}
		}
		
		String lirCode = "";
		
		// insert string literals
		lirCode += "# string literals #\n";
		for (String str: this.strLiterals.keySet()){
			lirCode += this.strLiterals.get(str)+": \""+str+"\"\n";
		}
		lirCode+= "\n # dispatch table #\n";
		
		
		
		// insert dispatch table
		buildDispatchTableCode();
		for (String line: this.classDispatchTableCodeList){
			lirCode += line+"\n";
		}
		
		lirCode+= "\n";
		
		
		// insert all methods (except main)
		for (String method: this.methodsCodeList){
			lirCode += method+"\n";
		}
		
		lirCode+= "\n";
		
		// insert main method
		lirCode += this.mainMethodCode;
		
		
		return new LIRUpType(lirCode, LIRAstNodeType.EXPLICIT,"");
	}

	@SuppressWarnings("unchecked")
	@Override
	public LIRUpType visit(Class cl, Integer d) {
		
		currentThisClass = cl.name; //update current class
		
		
		// fill dispatch table:
		if(cl.superName == null){ // has no super 
			dispatchTableMap.put(cl.name, new HashMap<String,String>());
		}
		else{ // has super - clone methods list from super class
			dispatchTableMap.put(cl.name, (HashMap<String, String>) dispatchTableMap.get(cl.superName).clone());
		}
		
		// TODO: change offsets to support inheritance
		int fieldOffset = 0;
		for (Field f: cl.fields){
			try{
				symTab.addEntry(new FieldSymbol(f.name, typTab.resolveType(f.type.getName()),fieldOffset));
			} catch (SemanticError se){}
			fieldOffset++;
		}
		
		
		for(Method m: cl.methods){
			// insert new methods into dispatch table map (if not static)
			if(!m.isStatic){
				// if method already exists because of super class, than it will be overridden
				dispatchTableMap.get(cl.name).put(m.name, cl.name);
			}
			
			//visit all methods
			symTab.enterScope();
			m.accept(this,0);
			symTab.exitScope();

		}
		
		

		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(Field field, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Formal formal, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Type type, Integer d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LIRUpType visit(Method method, Integer d) {
		boolean ismain = isMain(method); 
		String methodCode = "";
		
		// get method headline
		String methodHeadLine="";
		if(ismain){
			methodHeadLine+="_ic_main:\n";
		}
		else{
			methodHeadLine+="_"+currentThisClass+"_"+method.name+":\n";
		}
		methodCode+=methodHeadLine;
		
		// visit all statements and add their code to methodCode
		for (Stmt s: method.statementList){
			methodCode += s.accept(this,0).lirCode;
		}
		
		// as the specs says, if the method is void, we add "Return 9999"
		if(!ismain && method.type.getName().equals("void")){
			methodCode += "Return 9999\n";
		}
		
		// update methodsCode list / main method
		if (ismain){
			mainMethodCode = methodCode;
		} else {
			methodsCodeList.add(methodCode);
		}
		
		//empty return... (because we already updated the methodsCodeList.. no need to return code..)
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(AssignStmt assignStmt, Integer d) {
		String str = "";
		// translate rhs
		LIRUpType rhs = assignStmt.rhs.accept(this, d);
		str += rhs.lirCode;
		str += getMoveType(rhs.astNodeType);
		str += rhs.register+",";
		str += "R"+d+"\n";
		
		// translate lhs
		LIRUpType lhs = assignStmt.lhs.accept(this, d+1);
		str+= lhs.lirCode;
		
		// handle all variable cases
		str += getMoveType(lhs.astNodeType);
		str += "R"+d+","+lhs.register+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(ReturnStmt retStmt, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}
	
	@Override
	public LIRUpType visit(UnaryOpExpr unaryOp, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(BinaryOpExpr binaryOp, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(StaticCall staticCall, Integer d) {
		// TODO: not finished. Only deals with Library calls
		String str = "";
		
		// recursive calls to all arguments
		int reg = d;
		for (Expr arg: staticCall.args){
			LIRUpType argExp = arg.accept(this, reg);
			str += "# argument #"+(reg-d)+":\n";
			str += argExp.lirCode;
			str += getMoveType(argExp.astNodeType);
			str += argExp.register+", R"+reg+"\n";
			reg++;
		}
		
		// Library method call
		if (staticCall.className.equals("Library")){
			str += "Library __"+staticCall.funcName+"(";
			// iterate over values (registers)
			for(int i = 0; i < staticCall.args.size(); i++){
				str += "R"+(i+d)+", ";
			}
			// remove last comma
			if (str.endsWith(", ")) str = str.substring(0, str.length()-2);
			str += "), R"+d+"\n";
			
			return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
		}
		
		//TODO: other static methods
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(VirtCall virtCall, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(VarLocation varLoc, Integer d) {
		String str = "";
		
		// location.ID
		if (varLoc.location != null){
			// translate the location
			LIRUpType loc = varLoc.location.accept(this, d);
			// add code to translation
			str += loc.lirCode;
			
			// get the type of the location
			SemanticType locationType = (SemanticType) varLoc.location.accept(new ExprTypeResolver(symTab, typTab, currentThisClass), null);
			
			// get the field offset
			ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(locationType.name);	
			FieldSymbol fs = null;
			try{	// will always succeed
				fs = cs.getFieldSymbolRec(varLoc.name);
			}catch (SemanticError se){}			 
			int fieldOffset = fs.getOffset();
			
			// translate this step
			str += getMoveType(loc.astNodeType);
			String locReg = "R"+d;
			str += loc.register+","+locReg+"\n";
			
			// check external location null reference
			//TODO: str += "StaticCall __checkNullRef(a=R"+d+"),Rdummy\n";
			
			return new LIRUpType(str, LIRAstNodeType.EXTERNALVARLOC, locReg+"."+fieldOffset);
		// ID
		}else{
			int scopeLevel = symTab.findScopeLevel(varLoc.name);
			if (scopeLevel == 2){	// it is a field of THIS
				ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(currentThisClass);
				FieldSymbol fs = null;
				try{
					fs = cs.getFieldSymbolRec(varLoc.name);
				}catch(SemanticError se){}
				int fieldOffset = fs.getOffset();
				
				str += "Move this, R"+d+"\n";
				String tgtLoc = "R"+d+"."+fieldOffset;
				
				return new LIRUpType(str, LIRAstNodeType.EXTERNALVARLOC, tgtLoc);
			}else{
				// it's not a field, so it must be local variable
				return new LIRUpType("",LIRAstNodeType.LOCALVARLOC, varLoc.name + scopeLevel);
			}
		}
	}

	@Override
	public LIRUpType visit(ArrayLocation ArrLoc, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(CallStmt callStmt, Integer d) {
		return callStmt.call.accept(this, d);
	}

	@Override
	public LIRUpType visit(StmtList stmtList, Integer d) {
		String str = "";
		
		symTab.enterScope();
		for (Stmt s: stmtList.statements)
			str += s.accept(this, d).lirCode;
		
		symTab.exitScope();
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(IfStmt ifStmt, Integer d) {
		// TODO Auto-generated method stub
		symTab.enterScope();
		ifStmt.thenStmt.accept(this, null);
		symTab.exitScope();
		
		if (ifStmt.elseStmt != null){
			symTab.enterScope();
			ifStmt.elseStmt.accept(this, null);
			symTab.exitScope();
		}
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(WhileStmt whileStmt, Integer d) {
		// TODO Auto-generated method stub
		symTab.enterScope();
		whileStmt.thenStmt.accept(this, null);
		symTab.exitScope();
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(BreakStmt breakStmt, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(ContinueStmt contStmt, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(LocalVarStmt localVarStmt, Integer d) {
		try{
			symTab.addEntry(new VarSymbol(localVarStmt.name, typTab.resolveType(localVarStmt.type.getName())));
		}catch(SemanticError se) {}
		
		String str = "";
		
		if (localVarStmt.init != null){
			LIRUpType initVal = localVarStmt.init.accept(this, d);
			str += initVal.lirCode;
			str += getMoveType(initVal.astNodeType);
			str += initVal.register+",R"+d+"\n";
			// move register into the local var name
			str += "Move R"+d+","+localVarStmt.name+symTab.scopeLevel+"\n";
		}
		
		return new LIRUpType(str, LIRAstNodeType.STATEMENT,"");
	}

	@Override
	public LIRUpType visit(ThisExpr thisExp, Integer d) {
		String str = "Move this, R"+d+"\n";
		return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
	}

	@Override
	public LIRUpType visit(NewClassExpr newClassExp, Integer d) {
		ClassSymbol cs = (ClassSymbol) symTab.findEntryGlobal(newClassExp.name);
		String str = "Library __allocateObject("+cs.bytesInMemory()+"), R"+d+"\n";
		str += "MoveField _DV_"+cs.name+", R"+d+".0\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
	}

	@Override
	public LIRUpType visit(NewArrayExpr newArrExp, Integer d) {
		String str = "";
		
		LIRUpType size = newArrExp.index.accept(this, d);
		str += size.lirCode;
		str += getMoveType(size.astNodeType);
		str += size.register+", R"+d+"\n";
		str += "Mul 4, R"+d+"\n";
		
		// make sure index is non-negative
		//TODO: str += "StaticCall __checkSize(n=R"+d+"), Rdummy\n";
		str += "Library __allocateArray(R"+d+"), R"+d+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
	}

	@Override
	public LIRUpType visit(LengthExpr lengthExpr, Integer d) {
		String str = "";

		LIRUpType array = lengthExpr.context.accept(this, d);
		str += array.lirCode;
		str += getMoveType(array.astNodeType);
		str += array.register+", R"+d+"\n";
		
		// make sure context is non-null
		str += "StaticCall __checkNullRef(a=R"+d+"), Rdummy\n";
		str += "ArrayLength R"+d+", R"+d+"\n";
		
		return new LIRUpType(str, LIRAstNodeType.REGISTER,"R"+d);
	}

	@Override
	public LIRUpType visit(LiteralExpr expr, Integer d) {
		String strLiteral = "";
		LiteralsEnum type = expr.type;
		if (type == LiteralsEnum.QUOTE){
			//TODO: 1. We don't have escape characters inside quotes (see slp.lex).
			//      2. Why \\\\n and not \\n ?
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
	
	private boolean isMain(Method m){
		if (m.isStatic)  
			if (m.type.getName().equals("void")) 
				if (m.name.equals("main")) 
					if(m.formalList.size() == 1)
						if(m.formalList.get(0).name.equals("args"))
							if(m.formalList.get(0).type.getName().equals("string[]")) 
								return true;

		return false;
	}
	
	private void buildDispatchTableCode(){
		
		for (Map.Entry<String, HashMap<String,String>> ce: dispatchTableMap.entrySet()){ //go through each class entry		
			String str="";
			str+= "_DV_"+ ce.getKey() +": [";
			for (Map.Entry<String, String> me: ce.getValue().entrySet()){ // go through each method entry
				str+="_"+me.getValue()+"_"+me.getKey()+",";
			}
			str = str.substring(0, str.length()-1); //chop the last ','
			str+="]";
			classDispatchTableCodeList.add(str);
		}
	}
	
	private String getMoveType(LIRAstNodeType type){
		switch(type) {
		case LITERAL: 
		case REGISTER: 
		case LOCALVARLOC: 	 return "Move ";
		case ARRAYLOC: 		 return "MoveArray ";
		case EXTERNALVARLOC: return "MoveField ";
		default:
			System.out.println("Unhandled LIR instruction type");
			return null;
		}
	}
}
