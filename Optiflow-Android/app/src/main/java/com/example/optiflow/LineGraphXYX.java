package com.example.optiflow;

import android.content.Context;
import android.view.animation.DecelerateInterpolator;

import com.scichart.charting.model.dataSeries.IXyDataSeries;
import com.scichart.charting.visuals.SciChartSurface;
import com.scichart.charting.visuals.axes.AutoRange;
import com.scichart.charting.visuals.axes.IAxis;
import com.scichart.charting.visuals.renderableSeries.FastLineRenderableSeries;
import com.scichart.charting.visuals.renderableSeries.IRenderableSeries;
import com.scichart.core.framework.UpdateSuspender;
import com.scichart.core.model.FloatValues;
import com.scichart.core.model.IntegerValues;
import com.scichart.extensions.builders.SciChartBuilder;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


class LineGraphXYX {

    private int FIFOAMT = 1000;
    private final FloatValues posData = new FloatValues();
    private final FloatValues xData = new FloatValues();
    private final FloatValues yData = new FloatValues();
    private final FloatValues zData = new FloatValues();
    private IXyDataSeries<Float, Float> xDataSeries;
    private IXyDataSeries<Float, Float> yDataSeries;
    private IXyDataSeries<Float, Float> zDataSeries;
    private SciChartSurface graphView;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> schedule;
    private volatile boolean isRunning = true;

    LineGraphXYX(SciChartSurface graphViewIn, int fifoAmt, Context cont) {

        graphView = graphViewIn;
        SciChartBuilder.init(cont);

        this.FIFOAMT = fifoAmt;
        final SciChartBuilder sciChartBuilder = SciChartBuilder.instance();

        final IAxis xAxis = sciChartBuilder.newNumericAxis().withGrowBy(0.01d, 0.01d).withAutoRangeMode(AutoRange.Always).build();
        final IAxis yAxis = sciChartBuilder.newNumericAxis().withGrowBy(0.01d, 0.01d).withAutoRangeMode(AutoRange.Always).build();

        xDataSeries = sciChartBuilder.newXyDataSeries(Float.class, Float.class).withFifoCapacity(FIFOAMT).build();
        yDataSeries = sciChartBuilder.newXyDataSeries(Float.class, Float.class).withFifoCapacity(FIFOAMT).build();
        zDataSeries = sciChartBuilder.newXyDataSeries(Float.class, Float.class).withFifoCapacity(FIFOAMT).build();

        //Fill charts

        initData();



// Create and configure a Line Series
        //Red
        IRenderableSeries xlineSeries = sciChartBuilder.newLineSeries()
                .withDataSeries(xDataSeries)
                .withStrokeStyle(0xFFE13219, 1f, true)
                .build();

        //Orange
        IRenderableSeries ylineSeries = sciChartBuilder.newLineSeries()
                .withDataSeries(yDataSeries)
                .withStrokeStyle(0xFFFFA500, 1f, true)
                .build();

        //Blue
        IRenderableSeries zlineSeries = sciChartBuilder.newLineSeries()
                .withDataSeries(zDataSeries)
                .withStrokeStyle(0xFF4083B7, 1f, true)
                .build();

        UpdateSuspender.using(graphView, new Runnable() {
            @Override
            public void run() {
                Collections.addAll(graphView.getXAxes(), xAxis);
                Collections.addAll(graphView.getYAxes(), yAxis);
                Collections.addAll(graphView.getRenderableSeries(), xlineSeries, ylineSeries, zlineSeries);
                Collections.addAll(graphView.getChartModifiers(), sciChartBuilder.newModifierGroupWithDefaultModifiers().build());
            }
        });

        schedule = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    return;
                }
                UpdateSuspender.using(graphView, new Runnable() {
                    @Override
                    public void run() {
                        updateChart();
                    }
                });
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }


    private int t = FIFOAMT + 1;
    void appendPoints(float vectX, float vectY, float vectZ) {
        //Add data points to world acceleration series
        posData.add(t++);
        xData.add(vectX);
        yData.add(vectY);
        zData.add(vectZ);
    }

    private void updateChart() {
        xDataSeries.append(posData, xData);
        yDataSeries.append(posData, yData);
        zDataSeries.append(posData, zData);
    }

    public void initData() {

        posData.clear();
        xData.clear();
        yData.clear();
        zData.clear();

        //Fill Chart
        for (int i = 0; i < FIFOAMT; i++) {
            posData.add((0));
            xData.add((0));
            yData.add(0);
            zData.add(0);
        }

        //Add 0 point to the data set
        xDataSeries.append(posData, xData);
        yDataSeries.append(posData, yData);
        zDataSeries.append(posData, zData);


        t = FIFOAMT + 1;
    }

}
