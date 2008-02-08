/**
 * 
 */
package jd.plugins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author jDTeam
 *
 */
public class TempEmail {
	public String emailname=System.currentTimeMillis()+"jd";
	private String[][] emails = null;
	public TempEmail() {
	}
	/**
	 * new String[][] {{"From", "TargetURL", "Subject"}};
	 * @return
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public String[][] getMailInfos() throws MalformedURLException, IOException {
		if(emails!=null)
		return emails;
		RequestInfo requestInfo = Plugin.getRequest(new URL("http://mailin8r.com/maildir.jsp?email="+emailname));
		emails = new Regexp(requestInfo.getHtmlCode(), "<tr><td bgcolor=\\#EEEEFF><b>(.*?)</b></td><td bgcolor=\\#EEEEFF align=center><a href=(.*?)>(.*?)</a></td></tr>").getMatches();
		return emails;
	}
	/**
	 * Gibt den Inhalt der mail i zurück
	 * @param index
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public String getMail(int index) throws MalformedURLException, IOException
	{
		if(getEmailAdress()==null || emails.length<=index)
		return null;
		RequestInfo requestInfo = Plugin.getRequest(new URL("http://mailin8r.com"+emails[index][1].replaceFirst("showmail", "showmail2")));
		return requestInfo.getHtmlCode();
	}
	/**
	 * Die aktuelle Emailadresse
	 * @return
	 */
	public String getEmailAdress()
	{
		return emailname+"@mailinator.com";
	}
	public String getFilteredMail(MailFilter mailFilter) throws MalformedURLException, IOException
	{
		if(getEmailAdress()==null)return null;
		for (int i = 0; i < emails.length; i++) {
			if(mailFilter.fromAdress(emails[i]))return getMail(i);
		}
		return null;
	}
}