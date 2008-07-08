package myIrcBot;

import java.io.Serializable;
import java.util.Vector;

/**
 * Dieser teil des Programms steht unter Eula Lizenz Die Rechte an diesem
 * Programmteil/code obliegen dem Autor Das Programm/der Sourcecode darf nur vom
 * Autor verteilt werden!
 * 
 * @author DareWithDevil dwdaddhp@yahoo.de
 * 
 */
public class Settings implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7503086199401682499L;
	public Vector<String[]> joinMessages = new Vector<String[]>();
	public Vector<String> ignore = new Vector<String>();
	public String pass = "jdsIrcBot";
	public Vector<String[]> premAccs = new Vector<String[]>();
	public Vector<String> autoOp = new Vector<String>();
	public Vector<String> autoVoice = new Vector<String>();
	public int svnRev = 0;
}
