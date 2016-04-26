// <copyright file="Program.cs" company="Symbol Technologies, Inc.">
//     Company (C) Symbol Technologies, Inc.
// </copyright>
namespace ANS
{
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.IO;
    using Microsoft.Win32;
    using System.Runtime.InteropServices;

    enum ConnMgrStatus
    {
        Unknown = 0x00,
        Connected = 0x10,
        Suspended = 0x11,
        Disconnected = 0x20,
        ConnectionFailed = 0x21,
        ConnectionCanceled = 0x22,
        ConnectionDisabled = 0x23,
        NoPathToDestination = 0x24,
        WaitingForPath = 0x25,
        WaitingForPhone = 0x26,
        PhoneOff = 0x27,
        ExclusiveConflict = 0x28,
        NoResources = 0x29,
        ConnectionLinkFailed = 0x2a,
        AuthenticationFailed = 0x2b,
        NoPathWithProperty = 0x2c,
        WaitingConnection = 0x40,
        WaitingForResource = 0x41,
        WaitingForNetwork = 0x42,
        WaitingDisconnection = 0x80,
        WaitingConnectionAbort = 0x81
    }

    [Flags]
    enum ConnMgrParam : int
    {
        GuidDestNet = 0x1,
        MaxCost = 0x2,
        MinRcvBw = 0x4,
        MaxConnLatency = 0x8
    }

    [Flags]
    enum ConnMgrProxy : int
    {
        NoProxy = 0x0,
        Http = 0x1,
        Wap = 0x2,
        Socks4 = 0x4,
        Socks5 = 0x8
    }

    enum ConnMgrPriority
    {
        UserInteractive = 0x8000,
        HighPriorityBackground = 0x0200,
        LowPriorityBackground = 0x0008
    }


    [StructLayout(LayoutKind.Sequential)]
    class ConnMgrConnectionInfo
    {
        Int32 cbSize;                          // DWORD
        public ConnMgrParam dwParams = 0;      // DWORD
        public ConnMgrProxy dwFlags = 0;       // DWORD
        public ConnMgrPriority dwPriority = 0; // DWORD
        public Int32 bExclusive = 0;           // BOOL
        public Int32 bDisabled = 0;            // BOOL
        public Guid guidDestNet = Guid.Empty;  // GUID
        public IntPtr hWnd = IntPtr.Zero;      // HWND
        public UInt32 uMsg = 0;                // UINT
        public Int32 lParam = 0;               // LPARAM
        public UInt32 ulMaxCost = 0;           // ULONG
        public UInt32 ulMinRcvBw = 0;          // ULONG
        public UInt32 ulMaxConnLatency = 0;    // ULONG 

        public ConnMgrConnectionInfo()
        {
            cbSize = Marshal.SizeOf(typeof(ConnMgrConnectionInfo));
        }

        public ConnMgrConnectionInfo(Guid destination, ConnMgrPriority priority, ConnMgrProxy proxy)
            : this()
        {
            guidDestNet = destination;
            dwParams = ConnMgrParam.GuidDestNet;
            dwPriority = priority;
            dwFlags = proxy;
        }

        public ConnMgrConnectionInfo(Guid destination, ConnMgrPriority priority)
            : this(destination, priority, ConnMgrProxy.NoProxy) { }

        public ConnMgrConnectionInfo(Guid destination)
            : this(destination, ConnMgrPriority.UserInteractive) { }


    } ;


    /// <summary>
    /// The class defines entry point into application.
    /// </summary>
    public static class Program
    {

        [DllImport("coredll.dll", EntryPoint = "SHCreateShortcut", SetLastError = true)]
        public static extern bool SHCreateShortcut(string shortcut, string target);

        [DllImport("CellCore.dll")]
        static extern int ConnMgrMapURL(string url, ref Guid networkGuid, int passZero);
        [DllImport("CellCore.dll")]
        static extern int ConnMgrEstablishConnection(ConnMgrConnectionInfo connectionInfo, ref IntPtr connectionHandle);
        [DllImport("CellCore.dll")]
        static extern int ConnMgrEstablishConnectionSync(ConnMgrConnectionInfo connectionInfo, ref IntPtr connectionHandle, uint dwTimeout, ref ConnMgrStatus dwStatus);
        [DllImport("CellCore.dll")]
        static extern int ConnMgrReleaseConnection(IntPtr connectionHandle, int cache);
        [DllImport("CellCore.dll")]
        static extern int ConnMgrConnectionStatus(IntPtr connectionHandle, ref ConnMgrStatus status);


        /// <summary>
        /// The main entry point for the application.
        /// </summary>
        [MTAThread]
        public static void Main(string[] args)
        {
            FileStream stream = null;
            try
            {
                stream = new FileStream("lock.txt", FileMode.OpenOrCreate, FileAccess.ReadWrite, FileShare.None);
            }
            catch
            {
                System.Diagnostics.Process.GetCurrentProcess().Kill();
            }

            SHCreateShortcut(@"\Windows\StartUp\rhoconnect-push-service.lnk", "\"" + "Program Files\\rhoconnect-push-service\\rhoconnect-push.exe" + "\"");

            IntPtr _connectionHandle = IntPtr.Zero;
            Guid networkGuid = Guid.Empty;

            try
            {
                
                ConnMgrStatus status = ConnMgrStatus.Unknown;
                ConnMgrMapURL("http://www.google.com", ref networkGuid, 0);
                ConnMgrConnectionInfo info = new ConnMgrConnectionInfo(networkGuid, ConnMgrPriority.HighPriorityBackground);
                ConnMgrEstablishConnectionSync(info, ref _connectionHandle, uint.Parse(Properties.Resources.HttpRetryDelay), ref status);
            }
            catch{}

            Application.Instance.Start();
            if (stream != null)
                stream.Close();

            try
            {
                ConnMgrReleaseConnection(_connectionHandle, 1);
            }
            catch { }

        }
    }
}