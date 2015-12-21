package LIR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import semanticTypes.TypeTable;
import slp.ASTNode;
import slp.ArrayLocation;
import slp.AssignStmt;
import slp.BinaryOpExpr;
import slp.BreakStmt;
import slp.CallStmt;
import slp.Class;
import slp.ClassType;
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
import slp.Stmt;
import slp.StmtList;
import slp.ThisExpr;
import slp.Type;
import slp.UnaryOpExpr;
import slp.VarLocation;
import slp.VirtCall;
import slp.WhileStmt;
import symbolTable.SymbolTable;

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
			if (!cl.name.equals("Library")) //skip library class
				cl.accept(this, 0);
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
		
		
		for(Method m: cl.methods){
			// insert new methods into dispatch table map (if not static)
			if(!m.isStatic){
				// if method already exists because of super class, than it will be overridden
				dispatchTableMap.get(cl.name).put(m.name, cl.name);
			}
			
			//visit all methods
			m.accept(this,0);

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
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
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
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(VirtCall virtCall, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(VarLocation varLoc, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(ArrayLocation ArrLoc, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(CallStmt callStmt, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(StmtList stmtList, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(IfStmt ifStmt, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(WhileStmt whileStmt, Integer d) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(ThisExpr thisExp, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(NewClassExpr newClassExp, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(NewArrayExpr newArrExp, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
	}

	@Override
	public LIRUpType visit(LengthExpr lengthExpr, Integer d) {
		// TODO Auto-generated method stub
		return new LIRUpType("", LIRAstNodeType.EXPLICIT,"");
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
	


}
