using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using System.Threading;
using System.IO;
using System.Net;
using System.Net.Sockets;
using SciChart.Charting.Model.DataSeries;
using System.Windows.Threading;
using SciChart.Charting3D.Model;
using System.Reflection;
using Vlc.DotNet.Wpf;
using System.Diagnostics;
using System.Net.NetworkInformation;
using SciChart.Examples.ExternalDependencies.Data;

namespace OpticalFlowDashBoard
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        const int PORT_NO = 5000;
        string SERVER_IP = "";
        OpticalFlowServer udpServer = null;
        private const String FILE_NAME = "stream.sdp";

        string contents = "v=0 o=- 0 0 IN IP4 null\n" +
            "s=Unnamed\n" +
            "i = N / A\n" +
            "c=IN IP4 192.168.0.177\n" +
            "t=0 0\n" +
            "a=recvonly\n" +
            "m = audio 5004 RTP/AVP 96\n" +
            "a=rtpmap:96 AMR/8000\n" +
            "a=fmtp:96 octet-align=1;\n" +
            "a=control:trackID=0\n" +
            "m=video 5006 RTP/AVP 96\n" +
            "a=rtpmap:96 H264/90000\n" +
            "a=fmtp:96 packetization-mode=1;profile-level-id=4d4029;sprop-parameter-sets=Z01AKY1oFB+gHhEI1A==,aO4BqDXo;\n" +
            "a=control:trackID=1";
        

        IXyDataSeries<float, float> xWorldAcc;
        IXyDataSeries<float, float> yWorldAcc;
        IXyDataSeries<float, float> zWorldAcc;

        IXyDataSeries<float, float> xDeviceAcc;
        IXyDataSeries<float, float> yDeviceAcc;
        IXyDataSeries<float, float> zDeviceAcc;

        IXyDataSeries<float, float> xWorldVel;
        IXyDataSeries<float, float> yWorldVel;
        IXyDataSeries<float, float> zWorldVel;

        IXyDataSeries<float, float> xDeviceVel;
        IXyDataSeries<float, float> yDeviceVel;
        IXyDataSeries<float, float> zDeviceVel;
        
        IXyDataSeries<float, float> xRealVel;
        IXyDataSeries<float, float> yRealVel;
        IXyDataSeries<float, float> zRealVel;

        XyzDataSeries3D<double> positionVec;
        XyzDataSeries3D<double> realSensePositionVec;

        public MainWindow()
        {
            InitializeComponent();

            SERVER_IP = GetAddresses()[0];

            UpdateText("Starting server on IP Address: " + SERVER_IP );

            // Write the SDP FILE.
            string lines = "First line.\r\nSecond line.\r\nThird line.";

            // Write the string to a file.
            System.IO.StreamWriter file = new System.IO.StreamWriter(".\\videoDescription.sdp");
            file.WriteLine(contents);

            file.Close();



            var currentAssembly = Assembly.GetEntryAssembly();
            var currentDirectory = new FileInfo(currentAssembly.Location).DirectoryName;
            // Default installation path of VideoLAN.LibVLC.Windows
            var libDirectory = new DirectoryInfo(System.IO.Path.Combine(currentDirectory, "libvlc", IntPtr.Size == 4 ? "win-x86" : "win-x64"));


            vlcPlayer.SourceProvider.CreatePlayer(libDirectory, new string[] { "--network-caching=0", "--clock-jitter=0", "clock-syncro=0" } /* pass your player parameters here */);
            vlcPlayer.SourceProvider.MediaPlayer.Play(new FileInfo(".\\videoDescription.sdp"));

            //OpticalFlowServer.startServer(null);
            this.Loaded += OnLoaded;

            xWorldAcc = new XyDataSeries<float, float>();
            yWorldAcc = new XyDataSeries<float, float>();
            zWorldAcc = new XyDataSeries<float, float>();

            xDeviceAcc = new XyDataSeries<float, float>();
            yDeviceAcc = new XyDataSeries<float, float>();
            zDeviceAcc = new XyDataSeries<float, float>();

            xWorldVel = new XyDataSeries<float, float>();
            yWorldVel = new XyDataSeries<float, float>();
            zWorldVel = new XyDataSeries<float, float>();

            xDeviceVel = new XyDataSeries<float, float>();
            yDeviceVel = new XyDataSeries<float, float>();
            zDeviceVel = new XyDataSeries<float, float>();

            xRealVel = new XyDataSeries<float, float>();
            yRealVel = new XyDataSeries<float, float>();
            zRealVel = new XyDataSeries<float, float>();

            positionVec = new XyzDataSeries3D<double>();
            realSensePositionVec = new XyzDataSeries3D<double>();


            //a.Move((int)(myCanvas.Width/2), (int)(myCanvas.Height/2));

            udpServer = new OpticalFlowServer(optiflowCanvas, consoleOutput, PORT_NO, SERVER_IP, 
                                                    new IXyDataSeries<float, float>[] { xWorldAcc, yWorldAcc, zWorldAcc },
                                                    new IXyDataSeries<float, float>[] { xDeviceAcc, yDeviceAcc, zDeviceAcc },
                                                    new IXyDataSeries<float, float>[] { xWorldVel, yWorldVel, zWorldVel },
                                                    new IXyDataSeries<float, float>[] { xDeviceVel, yDeviceVel, zDeviceVel },
                                                    new IXyDataSeries<float, float>[] { xRealVel, yRealVel, zRealVel },
                                                    positionVec, 
                                                    realSensePositionVec);


            Thread test = new Thread(new ThreadStart(udpServer.startServer));
            test.Start();
        }

        public static List<string> GetAddresses()
        {
            var host = Dns.GetHostEntry(Dns.GetHostName());
            return (from ip in host.AddressList where ip.AddressFamily == AddressFamily.InterNetwork select ip.ToString()).ToList();
        }

        int i = 0;
        private void OnLoaded(object sender, RoutedEventArgs routedEventArgs)
        {
            XWorldAcc.DataSeries = xWorldAcc;
            YWorldAcc.DataSeries = yWorldAcc;
            ZWorldAcc.DataSeries = zWorldAcc;

            XDeviceAcc.DataSeries = xDeviceAcc;
            YDeviceAcc.DataSeries = yDeviceAcc;
            ZDeviceAcc.DataSeries = zDeviceAcc;

            XWorldVel.DataSeries = xWorldVel;
            YWorldVel.DataSeries = yWorldVel;
            ZWorldVel.DataSeries = zWorldVel;

            XDeviceVel.DataSeries = xDeviceVel;
            YDeviceVel.DataSeries = yDeviceVel;
            ZDeviceVel.DataSeries = zDeviceVel;

            XRealSenseVel.DataSeries = xRealVel;
            YRealSenseVel.DataSeries = yRealVel;
            ZRealSenseVel.DataSeries = zRealVel;

            PositionGraph.DataSeries = positionVec;
            RealSensePositionGraph.DataSeries = realSensePositionVec;


            var timer = new DispatcherTimer(DispatcherPriority.Render);
            timer.Interval = TimeSpan.FromMilliseconds(100);
            timer.Tick += (s, e) =>
            {
                // This updates the graph when the program is running! 
                using (xWorldAcc.SuspendUpdates())
                using (yWorldAcc.SuspendUpdates())
                using (zWorldAcc.SuspendUpdates())

                using (xDeviceAcc.SuspendUpdates())
                using (yDeviceAcc.SuspendUpdates())
                using (zDeviceAcc.SuspendUpdates())

                using (xWorldVel.SuspendUpdates())
                using (yWorldVel.SuspendUpdates())
                using (zWorldVel.SuspendUpdates())

                using (xDeviceVel.SuspendUpdates())
                using (yDeviceVel.SuspendUpdates())
                using (zDeviceVel.SuspendUpdates())

                using (xRealVel.SuspendUpdates())
                using (yRealVel.SuspendUpdates())
                using (zRealVel.SuspendUpdates())

                {
                    sciChartSurface.ZoomExtents();
                    sciChartSurface1.ZoomExtents();
                    sciChartSurface2.ZoomExtents();
                    sciChartSurface3.ZoomExtents();
                    sciRealSenseVel.ZoomExtents();

                    RotateTransform rotateTransform =
                    new RotateTransform(udpServer.getCompassAngle() + 90);
                    arrow.RenderTransform = rotateTransform;

                    RotateTransform rotateTransform1 =
                    new RotateTransform(udpServer.getCameraPointingAngle());
                    CamArrow.RenderTransform = rotateTransform1;

                    RotateTransform rotateTransform2 =
                    new RotateTransform(udpServer.getDeviceTiltAngle());
                    CamSpin.RenderTransform = rotateTransform2;

                }
            };
            timer.Start();
        }

        public delegate void UpdateTextCallback(string message);

        StringBuilder stringbuilder = new StringBuilder(0, 5000);
        public void UpdateText(string message)
        {
            if (stringbuilder.Length + message.Length + 2 > 4500)
            {
                stringbuilder.Remove(stringbuilder.Length - (message.Length + 2), message.Length + 2);
            }
            stringbuilder.Append(message + "\n");

            consoleOutput.Document.Blocks.Clear();
            consoleOutput.Document.Blocks.Add(new Paragraph(new Run(stringbuilder.ToString())));
        }




        //    consoleOutput.Dispatcher.Invoke(new UpdateTextCallback(this.UpdateText), "Connection Closed");

        //    client.Close();
        //    outputFile.Close();
        //    listener.Stop();
        //}

    }
}

