using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using SciChart.Charting.Visuals;

namespace OpticalFlowDashBoard
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : Application
    {
        public App()
        {
            // Set this code once in App.xaml.cs or application startup
            // Set this code once in App.xaml.cs or application startup
            SciChartSurface.SetRuntimeLicenseKey("DsQ4Yg8uJC9bZtbL4Sozb/CqFzSiP+gwMiAEE3NE9ONiGEtUnlkNcy+bTgys35SIP89+M50ZVqccueCcbwr/2C4CaFoQAMhKycvMvq3oFGHsgP23CT5BtfW/IFbBCDfhEhcrCoLVjcNL+ZbKtMOkt8GuMeJr2xRqPlF0ttwQUDYWq+ss4IJ9JfvzOBznaAiMUYuvo/4QfG7p92hBeqqjcMPEGI7ZZnx6BJ9Peccw/C//cilPDH5ddc6k4VTL/AhGGyGGp4DYPvCaSbWZ4slbpZiDL1VDVDgdwQh0eeDaiTESfRrfgiQpL3h123s+ZggPyVtHoHDJ5QE8sx7URGSiAa4elg0cZv5IkY6R28NzFsAk+XN65AIJAqNFDuxh3Xqv8904Ih0JxF7IV0U+N2BmiDWVHW1G7DNAdXMpiAnovJwG+8BC6m4z7hRLai0eSdXZ4DZQ2TqWAxFruBN3jgQ3bNEFZ119LOF0oax0WGihTuLoSVUBYOsYoyGbDai1xd1H7jCE2ZPqRh7Hp4RbIs6L9z8BRte1n4ThcBAttcb/7qfk/UuwIP4=");
            Unosquare.FFME.Library.FFmpegDirectory = @"c:\ffmpeg";
        }
    }

}
