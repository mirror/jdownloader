package tests.singletests;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Test {

    /**
     * @param args
     * @throws UnknownHostException
     */
    public static void main(final String[] args) throws UnknownHostException {
        // Get an instance of InetAddress for the IP address
        final InetAddress inetAddress = InetAddress.getByName("618l3.rapidshare.com");

        // Get the host name
        final String ipAddress = inetAddress.getHostName();

        // Print the host name
        System.out.println(ipAddress);
        System.out.println(inetAddress.getHostAddress());
    }

}
