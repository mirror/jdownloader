<?php
define( 'BASE_DIR', "d:\\Fahrschule\\xampp\\htdocs\\jd\\"); 
if(!$_POST["hersteller"] || !$_POST["name"] || !$_POST["script"])
	die("2");
if(!is_dir(BASE_DIR.$_POST["hersteller"]))
	{
	mkdir( BASE_DIR.$_POST["hersteller"], 0777 ); 
	}
	
$xml = new XmlWriter();
$xml->openMemory();
$xml->startDocument( '1.0" encoding="UTF-8' );
$xml->startElement('router');
$xml->writeElement('hersteller',$_POST["hersteller"]);
$xml->writeElement('name',$_POST["name"]);
$xml->writeElement('script',base64_decode($_POST["script"]));
$xml->endDocument();
$x = 0;
do{
$x++;
if(x>10)
	die("2");
$file = BASE_DIR.$_POST["hersteller"]."\\".rand().rand();
}
while(is_file($file));
$fh = fopen($file, 'w') or die("2");
fwrite($fh, $xml->outputMemory());
fclose($fh);
print ("1");


?>