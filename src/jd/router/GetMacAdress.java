package jd.router;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import jd.parser.Regex;
import jd.utils.JDUtilities;


public class GetMacAdress {
    public static String getMacAddress() throws SocketException, UnknownHostException 
    { 
    try {
        return new GetMacAdress().getMacAddress(RouterInfoCollector.getRouterIP());
    } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    return null;
     
    }
    public String getMacAddress ( InetAddress hostAddress ) throws UnknownHostException, IOException, InterruptedException
    {
        String resultLine = callArpTool ( hostAddress.getHostAddress() );
        String rd = new Regex(resultLine, "..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?").getMatch(-1).replaceAll("-", ":");
        if(rd==null)return null;
        rd=rd.replaceAll("\\s", "0");
        String[] d = rd.split("[:\\-]");
        StringBuilder ret = new StringBuilder(18);
        for (String string : d) {
            
            if (string.length()<2) {
                ret.append('0');
            }
            ret.append(string);
            ret.append(':');
        }
        return ret.toString().substring(0, 17);
    }
    

    
    private String callArpTool ( String ipAddress ) throws IOException, InterruptedException
    {
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.indexOf("nt") > -1 || OS.indexOf("windows") > -1)
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
        String out =null;
        InetAddress hostAddress = InetAddress.getByName( ipAddress );
        ProcessBuilder pb = null;
        try {
            pb = new ProcessBuilder(new String[] {"ping", ipAddress});
            pb.start();
            out = JDUtilities.runCommand("arp", new String[] {ipAddress}, null,10);
            pb.directory();
            if(!out.matches("(?is).*(("+hostAddress.getHostName()+"|"+hostAddress.getHostAddress()+").*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?|.*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?.*("+hostAddress.getHostName()+"|"+hostAddress.getHostAddress()+")).*"))
                out=null;
        } catch (Exception e) {
            if(pb!=null)
            pb.directory();
        }
        if(out==null||out.trim().length()==0)
        {
            try {
                pb = new ProcessBuilder(new String[] {"ping", ipAddress});
                pb.start();
                out = JDUtilities.runCommand("ip", new String[] {"neigh", "show"}, null,10);
                pb.directory();
                if(out!=null)
                {
//                    System.out.println(out);
//                    System.out.println(!out.matches("(?is).*(("+hostAddress.getHostName()+"|"+hostAddress.getHostAddress()+").*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?|.*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?.*("+hostAddress.getHostName()+"|"+hostAddress.getHostAddress()+")).*"));
                    if(!out.matches("(?is).*(("+hostAddress.getHostName()+"|"+hostAddress.getHostAddress()+").*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?|.*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?.*("+hostAddress.getHostName()+"|"+hostAddress.getHostAddress()+")).*"))
                        out=null;
                    else
                    out=new Regex(out, "("+hostAddress.getHostName()+"|"+hostAddress.getHostAddress()+")[^\r\n]*").getMatch(-1);
                }
            } catch (Exception e) {
                if(pb!=null)
                    pb.directory();
            }
        }
        return out;
     }
    public static void main(String[] args) {
        try {
            System.out.println(new GetMacAdress().getMacAddress(new GetRouterInfo(null).getAdress()));
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
