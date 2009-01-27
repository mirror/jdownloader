package jd.http.download;

import java.io.File;
import java.io.Serializable;

public class DownloadProgress implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7415576592826692024L;
    private File file;
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ChunkProgress[] getChunks() {
        return chunks;
    }
    public String toString(){
        String ret=""+file+"\r\n";
        for(int i=0; i<chunks.length;i++){
            ret+=i+": "+chunks[i]+"\r\n";
        }
        return ret;
       
    }
    private ChunkProgress[] chunks;
    private int i = 0;

    public DownloadProgress(File outputFile) {
        this.file = outputFile;
    }

    public void reset(int num) {
        i = 0;
        this.chunks = new ChunkProgress[num];

    }

    public void add(ChunkProgress cp) {
        chunks[i++] = cp;
    }
}
