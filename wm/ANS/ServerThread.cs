// <copyright file="ServerThread.cs" company="Symbol Technologies, Inc.">
//     Company (C) Symbol Technologies, Inc.
// </copyright>

namespace ANS
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.IO;
    using System.Linq;
    using System.MessagingCE;
    using System.Text;
    using System.Threading;
    using System.Xml.Serialization;

    using Newtonsoft.Json;

    /// <summary>
    /// The class implements server service communication.
    /// The class starts thread that constantly polls for new messages.
    /// </summary>
    public class ServerThread
    {
        /// <summary>
        /// Keeps settings instance.
        /// </summary> 
        private Settings settings;

        private int serverIndex;

        /// <summary>
        /// Keeps object lock used to protect access to data from different threads.
        /// </summary>
        private object thisLock = new object();
        
        /// <summary>
        /// Keeps dictionary with writeonly queues, each queue has a key (appname-username).
        /// </summary> 
        private Dictionary<string, P2PMessageQueue> queues;
        //settings.Queues.ToDictionary(q => q.GetKey(), q => new P2PMessageQueue(false, q.GetKey()));
        
        /// <summary>
        /// Keeps server thread instance.
        /// </summary>
        private Thread thread;

        /// <summary>
        /// Starts server thread.
        /// </summary>
        public ServerThread(Settings _settings, int _serverIndex)
        {
            settings = _settings;
            serverIndex = _serverIndex;
            queues = settings.Servers[serverIndex].Queues.ToDictionary(q => q.GetKey(), q => new P2PMessageQueue(false, q.GetKey()));
            thread = new Thread(Run);
            thread.Start();
        }

        /// <summary>
        /// Adds a queue with the specified guid/user if there is no queue with the same guid/user, otherwise deletes one.
        /// </summary>
        /// <param name="ai">An instance of the AppInfo class that totally describes the specific queue.</param>
        public void Register(AppInfo ai)
        {
            Http http = new Http(serverIndex);
            string key = ai.GetKey();

            // TODO: Add error handling
            lock (thisLock)
            {
                if (queues.ContainsKey(key))
                {
                    int index = Settings.Instance.getServerIndexByToken(ai.Token);
                    if (index != -1)
                        ai.Pass = settings.getUserPassByToken(ai.Token);
                    Debug.WriteLine("Entering FetchRegistrationToken at " + DateTime.Now.ToString()); 
                    ai.Token = http.FetchRegistrationToken(settings.Servers[serverIndex].InstanceId, settings.Servers[serverIndex].Cookie, ai.User, ai.Pass, ai.Guid, ai.Session);
                    
                    Send(queues[key], new OutgoingMessage { Action = "Updated", Token = ai.Token });
                    settings.Servers[serverIndex].Queues.Remove(settings.Servers[serverIndex].Queues.First(q => key.Equals(q.GetKey())));
                    settings.Servers[serverIndex].Queues.Add(ai);
                    Debug.WriteLine("Registration is updated for queue: " + key + " at " + DateTime.Now.ToString());
                }
                else
                {
                    settings.Servers[serverIndex].ANSServerURL = ai.ServerUrl;

                    if (settings.Servers[serverIndex].InstanceId == null)
                    {
                        settings.Servers[serverIndex].InstanceId = http.FetchInstanceId(ai);
                    }

                    if (settings.Servers[serverIndex].Cookie == null)
                    {
                        settings.Servers[serverIndex].Cookie = http.FetchInstanceCookie(settings.Servers[serverIndex].InstanceId, ai);
                    }

                    Settings.Save(settings);

                    ai.Token = http.CreateRegistrationToken(settings.Servers[serverIndex].InstanceId, settings.Servers[serverIndex].Cookie, ai.User, ai.Pass, ai.Guid, ai.Session);
                    queues.Add(key, new P2PMessageQueue(false, key));
                    Send(queues[key], new OutgoingMessage { Action = "Created", Token = ai.Token });

                    settings.Servers[serverIndex].Queues.Add(ai);

                    Debug.WriteLine("Added queue: " + key);
                }

                Settings.Save(settings);
            }
        }


        public void Unregister(AppInfo ai)
        {
            Http http = new Http(serverIndex);
            string key = ai.GetKey();

            // TODO: Add error handling
            lock (thisLock)
            {
                if (queues.ContainsKey(key))
                {
                    int index = Settings.Instance.getServerIndexByToken(ai.Token);
                    if (index != -1)
                        ai.Pass = settings.getUserPassByToken(ai.Token);

                    http.DeleteRegistrationToken(settings.Servers[serverIndex].InstanceId, settings.Servers[serverIndex].Cookie, ai.User, ai.Pass, ai.Guid, ai.Session);

                    try
                    {
                        Send(queues[key], new OutgoingMessage { Action = "Deleted" });
                    }
                    catch (Exception)
                    {
                        Debug.WriteLine("Error during sendmessage - queue will be removed.");
                    }
                    queues.Remove(key);
                    settings.Servers[serverIndex].Queues.Remove(settings.Servers[serverIndex].Queues.First(q => key.Equals(q.GetKey())));
                    // reset the Server's Last Message Id , if no queues are associated with it
                    if (settings.Servers[serverIndex].Queues.Count == 0)
                    {
                        settings.Servers[serverIndex].LastMessageId = -1;
                    }

                    Debug.WriteLine("Removed queue: " + key);
                }

                Settings.Save(settings);
            }
        }

        /// <summary>
        /// Creates new write only message queue for any existing guid.
        /// </summary>
        public void LoadQueues()
        {
            lock (thisLock)
            {
                foreach (AppInfo ai in settings.Servers[serverIndex].Queues)
                {
                    queues.Add(ai.GetKey(), new P2PMessageQueue(false, ai.GetKey()));
                }
            }
        }

        /// <summary>
        /// Sends the supplied message to the specified outgoing queue.
        /// Acess to the queue should be protected with the lock.
        /// </summary>
        /// <param name="queue">A queue to send to.</param>
        /// <param name="om">A message to send.</param>
        private void Send(P2PMessageQueue queue, OutgoingMessage om)
        {
            string data = JsonConvert.SerializeObject(om);
            Message message = new Message(Encoding.UTF8.GetBytes(data), false);
            ReadWriteResult result = queue.Send(message, 0);
        }

        /// <summary>
        /// Runs server thread.
        /// Perform server polling for new requests.
        /// </summary>
        private void Run()
        {
            Debug.WriteLine("Starting Server Thread with ID: " + serverIndex + " at " + DateTime.Now.ToString());
            Http http = new Http(serverIndex);

            while (settings.Servers[serverIndex].Cookie == null)
                Thread.Sleep(1000);

            while (true)
            {
                bool receiveNow = false;
                lock (thisLock)
                {
                    receiveNow = (queues.Values.Count > 0);
                }
                // nothing to do - wait
                if (!receiveNow)
                {
                    Thread.Sleep(http.fetchMessageTimeoutRetryDelay);
                    continue;
                }
                else
                {
                    // TODO: Handle situation when network is down
                    Debug.WriteLine("Entering FetchMessage at " + DateTime.Now.ToString()); 
                    ServerResponse sr = http.FetchMessage(settings.Servers[serverIndex].InstanceId, settings.Servers[serverIndex].Cookie, settings.Servers[serverIndex].LastMessageId);
                    // check if message is valid
                    if (sr.token == null)
                    {
                        Debug.WriteLine("Skipped message with empty token");
                        continue;
                    }
                    OutgoingMessage om = new OutgoingMessage { Action = "Received", Id = sr.id, Token = sr.token, Data = JsonConvert.SerializeObject(sr.data) };
                    Debug.WriteLine("Entering send message at " + DateTime.Now.ToString());
                    int currentReaders = 0;
                    string path;
                    string args;
                    lock (thisLock)
                    {
                        try
                        {
                            AppInfo ai = settings.Servers[serverIndex].Queues.First(queue => queue.Token.Equals(om.Token));
                            if(queues.ContainsKey(ai.GetKey())) {
                                P2PMessageQueue q = queues[ai.GetKey()];
                                currentReaders = q.CurrentReaders;
                                path = ai.Path;
                                args = ai.Args;
                            }
                            else {
                                Debug.WriteLine("Skipped: " + om.Id + ", " + om.Token);
                                continue;
                            }
                        }
                        catch (InvalidOperationException)
                        {
                            Debug.WriteLine("Skipped: " + om.Id + ", " + om.Token);
                            continue;
                        }
                    }

                    // if no readers - start the app and make sure that Ai is still valid
                    bool messageSkipped = false;
                    if(currentReaders == 0) {
                        Process.Start(path, args);
                        while(currentReaders == 0) {
                            Thread.Sleep(10000);
                            lock (thisLock) {
                                try
                                {
                                    AppInfo ai = settings.Servers[serverIndex].Queues.First(queue => queue.Token.Equals(om.Token));
                                    if(queues.ContainsKey(ai.GetKey())) {
                                        P2PMessageQueue q = queues[ai.GetKey()];
                                        currentReaders = q.CurrentReaders;
                                    }
                                    else {
                                        Debug.WriteLine("Skipped: " + om.Id + ", " + om.Token);
                                        messageSkipped = true;
                                        break;
                                    }
                                }
                                catch (InvalidOperationException)
                                {
                                    Debug.WriteLine("Skipped: " + om.Id + ", " + om.Token);
                                    messageSkipped = true;
                                    break;
                                }
                            }
                        }
                        if(messageSkipped) {
                            continue;
                        }
                        // give a chance for the client app to fully initialize
                        Thread.Sleep(10000);
                    }

                    // send the message
                    lock (thisLock)
                    {
                        try
                        {
                            AppInfo ai = settings.Servers[serverIndex].Queues.First(queue => queue.Token.Equals(om.Token));
                            if (queues.ContainsKey(ai.GetKey()))
                            {
                                P2PMessageQueue q = queues[ai.GetKey()];

                                string data = JsonConvert.SerializeObject(om);
                                Message message = new Message(Encoding.UTF8.GetBytes(data), false);
                                Debug.WriteLine("About to send message at " + DateTime.Now.ToString());
                                ReadWriteResult result = q.Send(message, 0);

                                // If message delivered save its id
                                // TODO: Verify client responce to ensure message is readed
                                if (result == ReadWriteResult.OK)
                                {
                                    settings.Servers[serverIndex].LastMessageId = sr.id;
                                    Settings.Save(settings);
                                }
                                else
                                {
                                    Debug.WriteLine("Skipped: " + om.Id + ", " + om.Token + ", result of Send is : " + result.ToString());
                                }

                                Debug.WriteLine("Delivered: " + ai.GetKey() + ", " + data + " at " + DateTime.Now.ToString());
                            }
                            else
                            {
                                Debug.WriteLine("Skipped: " + om.Id + ", " + om.Token);
                            }
                        }
                        catch (InvalidOperationException)
                        {
                            Debug.WriteLine("Skipped: " + om.Id + ", " + om.Token);
                        }
                    }
                }
            }
        }

        /// <summary>
        /// Keeps data for message to the client application.
        /// </summary>
        public struct OutgoingMessage
        {
            /// <summary>
            /// Gets or sets action type.
            /// </summary>
            public string Action { get; set; }

            /// <summary>
            /// Gets or sets message id.
            /// </summary>
            public int Id { get; set; }

            /// <summary>
            /// Gets or sets secret token.
            /// </summary>
            public string Token { get; set; }

            /// <summary>
            /// Gets or sets message data.
            /// </summary>
            public string Data { get; set; }
        }
    }
}
