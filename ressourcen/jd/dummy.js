/*
Dummy des navigator Objects
*/

function Navigator(){ 
	this.appCodeName="Mozilla";
	this.appName="Mozilla";
	this.appVersion="3.1";
	this.cookieEnabled=true;
	this.language="en";
	this.platform="vista";
	this.userAgent="Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)";
	this.mimeTypes=[];
	this.plugins=[];
}

Navigator.prototype.javaEnabled=function(){
	return true;
}

/*
Location
*/

function Location(){ 
	this.hash ="";
	/*
	PLatzhalter werden von java ausgefüllt
	*/
	this.host ="%%%HOST%%%";
	this.hostname  ="%%%HOST%%%";
	this.href  ="%%%URL%%%";
	this.pathname  ="%%%URLPATH%%%";
	this.port ="%%%PORT%%%";
	this.protocol  ="%%%PROTOCOL%%%";
	this.search  ="%%%QUERY%%%";
}

Location.prototype.reload=function(){
	this.content+="[MISSING::+createAttribute"+arguments+"]";
}
Location.prototype.replace=function() {
	this.content+="[MISSING::+createAttribute"+arguments+"]";
}



/*
Dummy von document
*/
function Document()  { 
	this.content="";
	this.alinkColor="#000000";
	this.bgColor="#000000";
	this.charset= "ISO-8859-5"
	this.cookie =null;
	this.defaultCharset ="ISO-8859-5"
	this.fgColor="#000000";
	this.lastModified="";
	this.linkColor="#000000";
	this.referrer ="%%%REFERRER%%%";
	this.title ="";
	this.URL="%%%URL%%%";
	this.vlinkColor="#000000";
	this.location=new Location();
}
Document.prototype.write = function(value){ 
	this.content+=value;
}
Document.prototype.getMyOutput = function(){
	var c=this.content;
	this.content="";
	return c;
} 

Document.prototype.captureEvents=function(){
	this.content+="[MISSING::+createAttribute"+arguments+"]";
}
Document.prototype.close=function() {
	this.content+="[MISSING::+createAttribute"+arguments+"]";
}
Document.prototype.createAttribute=function(){
	this.content+="[MISSING::+createAttribute"+arguments+"]";
}
Document.prototype.createElement=function(){
	this.content+="[MISSING::+createAttribute"+arguments+"]";
}
Document.prototype.createTextNode=function() {
	this.content+="[MISSING::+createTextNode"+arguments+"]";
}
Document.prototype.getElementById=function() {
	this.content+="[MISSING::+getElementById"+arguments+"]";
}
Document.prototype.getElementsByName=function() {
	this.content+="[MISSING::+getElementsByName"+arguments+"]";
}
Document.prototype.getElementsByTagName=function() {
	this.content+="[MISSING::+getElementsByTagName"+arguments+"]";
}
Document.prototype.getSelection=function() {
	this.content+="[MISSING::+getSelection"+arguments+"]";
}
Document.prototype.handleEvent=function() {
	this.content+="[MISSING::+handleEvent"+arguments+"]";
}
Document.prototype.open=function() {
	this.content+="[MISSING::+open"+arguments+"]";
}
Document.prototype.releaseEvents=function() {
	this.content+="[MISSING::+releaseEvents"+arguments+"]";
}
Document.prototype.routeEvent=function() {
	this.content+="[[MISSING::+routeEvent"+arguments+"]";
}

Document.prototype.writeln=function(line) {
	this.content+=line+"\r\n";
}

/*
Dummy von Window
*/
function Window()  {
	this.onLoad=function() {
	};
}

function setInterval ( e,  t){
	eval(e);
}


//INIT
navigator= new Navigator();
document= new Document();
location= new Location();
window=new Window();
document.write(window.onLoad());