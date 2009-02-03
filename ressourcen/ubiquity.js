CmdUtils.CreateCommand({ 
  name: "jdownloader",
  icon: "http://jdownloader.org/_media/de/knowledge/wiki/jd_logo.png?w=16&h=16&cache=cache",
  homepage: "http://www.jdownloader.org/",
  author: { name: "scr4ve, JD-Team", email: "scr4ve@jdownloader.org"},
  license: "GPL",
  description: "Download files using JDownloader.",
  takes: {"page, tabs, selected": noun_arb_text},
  preview: function( pblock, type) {
    if(type.text.match("^t") || type.text.match("tabs"))
    {
      pblock.innerHTML = "Download all links from the content of every single opened tab.";

    }
    else if(type.text.match("^p") || type.text.match("page"))
    {
      pblock.innerHTML = "Download all links on this page.";      
    }
    else if(type.text.match("^s") || type.text.match("selected"))
    {
      pblock.innerHTML = "Download all links from your current selection.";
    }
    else
    {
    pblock.innerHTML = "<b>&bdquo;page&ldquo;</b> to download all links on this page<br />"+
                       "<b>&bdquo;tabs&ldquo;</b> to download all links from the content of your tabs<br />"+
                       "<b>&bdquo;selected&ldquo;</b> to download all links from your current selection";
    }
  },
  execute: function(type) {
      CmdUtils.log( Application.activeWindow.tabs);
    var toclip;
    if(type.text.match("^t") || type.text.match("tabs"))
    { //tabs
      Application.activeWindow.tabs.forEach(function(tab){
        toclip= toclip +  tab.document.body.innerHTML + " " + tab.uri.spec + " ";
      });
    }
    else if(type.text.match("^p") || type.text.match("page"))
    {  //page
      toclip = Application.activeWindow.activeTab.document.body.innerHTML + " " + Application.activeWindow.activeTab.uri.spec;
    }
    else if(type.text.match("^s") || type.text.match("selected"))
    {  //selected
      if(CmdUtils.getHtmlSelection() != "")
      {
        toclip = CmdUtils.getHtmlSelection();
      }
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