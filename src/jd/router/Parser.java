package jd.router;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.logging.Logger;

import jd.JDUtilities;
import jd.plugins.Plugin;

/**
 * Versuch, die Routers.dat auszulesen
 * 
 * @author astaldo
 * 
 */
public class Parser {
    private static Logger logger = Plugin.getLogger();
    Vector<Object> routers = new Vector<Object>();

    int positionInFile = 0;

    public void parseFile(File file) {
        int count = 0;
        try {
            FileInputStream fis = new FileInputStream(file);
            int ende = readInt(fis) * 2;
            while (count < ende) {
                parseSingleRouter(fis);
                count++;
            }
            logger.info(count +" router data loaded");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveFile(File file) {
        JDUtilities.saveObject(null, routers, file,"jd",".routers", false);
    }

    public void parseSingleRouter(FileInputStream fis) throws IOException {
        RouterData routerData = new RouterData();
        int routerPort;
        routerPort = readInt(fis);
        routerData.setLoginType(readByte(fis));
        routerData.setLoginString(readNextString(fis));
        readLong(fis); // es muss ein long übersprungen werden
        routerData.setRouterName(readNextString(fis)); // routername wird gespeichert

        
        readNextString(fis);// url zum routerhersteller wird übersprungen
        readNextString(fis);// kommentar vom ersteller wird übersprungen
        routerData.setConnectionConnect(readNextString(fis));
        routerData.setConnectionDisconnect(readNextString(fis));
        routerData.setIpAddressSite(readNextString(fis));
        routerData.setIpAddressOffline(readNextString(fis));
        routerData.setIpAddressPre(readNextString(fis));
        routerData.setIpAddressPost(readNextString(fis));

        // informationen der Statusseite 2 werden übersprungen
        readNextString(fis);
        int loop1 = readInt(fis);
        for (int i = 0; i < loop1; i++) {
            readNextString(fis);
            readNextString(fis);
        }
        // hier muss noch rigendwie der sting für das abmelden vom router kommen
        // informationen über die benutzerspezifischen links werden übersprungen
        for (int i = 0; i < 3; i++) {
            readNextString(fis);
        }

        fis.read();
        fis.read();
        fis.read();
        fis.read();
        fis.read();
        positionInFile += 5;
        // signOffUrl = readNextString(fis);
        routerData.setConnectionLogoff(readNextString(fis));
        readNextString(fis);

        int loop2 = readInt(fis);
//        System.out.println("loop2 " + loop2);

        for (int i = 0; i < loop2; i++) {
            readNextString(fis);
            readNextString(fis);
        }
        // informationen über die statusseite 3 werden übersprungen
        readNextString(fis);
        int loop3 = readInt(fis);

        for (int i = 0; i < loop3; i++) {
            readNextString(fis);
            readNextString(fis);
        }
        
        //TODO Nachbearbeitung 
        // ZB routerData.getConnectionDisconnect <POST> rausschneiden etc
        routers.add(routerData);

    }

    private String readNextString(FileInputStream fis) throws IOException {
        int length = readShort(fis);
        byte b[] = new byte[length];
        fis.read(b);
        positionInFile += length;
//        System.out.println(new String(b));
        return new String(b);
    }

    // InputStreams
    private byte readBuffer[] = new byte[8];

    public byte readByte(InputStream in) throws IOException {
        int ch = in.read();
        positionInFile++;
        if (ch < 0)
            throw new EOFException();
        return (byte) (ch);
    }

    public short readShort(InputStream in) throws IOException {
        int ch1 = in.read();
        positionInFile++;
        int ch2 = in.read();
        positionInFile++;
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (short) ((ch2 << 8) + (ch1 << 0));
    }

    private int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        positionInFile++;
        int ch2 = in.read();
        positionInFile++;
        int ch3 = in.read();
        positionInFile++;
        int ch4 = in.read();
        positionInFile++;
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }

    public final long readLong(InputStream in) throws IOException {
        in.read(readBuffer, 0, 8);
        positionInFile += 8;
        return (((long) readBuffer[0] << 56)
                + ((long) (readBuffer[1] & 255) << 48)
                + ((long) (readBuffer[2] & 255) << 40)
                + ((long) (readBuffer[3] & 255) << 32)
                + ((long) (readBuffer[4] & 255) << 24)
                + ((readBuffer[5] & 255) << 16) + ((readBuffer[6] & 255) << 8) + ((readBuffer[7] & 255) << 0));
    }

    public static void main(String args[]) {
        Parser parser = new Parser();

        parser.parseFile(new File("C:/Dokumente und Einstellungen/rogerssocke/Desktop/jdownloader/routerControl Edit/Routers.dat"));
        parser.saveFile(new File("routerData.dat"));
    }

}
