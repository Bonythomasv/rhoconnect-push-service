﻿//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a tool.
//     Runtime Version:2.0.50727.5466
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------

namespace ANS.Properties {
    using System;
    
    
    /// <summary>
    ///   A strongly-typed resource class, for looking up localized strings, etc.
    /// </summary>
    // This class was auto-generated by the StronglyTypedResourceBuilder
    // class via a tool like ResGen or Visual Studio.
    // To add or remove a member, edit your .ResX file then rerun ResGen
    // with the /str option, or rebuild your VS project.
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
    internal class Resources {
        
        private static global::System.Resources.ResourceManager resourceMan;
        
        private static global::System.Globalization.CultureInfo resourceCulture;
        
        internal Resources() {
        }
        
        /// <summary>
        ///   Returns the cached ResourceManager instance used by this class.
        /// </summary>
        [global::System.ComponentModel.EditorBrowsableAttribute(global::System.ComponentModel.EditorBrowsableState.Advanced)]
        internal static global::System.Resources.ResourceManager ResourceManager {
            get {
                if (object.ReferenceEquals(resourceMan, null)) {
                    global::System.Resources.ResourceManager temp = new global::System.Resources.ResourceManager("ANS.Properties.Resources", typeof(Resources).Assembly);
                    resourceMan = temp;
                }
                return resourceMan;
            }
        }
        
        /// <summary>
        ///   Overrides the current thread's CurrentUICulture property for all
        ///   resource lookups using this strongly typed resource class.
        /// </summary>
        [global::System.ComponentModel.EditorBrowsableAttribute(global::System.ComponentModel.EditorBrowsableState.Advanced)]
        internal static global::System.Globalization.CultureInfo Culture {
            get {
                return resourceCulture;
            }
            set {
                resourceCulture = value;
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to http://hollow-stream-9101.herokuapp.com.
        /// </summary>
        internal static string ANSServerURL {
            get {
                return ResourceManager.GetString("ANSServerURL", resourceCulture);
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to HKEY_CURRENT_USER.
        /// </summary>
        internal static string ANSServerURLRegistryKeyName {
            get {
                return ResourceManager.GetString("ANSServerURLRegistryKeyName", resourceCulture);
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to .
        /// </summary>
        internal static string ANSServerURLRegistryKeyValue {
            get {
                return ResourceManager.GetString("ANSServerURLRegistryKeyValue", resourceCulture);
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to 60000.
        /// </summary>
        internal static string HttpRetryDelay {
            get {
                return ResourceManager.GetString("HttpRetryDelay", resourceCulture);
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to devicephilpwd.
        /// </summary>
        internal static string InstancePassword {
            get {
                return ResourceManager.GetString("InstancePassword", resourceCulture);
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to devicephil.
        /// </summary>
        internal static string InstanceUsername {
            get {
                return ResourceManager.GetString("InstanceUsername", resourceCulture);
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to 1000.
        /// </summary>
        internal static string MessageTimeoutRetryDelay {
            get {
                return ResourceManager.GetString("MessageTimeoutRetryDelay", resourceCulture);
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to FEF15A2B-CDF1-45e3-B1C6-D71E3718AFB6.
        /// </summary>
        internal static string ServiceGuid {
            get {
                return ResourceManager.GetString("ServiceGuid", resourceCulture);
            }
        }
    }
}
