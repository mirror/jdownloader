//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.captcha.easy.load;

import jd.parser.html.Form;

public class LoadAgain {

    /**
     * @param args
     */
    public static void main(String[] args) {
        LoadImage li = LoadImage.loadFile("protect-it.org");
        try {
            // von einer anderen url laden für Plugins nützlich
            li.baseUrl = "http://protect-it.org/?id=224692747cc7f9e";
            System.out.println(li.load("protect-it.org").file);
            Form form = li.br.getForm(0);
            form.getInputField("pass").setValue("test");
            System.out.println(form);
            // System.out.println(form);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
