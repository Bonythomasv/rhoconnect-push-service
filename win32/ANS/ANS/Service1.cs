using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Diagnostics;
using System.Linq;
using System.ServiceProcess;
using System.Text;

namespace ANS
{
    public partial class Service1 : ServiceBase
    {
        public Service1()
        {
            InitializeComponent();
        }

        protected override void OnStart(string[] args)
        {
            System.Net.ServicePointManager.DefaultConnectionLimit = 10;

            // Start client and server threads now
            ClientThread.Start();
        }

        protected override void OnStop()
        {
        }
    }
}
