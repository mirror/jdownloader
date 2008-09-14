package jd.router;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import jd.parser.Regex;

import jd.utils.JDUtilities;


public class getMacadress {
    public static String getMacAddress() throws SocketException, UnknownHostException 
    { 
    try {
        return new getMacadress().getMacAddress("10.11.12.253");
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    } 
    return null;
     
    }
    private String getMacAddress ( String host ) throws UnknownHostException, IOException, InterruptedException
    {
        InetAddress hostAddress = InetAddress.getByName( host );
        String resultLine = callArpTool ( hostAddress.getHostAddress() );
        return new Regex(resultLine, "..[:\\-]..[:\\-]..[:\\-]..[:\\-]..[:\\-]..").getMatch(-1).replaceAll("-", ":");
    }
    

    
    private String callArpTool ( String ipAddress ) throws IOException, InterruptedException
    {
        if ( System.getProperty("os.name").toLowerCase().startsWith("windows") )
        {
            return callArpToolWindows( ipAddress );
        }
 
        return callArpToolDefault( ipAddress );
    }
    
    private String callArpToolWindows ( String ipAddress ) throws IOException, InterruptedException
    {
        ProcessBuilder pb = new ProcessBuilder(new String[] {"ping", ipAddress});
        pb.start();
        String[] parts = JDUtilities.runCommand("arp", new String[] {"-a"}, null,10).split(System.getProperty("line.separator"));
        pb.directory();
        for ( String part : parts )
        {            
            if ( part.indexOf(ipAddress) > -1 )
            {
                return part;
            }
        }
        return null;
     }
    
    private String callArpToolDefault ( String ipAddress ) throws IOException, InterruptedException
    {
        ProcessBuilder pb = new ProcessBuilder(new String[] {"ping", ipAddress});
        pb.start();
        String out = JDUtilities.runCommand("arp", new String[] {"-a", ipAddress}, null,10);
        pb.directory();
        return out;
     }
    public static void main(String[] args) {
        try {
            System.out.println(getMacAddress());
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
