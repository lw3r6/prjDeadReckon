using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading.Tasks;

namespace OpticalFlowDashBoard
{
    public class Compass : INotifyPropertyChanged
    {
              public event PropertyChangedEventHandler PropertyChanged;

        int angle = 0;
        public int Angle
        {
            get {
                return angle;
            }
            set {
                angle = value;
                OnPropertyChanged("Angle");
            }
        }

        public static Compass getCompass()
        {
            var comp = new Compass()
            {
                Angle = 10,
            };
            return comp;
        }

        private void OnPropertyChanged(string property)
        {
            if (PropertyChanged != null)
                PropertyChanged(this, new PropertyChangedEventArgs(property));
        }
    }
}

