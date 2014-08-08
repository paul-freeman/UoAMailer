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

		$groupmembers = null;
		if (isset($params['groupMembers']))
			$groupmembers = $params['groupMembers'];

		if (!$this->upload($params['files'], $groupmembers, $jsonRPCId))
			return $this->makeJSONRPCReturn($jsonRPCId, array('error' => -1, 'message' => "failed to upload data"));
			
		return $this->makeJSONRPCReturn($jsonRPCId, array('error' => 0, 'files' => $_FILES));
	}

	private function upload($files, $groupmembers, $jsonRPCId) {
		$userName = Session::get()->getUser();
		if ($userName == "")
			return false;
		$now = date('Y-m-d_H-i-s');
		$dir = "uploads/".$userName."/".$now;
		if (!file_exists($dir))
			mkdir($dir, 0777, true);

		foreach ($files as $file) {
			move_uploaded_file($_FILES[$file]["tmp_name"], $dir."/".$_FILES[$file]["name"]);
		}

		if ($groupmembers !== null) {
			$groupmembersFile = $dir."/groupMembers";
			$fileContent = "";
			foreach ($groupmembers as $member)
				$fileContent = $fileContent.$member."\n";
			file_put_contents($groupmembersFile, $fileContent); 
		}

		return true;
	}
} 

?>
