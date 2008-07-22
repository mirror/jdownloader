package myIrcBot;



public class Main {
	/**
	 * GNU GPL Lizenz Den offiziellen englischen Originaltext finden Sie unter http://www.gnu.org/licenses/gpl.html.
	 * 
	 *
	 */
	public static void main(String[] args) throws Exception {
//		System.setProperty("socksProxySet", "true");
//		System.setProperty("socksProxyHost", "localhost");
//		System.setProperty("socksProxyPort", "9050");
		
//        System.setProperty("http.proxyHost", "www-proxy.t-online.de");
//        System.setProperty("http.proxyPort", "80");
		// Now start our bot up.
		MyBot bot = new MyBot();

		// Enable debugging output.
		bot.setVerbose(false);
		bot.setAutoNickChange(true);
		bot.connect(bot.server);
//		bot.connect("mejokbp2brhw4omd.onion");
		bot.identify(bot.password);
		bot.joinChannels();

	}

}