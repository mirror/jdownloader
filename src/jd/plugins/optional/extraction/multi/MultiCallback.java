package jd.plugins.optional.extraction.multi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * Gets the decrypted bytes and writes it into the file.
 * 
 * @author botzi
 *
 */
class MultiCallback implements ISequentialOutStream {
    private FileOutputStream fos;
    private Multi multi;
    private CRC32 crc;
//    private boolean shouldCrc = false;
    private int priority;
    
    MultiCallback(File file, Multi multi, int priority, boolean shouldCrc) throws FileNotFoundException {
        this.multi = multi;
//        this.shouldCrc = shouldCrc;
        this.priority = priority;
        
        fos = new FileOutputStream(file, true);
        
//        if(shouldCrc) {
//            crc = new CRC32();
//        }
    }
    
    public int write(byte[] data) throws SevenZipException {
        try {
            fos.write(data);
            
            multi.getArchive().setExtracted(multi.getArchive().getExtracted() + data.length);

            multi.updatedisplay();
            
            if(priority > 0) {
                try {
                    if(priority == 1) {
                        Thread.sleep(100);
                    } else if(priority == 2) {
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
//            if(shouldCrc) {
//                crc.update(data);
//            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return data.length;
    }
    
    /**
     * Retruns the computed CRC.
     * 
     * @return The computed CRC.
     */
    String getComputedCRC() {
        return Long.toHexString(crc.getValue());
    }
}