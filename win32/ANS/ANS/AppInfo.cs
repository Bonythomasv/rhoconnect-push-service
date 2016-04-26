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
    public class AppInfo
    {
        /// <summary>
        /// Gets or sets application guid used as an application name.
        /// </summary>        
        public string Guid { get; set; }

        /// <summary>
        /// Gets or sets user name.
        /// </summary>
        public string User { get; set; }

        /// <summary>
        /// Gets or sets user password.
        /// </summary>
        public string Pass { get; set; }

        /// <summary>
        /// Gets or sets secret token.
        /// </summary>
        public string Token { get; set; }

        public string MQName { get; set; }

        public string Session { get; set; }

        /// <summary>
        /// Gets or sets path to user application.
        /// </summary>
        public string Path { get; set; }

        public string ServerUrl { get; set; }

        /// <summary>
        /// Gets or sets startup arguments for user application.
        /// </summary>
        public string Args { get; set; }

        /// <summary>
        /// Returns unique key generated using application guid and user.
        /// </summary>
        /// <returns>A unique key.</returns>
        public string GetKey()
        {
            return this.Guid;
            //return this.Session.Substring(1,100);
        }

        /// <summary>
        /// Gets or sets command from the user app.
        /// </summary>
        public string Command { get; set; }
    }
}
