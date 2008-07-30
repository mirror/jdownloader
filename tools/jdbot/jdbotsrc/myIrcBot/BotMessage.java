package myIrcBot;

import java.io.Serializable;
import java.util.Vector;

public class BotMessage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Vector<String> users = new Vector<String>();
	String regExp = null;
	String message = null;
	public BotMessage(String regExp, String message, String user) {
		this.regExp=regExp;
		this.message=message;
		add(user);
	}
	public void add(String user)
	{
		if(!users.contains(user))
		this.users.add(user);
	}
	
	public boolean equals(Object obj) {
		if(obj instanceof BotMessage) 
		{
			try {
				
				BotMessage botMessage = (BotMessage) obj;
				if(!botMessage.message.equals(message)) return false;
				for (int i = 0; i < botMessage.users.size(); i++) {
					String buser = botMessage.users.get(i);
					if(users.contains(buser));
					return true;
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		return false;
	}
	
	public String toString() {
		// TODO Auto-generated method stub
		return "(\""+regExp +"\") "+message;
	}
}
