package jd.parser;

import jd.http.Browser;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
public final class JavaScript {
	public String javaScript;

	public JavaScript(String javaScript) {
		this.javaScript = javaScript;
	}

	public String callFunction(String functionName) {
		return callFunction(functionName, null);
	}

	public String callFunction(String functionName, String[] parameters) {

		Context cx = Context.enter();
		Scriptable scope = cx.initStandardObjects();
		String parameter = "";
		if (parameters != null) {
			if (parameters.length > 0) {
				parameter = "'" + parameters[0] + "'";
				for (int i = 1; i < parameters.length; i++) {
					parameter += ", '" + parameters[i] + "'";
				}
			}
		}
		String fun = "function f(){ " + javaScript + "\nreturn " + functionName
				+ "(" + parameter + ")} f()";
		Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
		String ret = Context.toString(result);
		Context.exit();
		return ret;
	}
	public String runJavaScript()
	{
		Context cx = Context.enter();
		Scriptable scope = cx.initStandardObjects();
		Object result = cx.evaluateString(scope, javaScript, "<cmd>", 1, null);
		String ret = Context.toString(result);
		Context.exit();
		return ret;
	}
	public String toString() {
		// TODO Auto-generated method stub
		return javaScript;
	}

	/**
	 * TODO
	 * muss noch überarbeitet werden
	 * @param args
	 */
	public static void main(String[] args) {
		Browser b = new Browser();
	b.getPage("http://rapidlayer.in/go/c8f16ccc");
	JavaScript js = b.getJavaScript();
	js.javaScript="var document;\r\n"+js.javaScript;
    String fun_id = b.getRegex("function (.*?)\\(").getFirstMatch();
    String dec = b.getRegex("a=(.*?);document.write").getFirstMatch();

	//js.javaScript=js.javaScript.replaceAll("document.getElementById\\('ausgabe'\\).innerHTML=", "return ");
System.out.println(js);
	System.out.println(js.callFunction(fun_id, new String[] {dec}));
	}

}
