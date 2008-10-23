package jd.utils.httpserver;

import java.util.HashMap;

public class Request {
	private HashMap<String, String> headers = new HashMap<String, String>();
	
	private HashMap<String, String> parameter = new HashMap<String, String>();
	
	private String requestUrl;
	
	private String requestType;
	
	private String httpType;
	
	private String data = "";
	
	public Request() {
	}
	
	protected void setData(String data) {
		this.data = data;
	}
	
	protected void setRequestUrl(String requestUrl) {
		this.requestUrl = requestUrl;
	}
	
	protected void setRequestType(String requestType) {
		this.requestType = requestType;
	}
	
	protected void setHttpType(String httpType) {
		this.httpType = httpType;
	}
	
	protected void addHeader(String key, String value) {
		headers.put(key, value);
	}
	
	protected void setHeader(HashMap<String, String> headers) {
		this.headers = headers;
	}
	
	protected void addParameter(String key, String value) {
		parameter.put(key, value);
	}
	
	protected void setParameter(HashMap<String, String> parameter) {
		this.parameter = parameter;
	}
	
	public String getData() {
		return data;
	}
	
	public String getRequestUrl() {
		return requestUrl;
	}
	
	public String getRequestType() {
		return requestType;
	}
	
	public String getHttpType() {
		return httpType;
	}
	
	public String getHeader(String key) {
		return headers.get(key);
	}
	
	public HashMap<String, String> getHeaders() {
		return headers;
	}
	
	public String getParameter(String key) {
		return parameter.get(key);
	}
	
	public HashMap<String, String> getParameters() {
		return parameter;
	}
}
