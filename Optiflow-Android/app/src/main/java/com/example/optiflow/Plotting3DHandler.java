package com.example.optiflow;

import android.content.Context;
import android.util.Log;

import com.scichart.charting.visuals.axes.AutoRange;
import com.scichart.charting3d.model.dataSeries.xyz.XyzDataSeries3D;
import com.scichart.charting3d.visuals.SciChartSurface3D;
import com.scichart.charting3d.visuals.axes.NumericAxis3D;
import com.scichart.charting3d.visuals.camera.Camera3D;
import com.scichart.charting3d.visuals.pointMarkers.EllipsePointMarker3D;
import com.scichart.charting3d.visuals.renderableSeries.scatter.ScatterRenderableSeries3D;
import com.scichart.core.framework.UpdateSuspender;
import com.scichart.core.model.DoubleValues;
import com.scichart.extensions.builders.SciChartBuilder;
import com.scichart.extensions3d.builders.SciChart3DBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


class Plotting3DHandler {

    private final XyzDataSeries3D<Double, Double, Double> dataSeries = new XyzDataSeries3D<>(Double.class, Double.class, Double.class);
    private final DoubleValues xData = new DoubleValues();
    private final DoubleValues yData = new DoubleValues();
    private final DoubleValues zData = new DoubleValues();
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    SciChartSurface3D surface3d;
    private EllipsePointMarker3D pointMarker3D;
    private ScheduledFuture<?> schedule;
    private volatile boolean isRunning = true;


    Plotting3DHandler(SciChartSurface3D graphViewIn, Context cont) {

        surface3d = graphViewIn;
        SciChartBuilder.init(cont);

        //Initialize the 3D chart
        SciChart3DBuilder.init(cont);
        final SciChart3DBuilder sciChart3DBuilder = SciChart3DBuilder.instance();
        final Camera3D camera = sciChart3DBuilder.newCamera3D().build();

        //Add the Axes onto the chart and define the initial markings
        final NumericAxis3D xAxis = sciChart3DBuilder.newNumericAxis3D().withGrowBy(.1, .1).build();
        final NumericAxis3D yAxis = sciChart3DBuilder.newNumericAxis3D().withGrowBy(.1, .1).build();
        final NumericAxis3D zAxis = sciChart3DBuilder.newNumericAxis3D().withGrowBy(.1, .1).build();

        pointMarker3D = sciChart3DBuilder.newEllipsePointMarker3D()
                .withFill(0x77ADFF2F)
                .withSize(2f)
                .build();

        final ScatterRenderableSeries3D scatterRenderableSeries3D = sciChart3DBuilder.newScatterSeries3D()
                .withDataSeries(dataSeries)
                .withPointMarker(pointMarker3D)
                .build();

        //Make the 3d chart scale with data
        yAxis.setAutoRange(AutoRange.Always);
        xAxis.setAutoRange(AutoRange.Always);
        zAxis.setAutoRange(AutoRange.Always);

        //Add initial point to the 3d chart
        xData.add((0));
        yData.add((0));
        zData.add((0));

        //Add 0 point to the data set
        dataSeries.append(xData, yData, zData);

        // Update the graph with the new data point.
        UpdateSuspender.using(surface3d, () -> {
            surface3d.setCamera(camera);
            surface3d.setXAxis(xAxis);
            surface3d.setYAxis(yAxis);
            surface3d.setZAxis(zAxis);
            surface3d.getRenderableSeries().add(scatterRenderableSeries3D);
            surface3d.getChartModifiers().add(sciChart3DBuilder.newModifierGroupWithDefaultModifiers().build());
        });

        //Schedule the graph to update with new data every 10 miliseconds
        schedule = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {

                    @Override
                    public void run() {
                        if (!isRunning) {
                            return;
                        }
                        UpdateSuspender.using(surface3d, new Runnable() {
                            @Override
                            public void run() {
                                dataSeries.append(xData, yData, zData);
                            }
                        });
                    }
                }, 0, 100, TimeUnit.MILLISECONDS);

    }


    void appendPoints(float vectX, float vectY, float vectZ) {
        //Add data points to world acceleration series
        xData.add(vectX);
        yData.add(vectY);
        zData.add(vectZ);
    }

    public void initData() {

        xData.clear();
        yData.clear();
        zData.clear();
        dataSeries.clear(false);


        //Add 0 point to the data set
        dataSeries.append(xData,yData,zData);
    }



}
