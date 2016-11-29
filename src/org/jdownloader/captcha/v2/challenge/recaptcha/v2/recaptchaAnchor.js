(function() {
	// does not work with psj 1.9.8

	// document.getElementsByClassName('recaptcha-checkbox-checkmark')[0].click();
//debugger;
	el = document.getElementsByClassName('recaptcha-checkbox-checkmark')[0];

	var ev = document.createEvent("MouseEvent");
	ev.initMouseEvent("click", true /* bubble */, true /* cancelable */, window, null, 23, 2, 0, 0, /* coordinates */
	false, false, false, false, /* modifier keys */
	0 /* left */, null);
	el.dispatchEvent(ev);
})()