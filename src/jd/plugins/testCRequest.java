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

import jd.parser.Form;

public class testCRequest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CRequest request = new CRequest().getRequest("http://www.google.de");
		Form form = request.getForm();
		form.put("q", "jDownloader");
		form.remove("btnI");
		RequestInfo requestInfo = form.getRequestInfo();
		System.out.println("Cookie Ohne CRequest:"+requestInfo.getCookie());
		System.out.println("Cookie mit CRequest:"+request.setRequestInfo(requestInfo).getCookie());
	}

}
