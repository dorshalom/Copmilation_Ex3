package slp;

import java_cup.runtime.*;

/******************************/
/* DOLAR DOLAR - DON'T TOUCH! */
/******************************/
      
%%
 
%class Lexer
%throws Lexception
%implements sym

%line
%column
%cup

%state STRING 

/****************/
/* DECLARATIONS */
/****************/
  
%{   
  	StringBuilder string = new StringBuilder();
  
    /*********************************************************************************/
    /* Create a new java_cup.runtime.Symbol with information about the current token */
    /*********************************************************************************/
    private Symbol symbol(int type)               { return new Symbol(type, yyline, yycolumn); }
    private Symbol symbol(int type, Object value) { return new Symbol(type, yyline, yycolumn, value); }
    
    private class Lexception extends Exception
	{
		private String m_message;
	
		public Lexception(String message, int line) {
			m_message = new String(""+line+": Lexical error: " + message);
	    }
	    
	    public String toString(){
	    	return m_message;
	    }
	}
	
	public int yline() { return yyline+1; }
    
%}

/***********************/
/* MACRO DECALARATIONS */
/***********************/

LineTerminator	 = \r|\n|\r\n
WhiteSpace		 = {LineTerminator} | [ \t\f]
INTEGER			 = 0 | [1-9][0-9]*
IDENTIFIER		 = [a-z][A-Za-z_0-9]*
CLASS_IDENTIFIER = [A-Z][A-Za-z_0-9]*
QUOTE_NO_ESCAPE  = [ !#-\[\]-~]			// space or ! or # until [ or ] until ~ .These are all printable ascii chars, except \ and "
WRONG_ID		 = [0-9]+[a-zA-Z_]+ 
   
TraditionalComment = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment = "//" [^\r\n]* {LineTerminator}?
DocumentationComment = "/*" "*"+ [^/*] ~"*/"
Comment = {TraditionalComment} | {EndOfLineComment} | {DocumentationComment} // from java 1.2 lexer
UnterminatedComment = "/*"([^*]|"*"*[^*/])*"*"*

/******************************/
/* DOLAR DOLAR - DON'T TOUCH! */
/******************************/

%%

<YYINITIAL> {
		"="					{ return symbol(ASSIGN); }
		";"					{ return symbol(SEMI); }
		","					{ return symbol(COMMA); }
		"."					{ return symbol(DOT); }
		"{"					{ return symbol(LCBR); }
		"}"					{ return symbol(RCBR); }
		"("					{ return symbol(LP); }
		")"					{ return symbol(RP); }
		"["					{ return symbol(LB); }
		"]"					{ return symbol(RB); }
		"+"					{ return symbol(PLUS); }
		"-"					{ return symbol(MINUS); }
		"*"					{ return symbol(MULTIPLY); }
		"/"					{ return symbol(DIVIDE); }
		"%"					{ return symbol(MOD); }
		"<"					{ return symbol(LT); }
		">"					{ return symbol(GT); }
		"<="				{ return symbol(LTE); }
		">="				{ return symbol(GTE); }
		"=="				{ return symbol(EQUAL); }
		"!="				{ return symbol(NEQUAL); }
		"!"					{ return symbol(LNEG); }
		"&&"				{ return symbol(LAND); }
		"||"				{ return symbol(LOR); }
		"true"				{ return symbol(TRUE); }
		"false"				{ return symbol(FALSE); }
		"null"				{ return symbol(NULL); }
		"boolean"			{ return symbol(BOOLEAN); }
		"void"				{ return symbol(VOID); }
		"int"				{ return symbol(INT); }
		"string"			{ return symbol(STR); }	// had to rename the token to STR, because of a bug which assigns type "boolean" instead of type "string"
		"break"				{ return symbol(BREAK); }
		"continue"			{ return symbol(CONTINUE); }
		"return"			{ return symbol(RETURN); }
		"new"				{ return symbol(NEW); }
		"while"				{ return symbol(WHILE); }
		"if"				{ return symbol(IF); }
		"else"				{ return symbol(ELSE); }
		"length"			{ return symbol(LENGTH); }
		"static"			{ return symbol(STATIC); }
		"this"				{ return symbol(THIS); }
		"extends"			{ return symbol(EXTENDS); }
		"class"				{ return symbol(CLASS); }
		
		\"                 	{ string.setLength(0); yybegin(STRING); }
		  
		{IDENTIFIER}		{ return symbol(ID, new String(yytext())); }
		
		{CLASS_IDENTIFIER}	{ return symbol(CLASS_ID, new String(yytext())); }
		
		{WRONG_ID}			{ throw new Lexception("Identifiers may not start with numbers", yline()); }
							
		{WhiteSpace}		{ 	/* ignore */ } 
		
		{Comment}           { 	/* ignore */ }
		
		{UnterminatedComment} { throw new Lexception("Unterminated comment", yline()); }
		
		"-2147483648"       { return symbol(INTEGER, new Integer(Integer.MIN_VALUE)); }
		
		{INTEGER}			{ 	
							  if (Long.parseLong(yytext()) > Integer.MAX_VALUE) 
								throw new Lexception("Constant out of integer bounds", yline());
							  else
								return symbol(INTEGER, new Integer(yytext()));
							}
		
		.					{ throw new Lexception("illegal character '" + yytext()+ "'", yline()); }  
}

<STRING> {					// from java 1.2 lexer
		\"                	{ yybegin(YYINITIAL); return symbol(QUOTE, string.toString()); }
		  
		{QUOTE_NO_ESCAPE}   { string.append( yytext() ); }
		  
		"\\t" | "\\n" | "\\r" | "\\\"" |  "\\\\"          { string.append( yytext() ); }
		 /* To be changed to 
		 *              { string.append( '\t' ); }
		 *              { string.append( '\n' ); }
		 *              { string.append( '\r' ); }
		 *              { string.append( '\"' ); }
		 *              { string.append( '\\' ); }
		 * once there's no need to print the quote */
		  
		\\.                 { throw new Lexception("Illegal escape sequence \""+yytext()+"\"", yline()); }
		{LineTerminator}    { throw new Lexception("Unterminated string", yline()); }
		.					{ throw new Lexception("illegal character '"+yytext()+"'", yline()); }
}

<STRING><<EOF>>				{ throw new Lexception("Unterminated string", yline()); }
<<EOF>> 					{ return symbol(EOF); }
