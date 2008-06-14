//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://gnu.org/licenses/>.


package jd.plugins.decrypt;  import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class EinsKhDe extends PluginForDecrypt {

static private String host = "1kh.de";

private String version = "1.0.0.0";
private Pattern patternSupported           = Pattern.compile("http://.*?1kh\\.de/(f/)?[0-9]*", Pattern.CASE_INSENSITIVE);
static private final Pattern patternFolder = Pattern.compile("<ul class=\"FolderHeader\">");
static private final Pattern patternPass   = Pattern.compile("Der Ordner ist Passwortgesch&uuml;tzt.");

//Testlinks: http://www.the-lounge.org/viewtopic.php?p=138217#p138217

public EinsKhDe() {
  super();
  steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
  currentStep = steps.firstElement();
}

@Override
public String getCoder() {
  return "b0ffed";
}

@Override
public String getHost() {
  return host;
}

@Override
public String getPluginID() {
  return host+"-"+version;
}

@Override
public String getPluginName() {
  return host;
}

@Override
public Pattern getSupportedLinks() {
  return patternSupported;
}

@Override
public String getVersion() {
  return version;
}

private String getSingleLink(String link) {

	RequestInfo reqinfosinglelink = null;
	try {
		reqinfosinglelink = HTTP.getRequest(new URL(link));
	} catch (MalformedURLException e) {
		
		e.printStackTrace();
	} catch (IOException e) {
		
		e.printStackTrace();
	}
	
	return (new Regex(reqinfosinglelink.getHtmlCode(), "<iframe name=\"pagetext\" height=\".*?\" frameborder=\"no\" width=\"100%\" src=\"(.*?)\"></iframe>").getFirstMatch()).toString();	
}

@Override
public PluginStep doStep(PluginStep step, String parameter) {
  if (step.getStep() == PluginStep.STEP_DECRYPT) {
      Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
      try {
          
          progress.setRange(1);
          URL url = new URL(parameter);
          //Erster Request, der entweder zur Seite führt oder zur Altersabfrage.    
          RequestInfo reqinfo = HTTP.getRequest(url);
          
          Matcher passmatcher = patternPass.matcher(reqinfo.getHtmlCode());
          
          
          if( passmatcher.find())  // = Passwort vorhanden
          {  
              logger.finest("1kh.de - Ordnerpasswort benötigt");
    
    
              String password = "";
              password = JDUtilities.getGUI().showUserInputDialog("1kh.de - Ordnerpasswort?");
                
              if(password == null) {
         	      step.setParameter(decryptedLinks);
         	      return null;
     	      }
    
              reqinfo = HTTP.postRequest(url, "Password=" + password + "&submit=weiter");
          }
          
          Matcher foldermatcher = patternFolder.matcher(reqinfo.getHtmlCode());
          
          if(foldermatcher.find())
          {
        	  logger.info("foldermatch");
              ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "id=\"FileDownload_°\"");
              logger.info("size: " + links.size());
              
              progress.setRange(links.size());
              
              for(int i=0; i<links.size(); i++) {
            	  //logger.info("http://1kh.de/" + links.get(i).get(1));
                  decryptedLinks.add(this.createDownloadlink(getSingleLink("http://1kh.de/" + links.get(i).get(0))));
                  progress.increase(1);
              }        	  
          }
          else
          {
              progress.increase(1);
              decryptedLinks.add(this.createDownloadlink(
            		  new Regex(reqinfo.getHtmlCode(), 
            				  "<iframe name=\"pagetext\" height=\".*?\" frameborder=\"no\" width=\"100%\" src=\"(.*?)\"></iframe>")
            		  .getFirstMatch()));        	  
          }

          // Decrypt abschliessen

          step.setParameter(decryptedLinks);
      } catch (IOException e) {
          e.printStackTrace();
      }
  }
  return null;
}
@Override
public boolean doBotCheck(File file) {
  return false;
}
}