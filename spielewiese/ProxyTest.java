import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

import jd.http.Browser;


public class ProxyTest {

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
       Browser br= new Browser();
      //hast mal einen proxy da zum testen?
       // schon funtzt, der proxy wird benutzt
       //yeah cool...
       
     //allerdings fehlen noch proxys mit paswörtern
      // oder?
       // hmm. a gabs doch so ein separaten part wo die pw's gesetzt wurden
       // das da wird jedesmal gemacht so wie ich das sehe
       //ja.. wenn das reicht ist das ok
               
       SocketAddress  sadd = new InetSocketAddress("server",8080);
       Proxy proxy = new Proxy(Proxy.Type.HTTP,sadd);
       br.setProxy(proxy);
       br.setFollowRedirects(true);
       br.getPage("http://google.de");
System.out.println(br+"");
    }

}
