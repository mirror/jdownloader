function loadContent(hash) {
	var subd = "";
	if(hash.split("?=")[1]) {
		var splitted = hash.split("?=");
		var hash = 	splitted[0];
		var subd = splitted[1]
	}
	if(!hash) { hash = "dashboard"; }
	$(".active").removeClass("active");
	$("#nav_"+hash).addClass("active");
	TemplateAjax(themepath+"pages/"+ hash +".html?mode="+subd,"#content","html");
}
function start() {
	$.history.init(loadContent);
	if(!location.href.split("#")[1]) {
		loadContent("dashboard");
	}
}
