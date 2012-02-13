package org.jdownloader.extensions.neembuu.newconnectionprovider;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import jd.http.URLConnectionAdapter;
import neembuu.vfs.connection.AbstractConnection;
import neembuu.vfs.connection.NewConnectionParams;
import org.jdownloader.extensions.neembuu.JD_Utils;

/**
 *
 * @author Shashank Tulsyan
 */
public final class JD_HTTP_Connection extends AbstractConnection{
    
    private final JD_HTTP_Download_Manager jddm;
    //private HTTPConnection httpConnection;
    private URLConnectionAdapter urlca;
    
    public JD_HTTP_Connection(JD_HTTP_Download_Manager jddm, NewConnectionParams cp) {
        super(cp);
        this.jddm = jddm;
    }
    
    
    @Override
    protected final void abortImpl() {
        urlca.disconnect();
    }
   
    @Override
    protected final void connectAndSupplyImpl() throws IOException{
        int firstByte = -1;
        int retriesMade = 0;
        InputStream is = null;
        
        while(firstByte==-1){
            urlca = JD_Utils.copyConnection(
                    jddm.jdds.getDownloadLink(),
                    jddm.jdds.getDownloadInterface(),
                    jddm.jdds.getPluginForHost(),
                    cp.getOffset(), 
                    jddm.jdds.getBrowser(),
                    jddm.jdds.getURLConnectionAdapter());
            if(urlca!=null){
                is = urlca.getInputStream();
                firstByte = is.read();
            }else {
                firstByte = -1;
            }
            if(firstByte!=-1){
                // when the first byte is send to {@link neembuu.vfs.connection.AbstractConnection#write }
                // cp.getTransientConnectionListener().success is called
            }else if(retriesMade>10){
                cp.getTransientConnectionListener().failed(new IllegalStateException("EOF"),cp);
                return;
            }else {
                retriesMade++;
                cp.getTransientConnectionListener().reportNumberOfRetriesMadeSoFar(retriesMade);
            }
        }
        
        try{
            int read = 0;
            byte[]b=new byte[1024];
            b[0]=(byte)firstByte;
            read = is.read(b,1,b.length-1);
            if(read!=-1)read+=1;
            while(read!=-1){
                write(b,0,read);
                read=is.read(b);
            }
        }catch(Exception e){
            downloadThreadLogger.log(Level.SEVERE, "Connection terminated", e);
        }
    }
    
}
