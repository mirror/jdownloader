package jd.gui.skins.swt;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

public class PreferencesTab {
    public CTabItem tbPreferences;
    private Image im;
    private MainGui mainGui;
    public PreferencesTab(MainGui mainGui) {
        this.mainGui = mainGui;
        initpreferences();
    }
    private void initpreferences() {
        if(tbPreferences==null || tbPreferences.isDisposed()){
        tbPreferences = new CTabItem(mainGui.folder, SWT.CLOSE);
        tbPreferences.setText(JDSWTUtilities.getSWTResourceString("PreferencesTab.tbPreferences.name"));
        im = JDSWTUtilities.getImageSwt("preferences");
        ImageData  dt = im.getImageData();
        dt = dt.scaledTo(24, 24);
        im=new Image(mainGui.getDisplay(),dt);
        tbPreferences.setImage(im);
        mainGui.folder.setSelection(tbPreferences);
        tbPreferences.addDisposeListener(new DisposeListener(){

            public void widgetDisposed(DisposeEvent arg0) {
                im.dispose();          
            }
            
        });
        }
    }
}
