package jd.plugins;

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
