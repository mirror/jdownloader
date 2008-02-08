package jd.plugins;

import java.io.IOException;
import java.net.MalformedURLException;

public class TempEmailTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TempEmail email = new TempEmail();
		email.emailname="jdownloader";
		try {
			String[][] emails = email.getMailInfos();
			for (int i = 0; i < emails.length; i++) {
				for (int j = 0; j < emails[i].length; j++) {
					System.out.println(emails[i][j]);

				}
				System.out.println(email.getMail(i));
			}
			System.out.println(email.getFilteredMail(new MailFilter(){
				public boolean fromAdress(String[] mailInfo) {
					if(mailInfo[0].matches("dwd.*")) return true;
					return false;
				}}));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
