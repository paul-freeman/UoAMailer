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
		$unixUser = get_current_user();

		$userName = Session::get()->getUser();
		if ($userName == "")
			return false;
		$now = date('Y-m-d_H-i-s');
		$dir = "uploads/".$userName."/".$now;
		if (!file_exists($dir)) {
			mkdir($dir, 0777, true);
		}
		//shell_exec('/usr/bin/setfacl -R -m "default:user:'.$unixUser.':rwx" uploads');
		shell_exec('/usr/bin/setfacl -R -m "user:'.$unixUser.':rwx" uploads');

		foreach ($files as $file) {
			$target = $dir."/".$_FILES[$file]["name"];
			move_uploaded_file($_FILES[$file]["tmp_name"], $target);
			shell_exec('/usr/bin/setfacl -m "user:'.$unixUser.':rwx" '.$target);
		}

		if ($groupmembers !== null) {
			$groupmembersFile = $dir."/groupMembers";
			$fileContent = "";
			foreach ($groupmembers as $member)
				$fileContent = $fileContent.$member."\n";
			file_put_contents($groupmembersFile, $fileContent);
			shell_exec('/usr/bin/setfacl -m "user:'.$unixUser.':rwx" '.$groupmembersFile);
		}

		$link = "http".(!empty($_SERVER['HTTPS'])?"s":"")."://".$_SERVER['SERVER_NAME'].dirname($_SERVER['REQUEST_URI']);
		$link = $link."/download.php?dir=".$dir;
		mail($groupMembers, $link);
		return true;
	}

	private function mail($groupMembers, $link) {
		$to = "";
		foreach ($groupMembers as $member) {
			$to = $to.$member."@uniauckland.ac.nz, ";
		}
		$subject = "Experiment data from the physics lab.";
		$message = "The experiment data has been uploaded successfully and can be downloaded here:\r\n".$link;
		$headers = "From: Auckland uni physics lab\r\n" .
			"X-Mailer: PHP/" . phpversion();

		mail($to, $subject, $message, $headers);
	}
} 

?>
