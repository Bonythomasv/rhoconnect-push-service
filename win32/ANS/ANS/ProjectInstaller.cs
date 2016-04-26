using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Configuration.Install;
using System.Linq;
using System.Configuration.Install;
using System.ServiceProcess;

namespace ANS
{
    [RunInstaller(true)]
    public partial class ProjectInstaller : System.Configuration.Install.Installer
    {
        public ProjectInstaller()
        {
            InitializeComponent();
          //  this.AfterInstall += new InstallEventHandler(serviceInstaller1_AfterInstall);
        }

        private void serviceInstaller1_Committed(object sender, System.Configuration.Install.InstallEventArgs e)
        {
            var serviceInstaller = sender as ServiceInstaller;
            // Start the service after it is installed.
            if (serviceInstaller != null && serviceInstaller.StartType == ServiceStartMode.Automatic)
            {
                var serviceController = new ServiceController(serviceInstaller.ServiceName);
                serviceController.Start();
            }
        }

     /*   private void serviceInstaller1_AfterInstall(object sender, InstallEventArgs e)
        {
            /*using (ServiceController sc = new ServiceController(serviceInstaller1.ServiceName))
            {
                sc.Start();
            }*/
        //}
    }
}
