//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.controlling.interaction;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Serializable;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse macht ein Backup der aktuellen Linkliste
 * 
 * @author JD-Team
 */
public class Backup extends Interaction implements Serializable {
    private static final String		backuppath		= JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/backup/";

    private static final String		links			= JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/links.dat";
    
    private static final String      NAME			= JDLocale.L("interaction.backup.name","Backup");
    
    /**
     * Unter diesen Namen werden die entsprechenden Parameter gespeichert
     * 
     */
    private static final long        serialVersionUID	= 4793649294489149258L;

    
    @Override
    public boolean doInteraction(Object arg) {
        try {
        	int size = 0;
        	if(getBooleanProperty("DLC", false)) {
        		size += 1;
        	}
        	if(getBooleanProperty("CONFIG", false)) {
        		File folder = new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/config/");
        		size += folder.list().length;
        	}
        	if(getBooleanProperty("LINK_DAT_COPY", false)) {
        		size += getIntegerProperty("LINK_DAT_NUMBER_COPY");
        	}
        	ProgressController progress = new ProgressController(JDLocale.L("interaction.backup.name","Backup"), size);
        	
        	File folderbackup = new File(backuppath);
        	if(!folderbackup.exists()) {
        		folderbackup.mkdirs();
        	}
        		
    		if(getBooleanProperty("DLC", false)) {
    			logger.info(JDLocale.L("interaction.backup.status.dlc", "Backup mit DLC"));
    			progress.setStatusText(JDLocale.L("interaction.backup.status.dlc", "Backup mit DLC"));
    			File backupcontainer = new File(getStringProperty("DLC_PATH"));
    			if(backupcontainer.exists()) {
    				backupcontainer.delete();
    			}
    			backupcontainer.createNewFile();
    			
    			JDController controller = JDUtilities.getController();
    			controller.saveDLC(backupcontainer);
    			progress.increase(1);
    		}
    		if(getBooleanProperty("LINK_DAT_COPY", false)) {
    			logger.info(JDLocale.L("interaction.backup.status.link", "Backup der links.dat"));
    			progress.setStatusText(JDLocale.L("interaction.backup.status.link", "Backup der links.dat"));
    			for(int i = getIntegerProperty("LINK_DAT_NUMBER_COPY"); i>1; i--) {
    				File f1 = new File(backuppath + "links_backup_" + (i-1) + ".dat");
    				File f2 = new File(backuppath + "links_backup_" + i + ".dat");
    				
    				if(f1.exists()) {
    	        		if(f2.exists()) {
    	            		f2.delete();
    	            	}
    	        		RandomAccessFile datei = new RandomAccessFile(backuppath + "links_backup_" + (i-1) + ".dat","r");
    	        		RandomAccessFile neudatei = new RandomAccessFile(backuppath + "links_backup_" + i + ".dat", "rw");
    	        		while (neudatei.length() < datei.length()) {
    	        			neudatei.write(datei.read());
    	        		}
    	        		datei.close();
    	        		neudatei.close();
    	        	}
    				progress.increase(1);
    			}
    			
    			File backup1 = new File(backuppath + "links_backup_1.dat");
    			if(backup1.exists()) {
            		backup1.delete();
            	}
            	RandomAccessFile datei = new RandomAccessFile(links,"r");
        		RandomAccessFile neudatei = new RandomAccessFile(backuppath + "links_backup_1.dat", "rw");
        		while (neudatei.length() < datei.length()) {
        			neudatei.write(datei.read());
        		}
        		datei.close();
        		neudatei.close();
        		progress.increase(1);
    		}
    		if(getBooleanProperty("CONFIG", false)) {
    			logger.info(JDLocale.L("interaction.backup.status.config", "Backup der Konfigurationsdatein"));
    			progress.setStatusText(JDLocale.L("interaction.backup.status.config", "Backup der Konfigurationsdatein"));
    			File folder = new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/config/");
    			File folderconfig = new File(backuppath + "config/");
    			if(folderconfig.exists()) {
    				folderconfig.delete();
    			}
    			folderconfig.mkdirs();
    			
    			String[] filenames = folder.list();
    			for(int i=0; i<filenames.length; i++) {
    				File pass = new File(backuppath + "config/" + filenames[i]);
        			if(pass.exists()) {
                		pass.delete();
                	}
        			
                	RandomAccessFile datei = new RandomAccessFile(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/config/" + filenames[i],"r");
            		RandomAccessFile neudatei = new RandomAccessFile(backuppath + "config/" + filenames[i], "rw");
            		while (neudatei.length() < datei.length()) {
            			neudatei.write(datei.read());
            		}
            		datei.close();
            		neudatei.close();
            		progress.increase(1);
    			}
    		}
    		
    		progress.finalize();
        }
        catch (Exception e) {
        	e.printStackTrace();
        	return false;
        }
               
        return true;
    }

	
    @Override
    public String getInteractionName() {
        return NAME;
    }

    
    @Override
    public void initConfig() {
    	config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this, "LINK_DAT_COPY", new String[] {""}, JDLocale.L("interaction.backup.linkcopy", "links.dat kopieren")));
    	config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, "LINK_DAT_NUMBER_COPY", JDLocale.L("interaction.backup.linkcopy.number", "Anzahl der Kopien"),1,50).setDefaultValue(5));
    	config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
    	config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this, "DLC", new String[] {""}, JDLocale.L("interaction.backup.dlc", "Als DLC-Container speichern")));
    	config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, this, "DLC_PATH", new String[] {"backup.dlc"}, JDLocale.L("interaction.backup.dlc.path", "Pfad zur DLC")));
    	config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
    	config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this, "CONFIG", new String[] {""}, JDLocale.L("interaction.backup.config", "Konfig speichern")));
    }

    
    @Override
    public void resetInteraction() {
        
    }

    
    @Override
    public void run() {}

    
    @Override
    public String toString() {
        return JDLocale.L("interaction.backup.toString","Backup der wichtigesten Datein");
    }

}
