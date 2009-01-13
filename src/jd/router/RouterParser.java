//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.router;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.JFrame;

import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ließt eine Routers.dat Datei aus
 * 
 * @author astaldo
 */
public class RouterParser {
    private static Logger logger = JDUtilities.getLogger();
    int positionInFile = 0;

    private byte readBuffer[] = new byte[8];

    private String getPlainURL(String string) {
        int index;
        if (string.startsWith("POST")) {
            string = string.substring("POST".length());

            if ((index = string.indexOf("?")) > 0) {

                string = string.substring(0, index);
            }
        } else if (string.startsWith("<POST>")) {
            string = string.substring("<POST>".length());

            if ((index = string.indexOf("?")) > 0) {

                string = string.substring(0, index);
            }
        } else if (string.startsWith("GET")) {
            string = string.substring("GET".length());
        } else if (string.startsWith("<GET>")) {
            string = string.substring("<GET>".length());
        }
        return string;

    }

    private String getPostParams(String string) {
        int index;
        if (!string.startsWith("POST") && !string.startsWith("<POST>")) { return null; }
        if ((index = string.indexOf("?")) > 0) { return string.substring(index + 1); }
        return null;

    }

    private int getRequestType(String string) {
        if (string.startsWith("POST") || string.startsWith("<POST>")) { return RouterData.TYPE_WEB_POST; }
        return RouterData.TYPE_WEB_GET;

    }

    /**
     * Mit dieser Methode wird eine Routers.dat in einzelne RouterData Objekte
     * zerteilt
     * 
     * @param file
     *            Die Datei, die importiert werden soll
     * @return Ein Vector mit eingelesen RouterData Objekten
     */
    public Vector<RouterData> parseFile(File file) {
        Vector<RouterData> routerData = new Vector<RouterData>();
        int count = 0;
        try {
            FileInputStream fis = new FileInputStream(file);
            int ende = readInt(fis) * 2;
            while (count < ende) {
                routerData.add(parseSingleRouter(fis));
                count++;
            }
            logger.info(count + " router data loaded");
            return routerData;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Hier wird ein einzelnes RouterData Objekt eingelesen
     * 
     * @param is
     *            Der InputStream, der zum Import genutzt werden soll
     * @return Das ausgelesene RouterData Objekt
     * @throws IOException
     */
    public RouterData parseSingleRouter(InputStream is) throws IOException {
        RouterData routerData = new RouterData();
        @SuppressWarnings("unused")
        int routerPort;
        String ipAddressPre;
        String ipAddressPost;
        String disconnectString;
        String connectString;
        String login;

        routerPort = readInt(is);
        readByte(is);
        login = readNextString(is);

        readLong(is); // es muss ein long übersprungen werden
        routerData.setRouterName(readNextString(is)); // routername wird
        // gespeichert

        readNextString(is);// url zum routerhersteller wird übersprungen
        readNextString(is);// kommentar vom ersteller wird übersprungen

        connectString = readNextString(is);
        disconnectString = readNextString(is);
        routerData.setIpAddressSite(readNextString(is));
        routerData.setIpAddressOffline(readNextString(is));
        ipAddressPre = readNextString(is);
        ipAddressPost = readNextString(is);

        // informationen der Statusseite 2 werden übersprungen
        readNextString(is);
        int loop1 = readInt(is);
        for (int i = 0; i < loop1; i++) {
            readNextString(is);
            readNextString(is);
        }
        // informationen über die benutzerspezifischen links werden übersprungen
        for (int i = 0; i < 3; i++) {
            readNextString(is);
        }

        is.read();
        is.read();
        is.read();
        is.read();
        is.read();
        positionInFile += 5;
        routerData.setLogoff(readNextString(is));
        readNextString(is);

        int loop2 = readInt(is);
        // System.out.println("loop2 " + loop2);

        for (int i = 0; i < loop2; i++) {
            readNextString(is);
            readNextString(is);
        }
        // informationen über die statusseite 3 werden übersprungen
        readNextString(is);
        int loop3 = readInt(is);

        for (int i = 0; i < loop3; i++) {
            readNextString(is);
            readNextString(is);
        }

        // Nachbearbeitung
        ipAddressPre = Pattern.quote(ipAddressPre);
        ipAddressPost = Pattern.quote(ipAddressPost);
        routerData.setIpAddressRegEx(ipAddressPre + "([0-9.]*)" + ipAddressPost);
        // Zerlege Disconnecturl
        routerData.setDisconnectType(getRequestType(disconnectString));
        routerData.setDisconnectPostParams(getPostParams(disconnectString));
        routerData.setDisconnect(getPlainURL(disconnectString));
        // Zerlege ConnectURL
        routerData.setConnectType(getRequestType(connectString));
        routerData.setConnectPostParams(getPostParams(connectString));
        routerData.setConnect(getPlainURL(connectString));
        // Zerlege LoginURL
        routerData.setLoginType(getRequestType(login));
        routerData.setLoginPostParams(getPostParams(login));
        routerData.setLogin(getPlainURL(login));

        return routerData;
    }

    /**
     * Mit dieser Methode wird eine routerdata.xml in einzelne RouterData
     * Objekte zerteilt
     * 
     * @param file
     *            Die XML Datei, die importiert werden soll
     * @return Ein Vector mit eingelesen RouterData Objekten
     */
    @SuppressWarnings("unchecked")
    public Vector<RouterData> parseXMLFile(File file) {

        Vector<RouterData> routerData = (Vector<RouterData>) JDIO.loadObject(new JFrame(), file, true);
        return routerData;
    }

    /**
     * Liest das nächste Byte ein
     * 
     * @param is
     *            Der InputStream, der zum Import genutzt werden soll
     * @return Die ausgelesene Zahl
     * @throws IOException
     */
    public byte readByte(InputStream is) throws IOException {
        int ch = is.read();
        positionInFile++;
        if (ch < 0) { throw new EOFException(); }
        return (byte) ch;
    }

    /**
     * Liest einen Integer Wert ein
     * 
     * @param is
     *            Der InputStream, der zum Import genutzt werden soll
     * @return Die ausgelesene Zahl
     * @throws IOException
     */
    private int readInt(InputStream is) throws IOException {
        int ch1 = is.read();
        positionInFile++;
        int ch2 = is.read();
        positionInFile++;
        int ch3 = is.read();
        positionInFile++;
        int ch4 = is.read();
        positionInFile++;
        if ((ch1 | ch2 | ch3 | ch4) < 0) { throw new EOFException(); }
        return (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0);
    }

    /**
     * Liest ein Long ein
     * 
     * @param is
     *            Der InputStream, der zum Import genutzt werden soll
     * @return Die ausgelesene Zahl
     * @throws IOException
     */
    public final long readLong(InputStream is) throws IOException {
        is.read(readBuffer, 0, 8);
        positionInFile += 8;
        return ((long) readBuffer[0] << 56) + ((long) (readBuffer[1] & 255) << 48) + ((long) (readBuffer[2] & 255) << 40) + ((long) (readBuffer[3] & 255) << 32) + ((long) (readBuffer[4] & 255) << 24) + ((readBuffer[5] & 255) << 16) + ((readBuffer[6] & 255) << 8) + ((readBuffer[7] & 255) << 0);
    }

    /**
     * Es wird der nächste Text ausgelesen. Dazu wird zuerst die Länge als Short
     * ausgelesen und dann der Text
     * 
     * @param is
     *            Der InputStream, der zum Import genutzt werden soll
     * @return Der eingelesene Text
     * @throws IOException
     */
    private String readNextString(InputStream is) throws IOException {
        int length = readShort(is);
        byte b[] = new byte[length];
        is.read(b);
        positionInFile += length;
        // System.out.println(new String(b));
        return new String(b);
    }

    /**
     * Liest ein Short ein.
     * 
     * @param is
     *            Der InputStream, der zum Import genutzt werden soll
     * @return Die ausgelesene Zahl
     * @throws IOException
     */
    public short readShort(InputStream is) throws IOException {
        int ch1 = is.read();
        positionInFile++;
        int ch2 = is.read();
        positionInFile++;
        if ((ch1 | ch2) < 0) { throw new EOFException(); }
        return (short) ((ch2 << 8) + (ch1 << 0));
    }

    /**
     * konvertiert die routers.dat in eine xml Datei
     * 
     * @param dat
     * @param xml
     */
    public void routerDatToXML(File dat, File xml) {
        RouterParser parser = new RouterParser();
        JDIO.saveObject(new JFrame(), parser.parseFile(dat), xml, "routerdata", "xml", true);

    }
}
