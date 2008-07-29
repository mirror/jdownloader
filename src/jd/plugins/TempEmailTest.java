//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


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
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}

	}

}
