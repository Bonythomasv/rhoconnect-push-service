// <copyright file="ServerResponse.cs" company="Symbol Technologies, Inc.">
//     Company (C) Symbol Technologies, Inc.
// </copyright>

namespace ANS
{
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Text;

    [System.Diagnostics.CodeAnalysis.SuppressMessage(
        "Microsoft.StyleCop.CSharp.NamingRules",
        "SA1300:ElementMustBeginWithUpperCaseLetter",
        Justification = "To simplify json convertion to/from lower case format.")]

    /// <summary>
    /// Keeps data received from the remote server.
    /// </summary>
    public struct ServerResponse
    {
        /// <summary>
        /// Gets or sets message instance.
        /// </summary>
        public string instance { get; set; }

        /// <summary>
        /// Gets or sets message id.
        /// </summary>
        public int id { get; set; }

        /// <summary>
        /// Gets or sets message token.
        /// </summary>
        public string token { get; set; }

        /// <summary>
        /// Gets or sets message data.
        /// </summary>
        public object data { get; set; }
    }
}
