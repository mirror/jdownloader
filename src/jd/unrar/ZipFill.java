package jd.unrar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ZipFill {
    public static void main(String[] argv) {
        if(argv.length!=2){
            System.out.println("Use: java ZipFill [file] [newsize]");
            System.exit(1);
        }
        File f = new File(argv[0]);
        if (!f.exists()) System.out.println("File not found: " + f);
        long size = f.length();
        System.out.println("Work on: " + f);
        f=new File(f.getParentFile(),(Integer.parseInt(argv[1])-size)+".fil");
        
        System.out.println("Current size: " + size);
        System.out.println("Append Bytes: " + (Integer.parseInt(argv[1])-size));
    
        ByteBuffer bbuf = ByteBuffer.allocate((int)(Integer.parseInt(argv[1])-size));    
        
        try {
        
            FileChannel wChannel = new FileOutputStream(f).getChannel();
       
            wChannel.write(bbuf);
        
            // Close the file
            wChannel.close();
        } catch (IOException e) {
        }
      
        
        System.out.println("filesize: " + f.length());
    }
}
