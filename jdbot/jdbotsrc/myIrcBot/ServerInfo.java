package myIrcBot;

/**
 * Dieser teil des Programms steht unter Eula Lizenz Die Rechte an diesem
 * Programmteil/code obliegen dem Autor Das Programm/der Sourcecode darf nur vom
 * Autor verteilt werden!
 * 
 * @author DareWithDevil dwdaddhp@yahoo.de
 * 
 */
public class ServerInfo {

	public String response = "";
	public int code = 0;

	public ServerInfo(int code, String response) {
		this.response = response;
		this.code = code;
	}

	public String toString() {
		return response;
	}
}
