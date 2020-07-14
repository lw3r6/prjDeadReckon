using System;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Diagnostics;
using System.Collections.Generic;
using System.Linq;
using SciChart.Charting.Model.DataSeries;
using System.Threading;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Shapes;
using System.Windows;
using SciChart.Charting3D.Model;
using Vlc.DotNet.Core.Interops.Signatures;
using System.Windows.Media;

namespace OpticalFlowDashBoard
{
    class OpticalFlowServer
    {
        int PORT_NO;
        string SERVER_IP;
        private IXyDataSeries<float, float>[] worldAcc;
        private IXyDataSeries<float, float>[] deviceAcc;
        private IXyDataSeries<float, float>[] worldVel;
        private IXyDataSeries<float, float>[] deviceVel;
        private IXyDataSeries<float, float>[] realSenseVel;
        private XyzDataSeries3D<double> positionVec;
        private XyzDataSeries3D<double> realSensePositionVec;

        public double compassAngle = 0;
        private double cameraPointingAngle = 0;
        private double deviceTiltAngle = 0;
        private RichTextBox windowConsole;
        private Canvas canvas;
        

        public OpticalFlowServer(Canvas canvasIn, 
            RichTextBox windowConsole, int port_Number, string IP, 
            IXyDataSeries<float, float>[] worldAcc, 
            IXyDataSeries<float, float>[] deviceAcc, 
            IXyDataSeries<float, float>[] worldVel, 
            IXyDataSeries<float, float>[] deviceVel, 
            IXyDataSeries<float, float>[] realSenseVel, 
            XyzDataSeries3D<double> positionDataSeries,
            XyzDataSeries3D<double> realSensePositionVec)
        {
            this.worldAcc = worldAcc;
            this.deviceAcc = deviceAcc;
            this.worldVel = worldVel;
            this.deviceVel = deviceVel;
            this.realSenseVel = realSenseVel;
            this.positionVec = positionDataSeries;
            this.realSensePositionVec = realSensePositionVec;
            this.windowConsole = windowConsole;
            this.PORT_NO = port_Number;
            this.SERVER_IP = IP;
            this.canvas = canvasIn; 
        }

        public double getCompassAngle() {
            return compassAngle;
        }

        public double getCameraPointingAngle() { 
            return cameraPointingAngle;  
        }

        public double getDeviceTiltAngle() { 
            return deviceTiltAngle; 
        }

        public void startServer()
        {

            byte[] data = new byte[1024];
            IPEndPoint ipep = new IPEndPoint(IPAddress.Any, 5000);
            UdpClient newsock = new UdpClient(ipep);
            IPEndPoint sender = new IPEndPoint(IPAddress.Any, 0);

            // Waiting for input
            data = newsock.Receive(ref sender);

            // Writing to console
            Trace.WriteLine("Message received from {0}:", sender.ToString());
            Trace.WriteLine(Encoding.ASCII.GetString(data, 0, data.Length));


            // 
            string welcome = "Handshake: ready for SDP packet";
            data = Encoding.ASCII.GetBytes(welcome);
            newsock.Send(data, data.Length, sender);

            while (true)
            {
                data = newsock.Receive(ref sender);
                Queue<string> messages = new Queue<string>();
                string dataReceived = "";

                //---convert the data received into a string---
                dataReceived += Encoding.ASCII.GetString(data, 0, data.Length);
                //Trace.WriteLine(Encoding.ASCII.GetString(data, 0, data.Length));

                //---Break the message in to substrings ended with '###'---
                if (dataReceived.Length > 0 && dataReceived.IndexOf("###") >= 0)
                {
                    messages.Enqueue(dataReceived.Substring(0, dataReceived.IndexOf("###")));
                }

                //---Remove the data from the data recieved array
                if (dataReceived.IndexOf("###") != -1)
                {
                    dataReceived = dataReceived.Substring((dataReceived.IndexOf("###") + 3));
                }

                int messageCount = 0;
                String messageUsed = "";
                while (messages.Count() > 0)
                {

                    messageUsed = messages.Dequeue().Trim();
                    //Trace.WriteLine(messageUsed);

                    // Data message from the accelerometer 
                    if (messageUsed.StartsWith("DataPush: "))
                    {
                        processGraphData(messageUsed);
                    }
                    else if (messageUsed.StartsWith("ImageFile: "))
                    {

                    }
                    else if (messageUsed.StartsWith("OptiFlow: "))
                    {
                        processOptiFlowData(messageUsed);
                    }
                    else if (messageUsed.StartsWith("RealSenseData: "))
                    {
                        processRealSenseData(messageUsed);
                    }
                    else
                    {
                        Trace.WriteLine("Unknown command: " + Encoding.ASCII.GetString(data, 0, data.Length));
                    }

                    if (messageUsed.Contains("STOP"))
                    {
                        break;
                    }
                    messageCount++;
                }

                if (messageUsed.Contains("STOP"))
                {
                    break;
                }
            }
        }

        int count = 0;
        private void processGraphData(String messageIn)
        {
            messageIn = messageIn.Substring(10);
            string[] data1 = messageIn.Split(",");

            //Append(deviceAcc[0].Count, float.Parse(data1[0]));
            deviceAcc[0].Append(deviceAcc[0].Count, float.Parse(data1[1]));
            deviceAcc[1].Append(deviceAcc[1].Count, float.Parse(data1[2]));
            deviceAcc[2].Append(deviceAcc[2].Count, float.Parse(data1[2]));

            worldAcc[0].Append(worldAcc[0].Count, float.Parse(data1[3]));
            worldAcc[1].Append(worldAcc[1].Count, float.Parse(data1[4]));
            worldAcc[2].Append(worldAcc[2].Count, float.Parse(data1[5]));

            deviceVel[0].Append(deviceVel[0].Count, float.Parse(data1[6]));
            deviceVel[1].Append(deviceVel[1].Count, float.Parse(data1[7]));
            deviceVel[2].Append(deviceVel[2].Count, float.Parse(data1[8]));

            worldVel[0].Append(worldVel[0].Count, float.Parse(data1[9]));
            worldVel[1].Append(worldVel[1].Count, float.Parse(data1[10]));
            worldVel[2].Append(worldVel[2].Count, float.Parse(data1[11]));

            
            if (count % 10 == 0)
            {
                var random = new Random(0);
                Color? randomColor = Color.FromArgb(0xFF, (byte)random.Next(50, 255), (byte)random.Next(50, 255), (byte)random.Next(50, 255));
                positionVec.Append(float.Parse(data1[12]), float.Parse(data1[13]), float.Parse(data1[14]), new PointMetadata3D(randomColor, 1));
                positionVec.OnDataSeriesChanged(DataSeriesUpdate.DataChanged, DataSeriesAction.Update);
            }

            //positionVec.IsDirty = true;

            compassAngle = float.Parse(data1[15]);
            deviceTiltAngle = float.Parse(data1[16]);
            cameraPointingAngle = float.Parse(data1[17]) - 90;
        }

        private void processRealSenseData(String messageIn)
        {
            messageIn = messageIn.Substring(15);
            string[] data1 = messageIn.Split(",");

            //Append(deviceAcc[0].Count, float.Parse(data1[0]));
            realSenseVel[0].Append(realSenseVel[0].Count, float.Parse(data1[0]));
            realSenseVel[1].Append(realSenseVel[1].Count, float.Parse(data1[1]));
            realSenseVel[2].Append(realSenseVel[2].Count, (float)(float.Parse(data1[2])/10000.0));

            var random = new Random(0);
            Color? randomColor = Color.FromArgb(0xFF, (byte)random.Next(50, 255), (byte)random.Next(50, 255), (byte)random.Next(50, 255));
            realSensePositionVec.Append(float.Parse(data1[3]), float.Parse(data1[4]), float.Parse(data1[5]), new PointMetadata3D(randomColor, 1));
            realSensePositionVec.OnDataSeriesChanged(DataSeriesUpdate.DataChanged, DataSeriesAction.Update);
         

        }


        private void processOptiFlowData(String messageIn)
        {
            messageIn = messageIn.Substring(10).Trim();
            string[] data1 = messageIn.Split(new char[] {',',';'}, StringSplitOptions.RemoveEmptyEntries);
            Application.Current.Dispatcher.Invoke((Action)delegate
            {

            this.canvas.Children.Clear();

                for (int i = 0; i < data1.Length; i += 6)
                {

                    double x1 = int.Parse(data1[0 + i]) / 480.0;
                    double y1 = int.Parse(data1[1 + i]) / 270.0;

                    double x2 = int.Parse(data1[3 + i]) / 480.0;
                    double y2 = int.Parse(data1[4 + i]) / 270.0;

                    Line line = new Line();
                    line.Visibility = System.Windows.Visibility.Visible;
                    line.StrokeThickness = 1;
                    line.Stroke = System.Windows.Media.Brushes.Black;
                    line.X1 = x1 * this.canvas.ActualWidth;
                    line.X2 = x2 * this.canvas.ActualWidth;
                    line.Y1 = y1 * this.canvas.ActualHeight;
                    line.Y2 = y2 * this.canvas.ActualHeight;
                    this.canvas.Children.Add(line);
                }
            });
        }

    }


}
