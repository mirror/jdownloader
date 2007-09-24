package jd.gui.simpleSWT;

import jd.utils.JDSWTUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

public class PreferencesTab {
    private static CTabItem tbPreferences;
    private static CTabFolder folder;
    private static Image im;
    public static void initpreferences() {
        if(tbPreferences==null || tbPreferences.isDisposed()){
        folder = MainGui.getFolder();
        tbPreferences = new CTabItem(folder, SWT.CLOSE);
        tbPreferences.setText(JDSWTUtilities.getSWTResourceString("PreferencesTab.tbPreferences.name"));
        im = JDSWTUtilities.getImageSwt("preferences");
        ImageData  dt = im.getImageData();
        dt = dt.scaledTo(24, 24);
        im=new Image(folder.getDisplay(),dt);
        tbPreferences.setImage(im);
        folder.setSelection(tbPreferences);
        tbPreferences.addDisposeListener(new DisposeListener(){

            public void widgetDisposed(DisposeEvent arg0) {
                im.dispose();          
            }
            
        });
        }
    }
}
