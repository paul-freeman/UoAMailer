<?php

include_once 'JSONHandler.php';
include_once 'Session.php';


class UploadHandler extends JSONHandler {
	public function call($jsonArray, $jsonRPCId) {
		if (strcmp($jsonArray['method'], "upload") != 0)
			return false;
		$params = $jsonArray["params"];
		if (!isset($params['files']))
			return false;

		if (!$this->upload($params['files'], $jsonRPCId))
			return $this->makeJSONRPCReturn($jsonRPCId, array('error' => -1, 'message' => "failed to upload data"));
			
		return $this->makeJSONRPCReturn($jsonRPCId, array('error' => 0, 'files' => $_FILES));
	}

	private function upload($files, $jsonRPCId) {
		$userName = Session::get()->getUser();
		if ($userName == "")
			return false;
		if (!file_exists($userName))
			mkdir($userName);
		$now = date('Y-m-d_H-i-s');
		$dir = $userName."/".$now;
		if (!file_exists($dir))
			mkdir($dir);

		foreach ($files as $file) {
			move_uploaded_file($_FILES[$file]["tmp_name"], $dir."/".$file);
		}

		return true;
	}
} 

?>
