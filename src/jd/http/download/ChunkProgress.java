package jd.http.download;

import java.io.Serializable;

public class ChunkProgress implements Serializable {

    public long getEnd() {
        return end;
    }

    public long getStart() {
        return start;
    }

    /**
     * 
     */
    private static final long serialVersionUID = 9203094151658724279L;
    private long end=0;
    private long start=0;

    public void setStart(long start) {
       this.start=start;
        
    }

    public void setEnd(long end) {
       this.end=end;
        
    }
   public String toString(){
       return "Chunk "+start+" - "+end;
       
       
   }
}
