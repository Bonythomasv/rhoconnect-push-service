using System;
using System.Collections.Generic;
using System.Linq;
using System.ServiceProcess;
using System.Text;
using System.IO;
using Microsoft.Win32;
using System.Runtime.InteropServices;
using System.Threading;

namespace ANS
{
    static class Program
    {
        /// <summary>
        /// The main entry point for the application.
        /// </summary>
        [MTAThread]
        static void Main()
        {
            FileStream stream = null;
            try
            {
                stream = new FileStream("lock.txt", FileMode.OpenOrCreate, FileAccess.ReadWrite, FileShare.None);
            }
            catch
            {
                System.Diagnostics.Process.GetCurrentProcess().Kill();
            }
            
            ServiceBase[] ServicesToRun;
            ServicesToRun = new ServiceBase[] 
            { 
                new Service1() 
            };
            ServiceBase.Run(ServicesToRun);

            System.Net.ServicePointManager.DefaultConnectionLimit = 10;

            // Start client and server threads now
           /// ClientThread.Start();

          ///  while (true) Thread.Sleep(1000);


            if (stream != null)
                stream.Close();
        }
    }
}
