package jd.gui.simpleSWT;
import java.io.Serializable;
public class GUIConfig implements Serializable {
    
    public static final long serialVersionUID = 1L;
    public int[] DownloadColumnWidht = new int[]{200, 160, 160, 70, 70, 56, 70};
    public int[] DownloadColumnOrder = new int[]{0, 1, 2, 3, 4, 5, 6};
    public int[] DownloadColumnWidhtMaximized;
    public int[] GUIsize = new int[] {800, 500};
    public boolean isMaximized = false;
    public String btOpenFile;
    public String save;
    public boolean[] warnings = new boolean[jd.gui.simpleSWT.SWTWarnings.count];
}
