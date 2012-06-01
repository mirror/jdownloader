noun_type_jd = new CmdUtils.NounType( "Selections", ["page", "tabs", "this"]);

CmdUtils.CreateCommand({
  name: "jdownloader",
  icon: "http://jdownloader.org/_media/de/knowledge/wiki/jd_logo.png?w=16&h=16&cache=cache",
  homepage: "http://www.jdownloader.org/",
  author: { name: "scr4ve, JD-Team", email: "scr4ve@jdownloader.org"},
  license: "GPL",
  description: "Download files using JDownloader.",
  takes: {"page, tabs, this": noun_type_jd},
  preview: function( pblock, type) {
    switch(type.text)
    {
      case "tabs":
        pblock.innerHTML = "Download all links from the content of every single opened tab.";
        break;
      case "page":
        pblock.innerHTML = "Download all links on this page.";      
        break;
      case "this":
        pblock.innerHTML = "Download all links from your current selection.";
        break;
      default:
        pblock.innerHTML = "<b>&bdquo;page&ldquo;</b> to download all links on this page<br />" +
                        "<b>&bdquo;tabs&ldquo;</b> to download all links from the content of your tabs<br />"
                         +"<b>&bdquo;this&ldquo;</b> to download all links from your current selection";
        break;
    }
  },
  execute: function(type) {
    var toclip;
    switch(type.text)
    {
      case "tabs":
        Application.activeWindow.tabs.forEach(function(tab){    
          toclip = tab.uri.spec;
          for (var i = 0; i < tab.document.links.length; i++)
          {
            toclip += " " + tab.document.links.item(i).href;
          }
        });
        break;
      
      case "page":
        toclip  = CmdUtils.getDocument().URL;
        for (var i = 0; i < CmdUtils.getDocument().links.length; i++)
        {
          toclip += " " + CmdUtils.getDocument().links[i].href;
        }
        break;
      
      case "this":
        toclip = CmdUtils.getHtmlSelection();
        break;
        /*
        Not enough performance - jQuery is buggy, too, so not every link will get included.
        Thats why i switched to the easy version.
      
        CmdUtils.log(CmdUtils.getHtmlSelection());
        var links = jQuery(CmdUtils.getHtmlSelection()).find("a");
        if(links.length == 0)
        {
          toclip = CmdUtils.getHtmlSelection(); //Unfortunately the parser has some problems
          break;
        }
        toclip = ""; //Avoid "undefined"
        //Insert all links from the site */
        /*
        CmdUtils.log(links,CmdUtils.getDocument());
 
        for (var i = 0; i < links.length; i++)
        {
          FindCorrect: for (var j = 0; j < CmdUtils.getDocument().links.length; j++)
          {
            
            if(links.get(i).pathname == CmdUtils.getDocument().links[j].pathname)
            {
              
              toclip += " " + CmdUtils.getDocument().links[j].href;
              break FindCorrect;
            }
          }
        }
        */
    }
    //Show Feedback (might be positive or negative)
    if (typeof toclip != 'undefined')
    {
      //Copy to clipboard
      CmdUtils.copyToClipboard(toclip);
      displayMessage("Make sure that your Clipboard-Detection is turned on! (Or press \"Add\" in JD manually)");
    }
    else
    {
      displayMessage("Which links should i download, huh?");
    }
}
});


