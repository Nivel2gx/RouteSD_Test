package com.devxtend.sdmapadvanced;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.util.Log;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matiaspreciozzi on 4/4/18.
 */

public class MarkerAnimation {

    private static final String TAG = "DX-";

    private int index, next;
    private LatLng startPosition, endPosition;
    private List<LatLng> polyLineList;
    private float v;
    private double lat, lng;
    private Marker marker;
    private Handler handler;

    public MarkerAnimation(List<LatLng> lineList, Marker mMarker){
        this.polyLineList = lineList; //filterLatLng(lineList);
        this.marker = mMarker;
    }

    /**
     * Comienza la animación del marker.
     */
    public void startAnimation(){

        index = -1;
        next = 1;

        handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {

                if (index < polyLineList.size() - 1) {
                    index++;
                    next = index + 1;
                }

                if (index < polyLineList.size() - 1) {
                    startPosition = polyLineList.get(index);
                    endPosition = polyLineList.get(next);
                }

                ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
                valueAnimator.setDuration(3000);
                valueAnimator.setInterpolator(new LinearInterpolator());

                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        v = valueAnimator.getAnimatedFraction();

                        lng = v * endPosition.longitude + (1 - v)
                                * startPosition.longitude;
                        lat = v * endPosition.latitude + (1 - v)
                                * startPosition.latitude;

                        LatLng newPos = new LatLng(lat, lng);

                        marker.setPosition(newPos);
                        marker.setAnchor(0.5f, 0.5f);
                        marker.setRotation(getBearing(startPosition, newPos));

                    }

                });

                valueAnimator.start();

                handler.postDelayed(this, 3000);
            }
        }, 3000);

    }

    /**
     * Calcula el angulo de giro.
     * @param begin
     * @param end
     * @return
     */
    private float getBearing(LatLng begin, LatLng end) {
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }

}
