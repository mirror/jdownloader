import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class DNS {

    /**
     * @param args
     * @throws UnknownHostException
     * @throws MalformedURLException
     */
    public static void main(final String[] args) throws UnknownHostException, MalformedURLException {
        final URL url = new URL(args[0]);
        final InetAddress inetAddress = InetAddress.getByName(url.getHost());

        System.out.println("getHostAddress " + inetAddress.getHostAddress());
        System.out.println("getHostAddress " + inetAddress.getHostName());

    }

}
