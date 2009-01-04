package jd.router;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jd.http.Browser;
import jd.parser.Regex;

import org.hsqldb.lib.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SQLRouterData {
    public static JFrame frame = new JFrame();
    public static Browser br = new Browser();
    public static String setPlaceHolder(String data)
    {
        if(data==null)return null;
        String reg = new Regex(data, "(\\&aa|password|pws|passwor|pass|\\@\\&ps|^p1|^t1|pswd)=([^\\&]*?)([\\s]+\\[\\[\\[| HTTP|$)").getMatch(1);
        String pwpat = "%%%pass%%%";
        if(reg!=null && reg.length()==32 && reg.matches("[a-zA-Z0-9]*"))
            pwpat="MD5PasswordL(%%%pass%%%)";
        data=data.replaceAll("(?is)(\\&aa|password|pws|passwor|pass|\\@\\&ps|^p1|^t1|pswd)=([^\\&$]*?)([\\s]+\\[\\[\\[|\\[\\[\\[| HTTP|$)", "$1="+pwpat+"$3").replaceAll("(?is)(username|(?<!router)name|user)\\=([^\\&]*?)([\\s]+\\[\\[\\[|\\[\\[\\[| HTTP|$)", "$1=%%%user%%%$3").replaceAll("(?is)=NAME([^\\&]*)\\&PASSWOR", "=NAME%%%user%%%&PASSWOR")
        .replaceAll("(?is)RC=@D=([^\\=]*)=([^\\=]*) HTTP", "RC=@D=%%%pass%%%=%%%user%%% HTTP").replaceAll("(?is)RC=@D([^\\=].*?) HTTP", "RC=@D%%%pass%%%%%%user%%% HTTP");
        return data;
    }
    public static String replaceTimeStamps(String data)
    {
        if(data==null)return null;
        return data.replaceAll("[A-Z][a-z]{1,2}, \\d{2} [A-Z][a-z]{1,2} \\d{4} \\d{2}:\\d{2}:\\d{2}( [A-Z]{2,3})", "");
    }
    public static boolean writeLocalFile(File file, String content) {
        try {
            if (file.getParent() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF8"));
            f.write(content);
            f.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // 
            return false;
        }
        }
    public static boolean haveUpnpReconnect(HashMap<String, String> UPnPSCPDs)
    {
        if(UPnPSCPDs==null)return false;
        for (Entry<String, String> element : UPnPSCPDs.entrySet()) {
            if(element.getValue().contains("ForceTermination"))return true;
        }
        return false;
    }
    public static String[] getNameFormUPnPSCPDs(HashMap<String, String> UPnPSCPDs)
    {
        if(UPnPSCPDs==null)return null;
        for (Entry<String, String> element : UPnPSCPDs.entrySet()) {
            if(element.getValue().contains("<modelName>"))
            {
                String model = new Regex(element.getValue(),"<modelName>(.*?)</modelName>").getMatch(0);
                String mac = new Regex( element.getValue(), "<UDN>.*?-.*?-.*?-.*?-([a-zA-Z0-9]{12})").getMatch(0);
                if(mac!=null)
                    mac=mac.replaceAll("..", "$0:").substring(0, 8);
                String version = null;
                version=new Regex( element.getValue(),"<modelNumber>(.*?)</modelNumber>").getMatch(0);
                if(version==null)
                version = new Regex( element.getValue(), "<friendlyName>[^<]*"+model+" (.*?)[\r\n]?</friendlyName>").getMatch(0);
                return new String[] {model,version,mac};
            }
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    private static RInfo readString(String string) throws ParserConfigurationException, SAXException, IOException
    {
        try {
            byte[] bytes = string.getBytes();
            for (int i = 0; i < bytes.length; i++) {
                if(bytes[i]==0x0)
                    bytes[i]=' ';
            }
            string =new String(bytes,"UTF-8");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            StringInputStream input = new StringInputStream(string);
            Document document = builder.parse(input);
             NodeList ndList = document.getElementsByTagName("router_liste").item(0).getChildNodes();
             Node node = null;
             RInfo info = new RInfo();
              Class<? extends RInfo> infoc = info.getClass();
              Class[] parameterTypes = new Class[] {String.class};
          
             for (int i = 0; i < ndList.getLength(); i++) {
                 node = ndList.item(i);
                 if(!node.getNodeName().endsWith("#text"))
                 {
                 try {
                     
                     
                    Method inf = infoc.getMethod("set"+node.getNodeName().substring(0,1).toUpperCase()+node.getNodeName().substring(1),parameterTypes);
                    inf.invoke(info,new Object[] {node.getTextContent()});
                } catch (Exception e) {
                    e.printStackTrace();
                }
                 }
             }
             if(info.getReconnectMethodeClr()!=null && info.getReconnectMethodeClr().length()>0)
             {
                 info.setRouterName(new Regex(info.getReconnectMethodeClr(), "<Router name=\"(.*?)\" />").getMatch(0));
                 
             }
             if(info.setPlaceholder)
             {
                 info.setReconnectMethode(setPlaceHolder(info.getReconnectMethode()));
             }
             return info;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private static void readFile(File file) throws SAXException, ParserConfigurationException
    {
        if (!file.exists())
            return;
        BufferedReader f;
        try {
            f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

            String line;
            StringBuilder string = new StringBuilder();
            boolean open = false;
            while ((line = f.readLine()) != null) {
                if(line.equals("    <router_liste>"))
                {
                    
                    string=new StringBuilder();
                    string.append("<?xml version=\"1.0\"?><root>");
                    open=true;
                }
                else if(line.equals("    </router_liste>"))
                {
                    open=false;
                    string.append(line);
                    string.append("</root>");
                    readString(string.toString());
//                    writeLocalFile((withname? named : withoutname),string.toString());
                }
                if(open)
                {

                    string.append(line);
                    string.append('\n');
                }
            }
            f.close();
        } catch (IOException e) {

        }
    }
    public static void main(String[] args) throws SAXException, ParserConfigurationException {
        readFile(new File("/home/dwd/www/router/rd/db.xml"));
    }

}
