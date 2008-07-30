package myIrcBot;

/**
 * Dieser teil des Programms steht unter Eula Lizenz Die Rechte an diesem
 * Programmteil/code obliegen dem Autor Das Programm/der Sourcecode darf nur vom
 * Autor verteilt werden!
 * 
 * @author DareWithDevil dwdaddhp@yahoo.de
 * 
 */
public class UserInfo {
	public final static int UNIDENTIFIED = 0;
	public final static int IDENTIFIED = 1;
	public final static int VOICE = 2;
	public final static int OP = 3;
	public String hostname = "";
	public String channels = "";
	public boolean identified = false;

	
	public String toString() {
		// TODO Auto-generated method stub
		return hostname + "\r\n" + channels + "\r\n" + "Identified:"
				+ identified;
	}
}
