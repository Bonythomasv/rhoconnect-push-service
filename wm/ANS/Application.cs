// <copyright file="Application.cs" company="Symbol Technologies, Inc.">
//     Company (C) Symbol Technologies, Inc.
// </copyright>

namespace ANS
{
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Diagnostics;

    /// <summary>
    /// The class implements service like application behavior.
    /// </summary>
    public class Application : ManagedService.ServiceApplication
    {
        /// <summary>
        /// Keeps static instance of the singleton class.
        /// </summary> 
        private static Application instance = new Application();

        /// <summary>
        /// Prevents a default instance of the <see cref="Application" /> class from being created.
        /// Initializes default values of the class instance.
        /// </summary>
        private Application()
        {
            // Register service with unique identifier
            // uncomment this to enable debug output
            /*
            System.IO.FileStream myTraceLog = new 
            System.IO.FileStream("myTraceLog.txt", 
            System.IO.FileMode.OpenOrCreate);
            // Creates the new trace listener.
            System.Diagnostics.TextWriterTraceListener myListener = 
                new System.Diagnostics.TextWriterTraceListener(myTraceLog);
            Debug.Listeners.Add(myListener);
            */
            Debug.WriteLine("Starting the Push Service, before setting it is : " +  System.Net.ServicePointManager.DefaultConnectionLimit + " at " + DateTime.Now.ToString());
            this.ServiceGuid = new Guid(Application.Guid);

            System.Net.ServicePointManager.DefaultConnectionLimit = 10;

            // Start client and server threads now
            ClientThread.Start();
            ///ServerThread.Start();
        }

        /// <summary>
        /// Gets application service instance.
        /// </summary>
        public static Application Instance
        {
            get { return instance; }
        }

        /// <summary>
        /// Gets application service guid
        /// </summary>
        public static string Guid
        {
            get { return Properties.Resources.ServiceGuid; }
        }
    }
}
