package jd.router;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
/**
 * Versuch, die Routers.dat auszulesen
 * 
 * @author astaldo
 *
 */
public class Parser {
    Vector splittedData = new Vector();

    public void parseFile(File file){
        int count=0;
        try {
            FileInputStream fis = new FileInputStream(file);
            while(true){
                parseSingleRouter(fis);
                count++;
            }
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }
        catch (IOException e)           { e.printStackTrace(); }
    }
    public void parseSingleRouter(FileInputStream fis) throws IOException{
        System.out.println("------------------------");
        addToVector(readInt(fis));
        addToVector(readInt(fis));
        addToVector(readByte(fis));

        addToVector(readNextString(fis));       
        addToVector(readLong(fis));

        for(int i=0;i<10;i++){
            addToVector(readNextString(fis));
        }
        int loop1=readInt(fis);
        System.out.println(loop1);
        for(int i=0;i<loop1;i++){
            addToVector(readNextString(fis));
            addToVector(readNextString(fis));
        }
        for(int i=0;i<5;i++){
             addToVector(readNextString(fis));   
        }
        
        byte loop2 = readByte(fis);
        System.out.println(loop2);
        for(int i=0;i<loop2;i++){
            addToVector(readNextString(fis));
            addToVector(readNextString(fis));
        }
       
        int loop3 = readInt(fis);
        System.out.println(loop3);
        for(int i=0;i<loop3;i++){
            addToVector(readNextString(fis));
            addToVector(readNextString(fis));
        }
        addToVector(readNextString(fis));
    }

    private String readNextString(FileInputStream fis)throws IOException{
        int length = readShort(fis);
        byte b[] = new byte[length];
        fis.read(b);
        return new String(b);
    }


    // InputStreams
    private byte readBuffer[] = new byte[8];
    private void addToVector(Object o){
        System.out.println(o);
        splittedData.add(o);
    }
    public byte readByte(InputStream in) throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return (byte)(ch);
    }
    public short readShort(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (short)((ch2 << 8) + (ch1 << 0));
    }
    private int readInt(InputStream in) throws IOException{
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }
    public final long readLong(InputStream in) throws IOException {
        in.read(readBuffer, 0, 8);
        return (((long)readBuffer[0] << 56) +
                ((long)(readBuffer[1] & 255) << 48) +
                ((long)(readBuffer[2] & 255) << 40) +
                ((long)(readBuffer[3] & 255) << 32) +
                ((long)(readBuffer[4] & 255) << 24) +
                ((readBuffer[5] & 255) << 16) +
                ((readBuffer[6] & 255) <<  8) +
                ((readBuffer[7] & 255) <<  0));
    }
}
