// <copyright file="Http.cs" company="Symbol Technologies, Inc.">
//     Company (C)Symbol Technologies, Inc.
// </copyright>

namespace ANS
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.IO;
    using System.Linq;
    using System.Net;
    using System.Text;
    using System.Text.RegularExpressions;
    using System.Threading;

    using Microsoft.Win32;
    using Newtonsoft.Json;

    /// <summary>
    /// Performs network operations.
    /// </summary>
    public class Http
    {
        /// <summary>
        /// Keeps url for ANS server. 
        /// If registry path is valid, the url is taken from registry, otherwise it is taken from the properties.
        /// </summary>
        private Uri serverUrl;

        /// <summary>
        /// Keeps value taken from the properties of http retries in milliseconds.
        /// </summary>
        private readonly int retryDelay = int.Parse(Properties.Resources.HttpRetryDelay);
        /// <summary>
        /// Keeps value taken from the properties of FetchMessage Timeout retries in milliseconds.
        /// </summary>
        public readonly int fetchMessageTimeoutRetryDelay = int.Parse(Properties.Resources.MessageTimeoutRetryDelay);

        private int serverIndex;

        public Http(int _serverIndex)
        {
            serverIndex = _serverIndex;
            serverUrl = new Uri(Settings.Instance.Servers[serverIndex].ANSServerURL);
        }

        /// <summary>
        /// Fetches instance ID from the remote server.
        /// Executes POST for "/instanceId" noun.
        /// </summary>
        /// <param name="instanceId">A string containing instance id.</param>
        /// <returns>A string containing instance id.</returns>
        public string FetchInstanceId(AppInfo ai)
        {
            string result = string.Empty;
            string url = this.serverUrl + "instanceId";
            string credentials = Settings.Instance.Servers[serverIndex].InstanceUsername + ":" + Settings.Instance.Servers[serverIndex].InstancePassword;

            Debug.WriteLine("Fetch instance: url = " + url + ", creds = " + credentials);

            while (true)
            {
                try
                {
                    HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);

                    request.Headers["Authorization"] = "Basic " + Convert.ToBase64String(Encoding.UTF8.GetBytes(credentials));
                    request.Method = "POST";
                    request.Headers["Cookie"] = ai.Session;
                    request.ContentLength = 0;


                    using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
                    {
                        if (response.StatusCode == HttpStatusCode.OK)
                        {
                            Debug.WriteLine("Instance received");
                        }

                        using (Stream dataStream = response.GetResponseStream())
                        {
                            using (StreamReader reader = new StreamReader(dataStream))
                            {
                                result = JsonConvert.DeserializeObject<ServerResponse>(reader.ReadToEnd()).instance;
                                reader.Close();
                            }

                            dataStream.Close();
                        }

                        response.Close();
                    }
                }
                catch (WebException e)
                {
                    Debug.WriteLine(e.Message);
                    Thread.Sleep(this.retryDelay);

                    continue;
                }
                
                break;
            }

            return result;
        }

        /// <summary>
        /// Deletes instance id on the remote server.
        /// Executes DELETE for "/instanceId/{instance}" none.
        /// </summary>
        /// <param name="instanceId">A string containing instance id.</param>
        /// <param name="ai">AppInfo object containing info about RhoConnect app.</param>
        public void DeleteInstanceId(string instanceId, AppInfo ai)
        {
            string result = string.Empty;
            string url = this.serverUrl + "instanceId" + "/" + Uri.EscapeUriString(instanceId);
            string credentials = Settings.Instance.Servers[serverIndex].InstanceUsername + ":" + Settings.Instance.Servers[serverIndex].InstancePassword;

            Debug.WriteLine("Delete instance: url = " + url + ", creds = " + credentials);

                while (true)
                {
                    try
                    {
                        HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);

                        request.Headers["Authorization"] = "Basic " + Convert.ToBase64String(Encoding.UTF8.GetBytes(credentials));
                        request.Method = "DELETE";
                        request.Headers["Cookie"] = ai.Session;

                        using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
                        {
                            if (response.StatusCode == HttpStatusCode.NoContent)
                            {
                                Debug.WriteLine("Instance deleted");
                            }

                            response.Close();
                        }
                    }
                    catch (WebException e)
                    {
                        Debug.WriteLine(e.Message);
                        Thread.Sleep(this.retryDelay);

                        continue;
                    }

                    break;
                }
        }

        /// <summary>
        /// Fetches instance cookie from the remote server.
        /// Executes GET for "/instanceId/{instance}" none. 
        /// </summary>
        /// <param name="instanceId">A string containing instance id.</param>
        /// <param name="ai">AppInfo object containing info about RhoConnect app.</param>
        /// <returns>A string containing instance cookie.</returns>
        public string FetchInstanceCookie(string instanceId, AppInfo ai)
        {
            string result = string.Empty;
            string url = this.serverUrl + "instanceId" + "/" + Uri.EscapeUriString(instanceId);
            string credentials = Settings.Instance.Servers[serverIndex].InstanceUsername + ":" + Settings.Instance.Servers[serverIndex].InstancePassword;

            Debug.WriteLine("Fetch cookie: url = " + url + ", creds = " + credentials);

            while (true)
            {
                try
                {
                    HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);

                    request.Headers["Authorization"] = "Basic " + Convert.ToBase64String(Encoding.UTF8.GetBytes(credentials));
                    request.Method = "GET";
                    request.Headers["Cookie"] = ai.Session;

                    using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
                    {
                        if (response.StatusCode == HttpStatusCode.NoContent)
                        {
                            Debug.WriteLine("Cookie received");
                        }

                        if (response.Headers["Set-Cookie"] != null)
                        {
                            result = Regex.Match(response.Headers["Set-Cookie"], @"instance\=([^\=\;]+)").Groups[1].Value;
                        }

                        response.Close();
                    }
                }
                catch (WebException e)
                {
                    Debug.WriteLine(e.Message);
                    Thread.Sleep(this.retryDelay);

                    continue;
                }

                break;
            }

            return result;
        }

        /// <summary>
        /// Creates or update registration token for specific user/app.
        /// Executes PUT for "/registrations/{instance}/{user}/{application}" noun.
        /// </summary>
        /// <param name="instanceId">A string containing instance id.</param>
        /// <param name="cookie">A string containing instance cookie.</param>
        /// <param name="user">A string containing user name.</param>
        /// <param name="pass">A string containing user password.</param>
        /// <param name="guid">A string containing user guid.</param>
        /// <param name="sessionCookie">A string containing user's RhoConnect session cookie</param>
        /// <returns>A string containing registration token.</returns>
        public string CreateRegistrationToken(string instanceId, string cookie, string user, string pass, string guid, string sessionCookie)
        {
            string result = string.Empty;
            string url = this.serverUrl + "registrations" + "/" + Uri.EscapeUriString(instanceId) + "/" + Uri.EscapeUriString(user) + "/" + Uri.EscapeUriString(guid);
            string credentials = user + ":" + pass;

            Debug.WriteLine("Create token: url = " + url + ", creds = " + credentials);

                while (true)
                {
                    try
                    {
                        HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);

                        request.Headers["Authorization"] = "Basic " + Convert.ToBase64String(Encoding.UTF8.GetBytes(credentials));
                        request.Method = "PUT";
                        request.Headers.Add("Cookie", "instance=" + cookie);
                        if (sessionCookie.Length != 0)
                            request.Headers.Add("Cookie", sessionCookie);

                        using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
                        {
                            if (response.StatusCode == HttpStatusCode.Created)
                            {
                                Debug.WriteLine("Token received");
                            }

                            using (Stream dataStream = response.GetResponseStream())
                            {
                                using (StreamReader reader = new StreamReader(dataStream))
                                {
                                    result = JsonConvert.DeserializeObject<ServerResponse>(reader.ReadToEnd()).token;
                                    reader.Close();
                                }

                                dataStream.Close();
                            }

                            response.Close();
                        }
                    }
                    catch (WebException e)
                    {
                        Debug.WriteLine(e.Message);
                        Thread.Sleep(this.retryDelay);

                        continue;
                    }

                    break;
                }

            return result;
        }

        /// <summary>
        /// Receives registration token for specific user/app.
        /// Executes GET for "/registrations/{instance}/{user}/{application}" noun.
        /// </summary>
        /// <param name="instanceId">A string containing instance id.</param>
        /// <param name="cookie">A string containing instance cookie.</param>
        /// <param name="user">A string containing user name.</param>
        /// <param name="pass">A string containing user pass.</param>
        /// <param name="guid">A string containing application guid.</param>
        /// <param name="sessionCookie">A string containing user's RhoConnect session cookie</param>
        /// <returns>A string containing registration token.</returns>
        public string FetchRegistrationToken(string instanceId, string cookie, string user, string pass, string guid, string sessionCookie)
        {
            Debug.WriteLine("In FetchRegistrationToken at :" + DateTime.Now.ToString());
            string result = string.Empty;
            string url = this.serverUrl + "registrations" + "/" + Uri.EscapeUriString(instanceId) + "/" + Uri.EscapeUriString(user) + "/" + Uri.EscapeUriString(guid);
            string credentials = user + ":" + pass;

            Debug.WriteLine("Fetch token: url = " + url + ", creds = " + credentials + " at : " + DateTime.Now.ToString());

            while (true)
            {
                try
                {
                    HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);

                    request.Headers["Authorization"] = "Basic " + Convert.ToBase64String(Encoding.UTF8.GetBytes(credentials));
                    request.Method = "GET";
                    request.Headers.Add("Cookie",  "instance=" + cookie);
                    if (sessionCookie.Length != 0)
                        request.Headers.Add("Cookie", sessionCookie);

                    using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
                    {
                        if (response.StatusCode == HttpStatusCode.Created)
                        {
                            Debug.WriteLine("Token received");
                        }

                        using (Stream dataStream = response.GetResponseStream())
                        {
                            using (StreamReader reader = new StreamReader(dataStream))
                            {
                                result = JsonConvert.DeserializeObject<ServerResponse>(reader.ReadToEnd()).token;
                                reader.Close();
                            }

                            dataStream.Close();
                        }

                        response.Close();
                    }
                }
                catch (WebException e)
                {
                    Debug.WriteLine(e.Message);
                    Thread.Sleep(this.retryDelay);

                    continue;
                }

                break;
            }

            return result;
        }

        /// <summary>
        /// Deletes registration token for specific user/app.
        /// Executes DELETE for "/registrations/{instance}/{user}/{application}" noun.
        /// </summary>
        /// <param name="instanceId">A string containing instance id.</param>
        /// <param name="cookie">A string containing instance cookie.</param>
        /// <param name="user">A string containing user name.</param>
        /// <param name="pass">A string containing user pass.</param>
        /// <param name="guid">A string containing application guid.</param>
        /// <param name="sessionCookie">A string containing user's RhoConnect session cookie</param>
        public void DeleteRegistrationToken(string instanceId, string cookie, string user, string pass, string guid, string sessionCookie)
        {
            string result = string.Empty;
            string url = this.serverUrl + "registrations" + "/" + Uri.EscapeUriString(instanceId) + "/" + Uri.EscapeUriString(user) + "/" + Uri.EscapeUriString(guid);
            string credentials = user + ":" + pass;

            Debug.WriteLine("Delete token: url = " + url + ", creds = " + credentials);

                while (true)
                {
                    try
                    {
                        HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);

                        request.Headers["Authorization"] = "Basic " + Convert.ToBase64String(Encoding.UTF8.GetBytes(user + ":" + pass));
                        request.Method = "DELETE";
                        request.KeepAlive = false;
                        request.Headers.Add("Cookie", "instance=" + cookie);
                        if (sessionCookie.Length != 0)
                            request.Headers.Add("Cookie", sessionCookie);

                        using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
                        {
                            if (response.StatusCode == HttpStatusCode.NoContent)
                            {
                                Debug.WriteLine("Token deleted");
                            }

                            response.Close();
                        }
                    }
                    catch (WebException e)
                    {
                        Debug.WriteLine(e.Message);
                        Thread.Sleep(this.retryDelay);

                        continue;
                    }

                    break;
                }
        }

        /// <summary>
        /// Receives the next message from the server.
        /// Executes GET for "/nextMessage/{instance}" noun.
        /// </summary>
        /// <param name="instanceId">A string containing instance id.</param>
        /// <param name="cookie">A string containing instance cookie.</param>
        /// <param name="last">A number of the last message received.</param>
        /// <returns>A string with the JSON containing the message.</returns>
        public ServerResponse FetchMessage(string instanceId, string cookie, int last)
        {
            ServerResponse result = new ServerResponse();
            string url = this.serverUrl + "nextMessage" + "/" + Uri.EscapeUriString(instanceId) + (last == -1 ? string.Empty : "?lastMessage=" + last.ToString());

            Debug.WriteLine("Fetch message: url = " + url);

            while (true)
            {
                try
                {
                        HttpWebRequest fetchMessageRequest = (HttpWebRequest)WebRequest.Create(url);
                        fetchMessageRequest.Method = "GET";
                        fetchMessageRequest.KeepAlive = false;
                        fetchMessageRequest.Headers["Cookie"] = "instance=" + cookie;

                    using (HttpWebResponse response = (HttpWebResponse)fetchMessageRequest.GetResponse())
                    {
                            if (response.StatusCode == HttpStatusCode.OK)
                            {
                                Debug.WriteLine("Message received");
                            }

                            using (Stream dataStream = response.GetResponseStream())
                            {
                                using (StreamReader reader = new StreamReader(dataStream))
                                {
                                    string data = reader.ReadToEnd();

                                    if (data != "")
                                        result = JsonConvert.DeserializeObject<ServerResponse>(data);
                                    else
                                        result = new ServerResponse();
                                    reader.Close();
                                }

                                dataStream.Close();
                            }

                            response.Close();
                    }
                }
                catch (WebException e)
                {
                    Debug.WriteLine(e.Message + ", err: " + e.Status.ToString());
                    if (e.Status == WebExceptionStatus.Timeout)
                        Thread.Sleep(this.fetchMessageTimeoutRetryDelay);
                    else if (e.Status == WebExceptionStatus.RequestCanceled)
                        break;
                    else
                        Thread.Sleep(this.retryDelay);

                    break;
                }

                break;
            }

            return result;
        }
    }
}
