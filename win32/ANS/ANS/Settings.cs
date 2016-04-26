// <copyright file="Settings.cs" company="Symbol Technologies, Inc.">
//     Company (C) Symbol Technologies, Inc.
// </copyright>

namespace ANS
{
    using System;
    using System.Collections.Generic;
    using System.IO;
    using System.Linq;
    using System.Reflection;
    using System.Text;
    using System.Xml.Serialization;

    /// <summary>
    /// The class keeps settings - values that should servive powerdown in the xml file.
    /// </summary>
    public class Settings
    {
        [XmlIgnore]

        /// <summary>
        /// Keeps instanse of the singletone class.
        /// </summary>
        private static Settings instance = Settings.Load();

        /// <summary>
        /// Prevents a default instance of the <see cref="Settings" /> class from being created.
        /// Initializes default values of the class instance.
        /// </summary>
        private Settings()
        {
            this.Servers = new List<ServerInfo>();
        }

        [XmlIgnore]

        /// <summary>
        /// Gets a value of the singleton class instance.
        /// </summary>
        public static Settings Instance
        {
            get { return instance; }
        }

        public List<ServerInfo> Servers { get; set; }
       
        /// <summary>
        /// Loads global service settings from the settings file if there is any.
        /// </summary>
        /// <returns>An instance of the Settings class initialized either to default or to saved values.</returns>
        public static Settings Load()
        {
            Settings settings = null;
            XmlSerializer serializer = new XmlSerializer(typeof(Settings));
            string path = Path.GetDirectoryName(Assembly.GetCallingAssembly().GetName().CodeBase);
            path = path.Substring(6);

            try
            {
                using (StreamReader reader = new StreamReader(path + "\\settings.xml"))
                {
                    settings = (Settings)serializer.Deserialize(reader);
                    reader.Close();
                }
            }
            catch (FileNotFoundException)
            {
                settings = new Settings();
            }
            catch (InvalidOperationException)
            {
                settings = new Settings();
            }

            return settings;
        }

        public int getServerIndexByToken(string token)
        {
            for (int i = 0; i < Servers.Count(); ++i)
            {
                for (int j = 0; j < Servers[i].Queues.Count(); ++j)
                {
                    if (Servers[i].Queues[j].Token.Equals(token))
                        return i;
                }
            }

            return -1;
        }

        public int getServerIndexByUrl(string url)
        {
            for (int i = 0; i < Servers.Count(); ++i)
            {
                    if (Servers[i].ANSServerURL.Equals(url))
                        return i;
            }

            return -1;
        }

        public AppInfo findAndUpdateAppInfo(AppInfo ai)
        {
            for (int i = 0; i < Servers.Count(); ++i)
            {
                for (int j = 0; j < Servers[i].Queues.Count(); ++j)
                {
                    if (Servers[i].Queues[j].GetKey().Equals(ai.GetKey()))
                    {
                        if (ai.User.Length != 0 && ai.Pass.Length != 0)
                        {
                            Servers[i].Queues[j].User = ai.User;
                            Servers[i].Queues[j].Pass = ai.Pass;    
                        }
                        Servers[i].Queues[j].Command = ai.Command;
                        return Servers[i].Queues[j];
                    }
                }
            }

            return ai;
        }

        public String getUserPassByToken(string token)
        {
            for (int i = 0; i < Servers.Count(); ++i)
            {
                for (int j = 0; j < Servers[i].Queues.Count(); ++j)
                {
                    if (Servers[i].Queues[j].Token.Equals(token))
                        return Servers[i].Queues[j].Pass;
                }
            }

            return "";
        }

        /// <summary>
        /// Saves global service settings to the settings file.
        /// </summary>
        /// <param name="settings">An instance of the Settings class with values to be saved.</param>
        public static void Save(Settings settings)
        {
            XmlSerializer serializer = new XmlSerializer(typeof(Settings));
            string path = Path.GetDirectoryName(Assembly.GetCallingAssembly().GetName().CodeBase);
            path = path.Substring(6);

            using (StreamWriter writer = new StreamWriter(path + "\\settings.xml"))
            {
                serializer.Serialize(writer, settings);
                writer.Close();
            }

            Settings sample = Settings.Load();
        }
    }
}
