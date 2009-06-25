package jd;

import java.io.IOException;

import jd.http.Browser;

import org.tmatesoft.svn.core.SVNException;

public class Tester {

    public static void main(String[] args) throws SVNException, IOException {

    Browser br = new Browser();
    br.getPage("http://www.davros.org/misc/iso3166.html#existing");
   String[][] matches = br.getRegex("<TR>.*?<TD>(.*?)</TD>.*?<TD>(.*?)</TD>.*?<TD>(.*?)</TD>.*?<TD>+b</TD>.*?<TD>(.*?)</TD></TR>").getMatches();
   for(int i=0; i<matches.length;i++){
       System.out.println("COUNTRIES.put(\""+matches[i][0]+"\",new String[]{\""+matches[i][1]+"\",\""+matches[i][2]+"\"});");
   }
    }
}
