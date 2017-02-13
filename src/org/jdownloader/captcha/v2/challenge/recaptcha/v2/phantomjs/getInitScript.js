(function() {

	scripts = document.getElementsByTagName('script');
	for (i = 0; i < scripts.length; i++) {
		index = scripts[i].innerHTML.indexOf('recaptcha.anchor.Main.init');
		console.log("i " + i + " " + scripts[i].innerHTML);
		if (index > -1) {
			return scripts[i].innerHTML;
		}
	}

	throw "Init Script not Found";
})()