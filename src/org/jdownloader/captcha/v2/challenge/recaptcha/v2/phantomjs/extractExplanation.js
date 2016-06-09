(function() {

	descs = document.getElementsByClassName('rc-imageselect-desc-no-canonical');
	if (!descs || descs.length == 0) {
		descs = document.getElementsByClassName('rc-imageselect-desc');
	}

	return {description: descs[0].innerText, tag:descs[0].childNodes[1].innerHTML };

})()