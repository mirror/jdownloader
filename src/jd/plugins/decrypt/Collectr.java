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
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Collectr extends PluginForDecrypt {

static private String host = "collectr.net";

private String version = "1.2.0.0";
private Pattern patternSupported = Pattern.compile("http://.*?collectr\\.net/out/[0-9]*[/]{0,1}[\\d]*", Pattern.CASE_INSENSITIVE);
static private final Pattern patternAb18 = Pattern.compile("Hast du das 18 Lebensjahr bereits abgeschlossen\\?");

//Testlinks: http://collectr.net/out/756338/steelwarez.com   (Als Ab-18 markiert)
//           http://collectr.net/out/376910/sceneload.to     (Keine Alterskontrolle)
//
//Erkennung auch für:
//           http://collectr.net/out/376910/
//           http://collectr.net/out/376910
public Collectr() {
  super();
  //steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
  //currentStep = steps.firstElement();
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

@Override
public ArrayList<DownloadLink> decryptIt(String parameter) {
  //if (step.getStep() == PluginStep.STEP_DECRYPT) {
      ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
      try {
          
          progress.setRange(1);
          URL url = new URL(parameter);
          //Erster Request, der entweder zur Seite führt oder zur Altersabfrage.    
          RequestInfo reqinfo = HTTP.getRequest(url);
          
          
          Matcher matcher = patternAb18.matcher(reqinfo.getHtmlCode());
          
          if( matcher.find())  // = Datei ab 18
          {  
              logger.finest("CollectrDecrypt - Angeforderte Datei(en) ab 18.");

              if(JDUtilities.getGUI().showConfirmDialog(
                    "Du hast ein Suchergebnis der Kategorie \"Erotik\" ausgewählt.\n" +
                    "Diese Suchergebnisse verweisen meist auf Internetseiten mit nicht jugendfreien Inhalten.\n" +
                    "Da uns der Jugendschutz jedoch sehr am Herzen liegt, \n" +
                    "bitten wir dich hier um die ausdrückliche Bestätigung deiner Volljährigkeit!\n\n\n" +
                    "Hast du das 18 Lebensjahr bereits abgeschlossen?\n  "))
              {
                  logger.finest("Nutzer hat das 18te Lebensjahr erreicht. Decrypten wird fortgesetzt.");
                  reqinfo = HTTP.postRequest(url, "o18=true"); 
              }
              else
              {  
                  logger.finest("Nutzer hat das 18te Lebensjahr noch nicht erreicht. Abbruch.");
                  return null;
              }
          }
          else
          {
              logger.finest("CollectrDecrypt - Angeforderte Datei(en) ohne Jugendschutz."); 
          }
          
          String link = new Regex(reqinfo.getHtmlCode(), "<iframe id=\"displayPage\" src=\"(.*?)\" name=\"displayPage\"").getFirstMatch();
          progress.increase(1);
          decryptedLinks.add(this.createDownloadlink(link));

          // Decrypt abschliessen

          //step.setParameter(decryptedLinks);
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