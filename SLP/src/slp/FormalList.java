package slp;

import java.util.List;
import java.util.ArrayList;

public class FormalList{
	public final List<Formal> formals;
	
	public FormalList(Formal fl) {
		formals = new ArrayList<Formal>();
		formals.add(fl);
	}
	
	public FormalList(List<Formal> formals) {
		this.formals = formals;
	}

	public void add(Formal fl) {
		formals.add(fl);
	}

}
