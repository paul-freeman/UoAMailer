<?php
session_start();

include_once 'Session.php';

$file = $_GET["file"];

if (!Session::get()->hasFileAccess($file))
	die("no access");

	header('Content-Description: File Transfer');
    header('Content-Type: '.mime_content_type($file));
    header('Content-Disposition: attachment; filename='.basename($file));
    header('Expires: 0');
    header('Cache-Control: must-revalidate');
    header('Pragma: public');
    header('Content-Length: ' . filesize($file));
    readfile($file); 

?>
