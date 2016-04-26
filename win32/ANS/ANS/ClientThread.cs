// <copyright file="ClientThread.cs" company="Symbol Technologies, Inc.">
//     Company (C) Symbol Technologies, Inc.
// </copyright>

namespace ANS
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.IO;
    using System.Linq;
    using System.Messaging;
    using System.Text;
    using System.Threading;
    using System.Xml.Serialization;

    using Newtonsoft.Json;

    /// <summary>
    /// The class implements client service communication.
    /// The class starts thread that constantly polls for new messages.
    /// </summary>
    public static class ClientThread
    {
        /// <summary>
        /// Keeps readonly queue used for clients registrations/
        /// </summary>
        private static MessageQueue queue = null;//new MessageQueue(".\\Private$\\" + Properties.Resources.ServiceGuid);

        private static List<ServerThread> serverThreads = new List<ServerThread>();

        /// <summary>
        /// Keeps instance of the client thread.
        /// </summary>
        private static Thread thread = new Thread(Run);

        /// <summary>
        /// Starts a client thread.
        /// </summary>
        public static void Start()
        {
            if (!MessageQueue.Exists(".\\Private$\\" + Properties.Resources.ServiceGuid))
            {
                queue = MessageQueue.Create(".\\Private$\\" + Properties.Resources.ServiceGuid);
                queue.SetPermissions("Everyone", MessageQueueAccessRights.FullControl);
            }
            else
                queue = new MessageQueue(".\\Private$\\" + Properties.Resources.ServiceGuid);

            //queue.Formatter = new BinaryMessageFormatter();
            
            thread.Start();
        }

        /// <summary>
        /// Executes thread that reads messages from the incoming client queue.
        /// Based on the message contents requests registration or deregistration from the server thread.
        /// </summary>
        private static void Run()
        {
            for (int i = 0; i < Settings.Instance.Servers.Count(); ++i)
            {
                serverThreads.Add(new ServerThread(Settings.Instance, i));
            }

            // TODO: Add extended error handling
            // TODO: Test for valid symbols in the names
            // TODO: Implement graceful shutdown
            while (true)
            {
                checkForMessage();    
            }
        }

        public static void checkForMessage()
        {
            ///if (Settings.Instance.InstanceId == null) continue;
            Message message = queue.Receive();

            var reader = new StreamReader(message.BodyStream);
            var data = reader.ReadToEnd();

           // if (result == ReadWriteResult.OK)
           /// {
           // string data = (String)message.Body;//Encoding.UTF8.GetString(message.Body, 0, message.MessageBytes.GetLength(0));
                AppInfo ai = JsonConvert.DeserializeObject<AppInfo>(data);
                Debug.WriteLine("Received Command: " + data + ", " + DateTime.Now.ToString());

                // find if we already have ai with matching key and use it
                ai = Settings.Instance.findAndUpdateAppInfo(ai);

                Debug.WriteLine("AppInfo: " + ai.User + ", " + ai.Pass + ", at " + DateTime.Now.ToString());

                //int index = Settings.Instance.getServerIndexByToken(ai.Token);
                int index = Settings.Instance.getServerIndexByUrl(ai.ServerUrl);
                if (index == -1)
                {
                    Settings.Instance.Servers.Add(new ServerInfo(ai.ServerUrl, ai.User, ai.Pass));
                    Settings.Save(Settings.Instance);
                    serverThreads.Add(new ServerThread(Settings.Instance, Settings.Instance.Servers.Count() - 1));
                    index = serverThreads.Count() - 1;
                }

                if (ai.Command.Equals("Register"))
                {
                    serverThreads[index].Register(ai);
                }
                else if (ai.Command.Equals("Unregister"))
                {
                    serverThreads[index].Unregister(ai);
                }
           // }
        }
    }
}
