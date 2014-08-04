<?php

include_once 'Session.php';


class JSONHandler {
	public function call($jsonArray, $jsonRPCId) {
		return false;
	}

	public static function makeJSONRPCReturn($id, $result) {
		$array = array('jsonrpc' => "2.0", 'result' => $result, 'id' => $id);
		return json_encode($array);
	}
}


?>
