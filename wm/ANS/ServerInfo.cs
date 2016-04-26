// <copyright file="AppInfo.cs" company="Symbol Technologies, Inc.">
//     Company (C) Symbol Technologies, Inc.
// </copyright>

namespace ANS
{
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Text;

    /// <summary>
    /// Keeps data that identifies an application user info.
    /// </summary>
    public class ServerInfo
    {
        /// <summary>
        /// Gets or sets received instance id for the service.
        /// </summary>
        public string InstanceId { get; set; }

        public string ANSServerURL { get; set; }

        public string InstancePassword { get; set; }

        public string InstanceUsername { get; set; }

        /// <summary>
        /// Gets or sets received cookie for the service.
        /// </summary>
        public string Cookie { get; set; }

        /// <summary>
        /// Gets or sets id of the last received message.
        /// </summary>
        public int LastMessageId { get; set; }

        /// <summary>
        /// Gets or sets lisf of queues registered with the service. 
        /// Each queue is identified with an guid, username, password and token.
        /// </summary>
        public List<AppInfo> Queues { get; set; }

        public ServerInfo() { }

        public ServerInfo(string serverURL, string username, string password)
        {
            this.Queues = new List<AppInfo>();
            this.LastMessageId = -1;
            ANSServerURL = serverURL;
            InstancePassword = password;
            InstanceUsername = username;
        }
    }
}
