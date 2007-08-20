package jd.gui.skins.simple;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.JDUtilities;
import jd.plugins.DownloadLink;
/**
 * Diese Klasse soll, abhängig von einem DownloadLink die Geschwindigkeit messen
 * 
 * @author astaldo
 *
 */
public class Speedometer extends JPanel{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3359577498230076558L;
    private DownloadLink downloadLink = null;
    private JLabel lblDownloadName  = new JLabel("0");
    private JLabel lblDownloadSize  = new JLabel("0");
    private JLabel lblDownloadSpeed = new JLabel("0");
    
    public Speedometer(){
        setLayout(new GridBagLayout());
        JDUtilities.addToGridBag(this, new JLabel("Datei:"),           0, 0, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, new JLabel("Größe:"),           0, 1, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, new JLabel("Geschwindigkeit:"), 0, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, lblDownloadName,                1, 0, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, lblDownloadSize,                1, 1, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, lblDownloadSpeed,               1, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    public void setDownloadLink(DownloadLink downloadLink){
        this.downloadLink = downloadLink;
        lblDownloadName.setText(downloadLink.getName());
        if (downloadLink.getDownloadLength()>0)
            lblDownloadSize.setText(Integer.toString(downloadLink.getDownloadLength()));
    }

    private class SpeedometerRefresher extends Thread{
        private final int REFRESH_TIME = 1000;
        private int downloadedBytes=0;
        
        public SpeedometerRefresher(){
            super("JD-SpeedometerRefresher");
        }
        
        public void run(){
            long differenceBytes;
            long differenceTime;
            long start,end;
            
            while(downloadLink!=null && downloadLink.isInProgress()){
                start = System.currentTimeMillis();
                downloadedBytes = downloadLink.getDownloadedBytes();
                try {
                    Thread.sleep(REFRESH_TIME);
                    end = System.currentTimeMillis();
                    differenceBytes = downloadLink.getDownloadedBytes()-downloadedBytes;
                    downloadedBytes += differenceBytes;
                    differenceTime = end-start;
                    System.out.println(differenceBytes+"bytes in "+differenceTime+" ms");
                    
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
