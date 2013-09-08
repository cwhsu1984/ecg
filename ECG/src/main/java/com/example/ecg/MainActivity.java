package com.example.ecg;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.content.Context;

import java.util.ArrayList;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new ECGView(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public class ECGView extends View {

        ArrayList<Pt> original = new ArrayList<Pt>(); // original data
        ArrayList<Pt> points = new ArrayList<Pt>(); // points to be drawn
        int radius = 10; // radius of circle
        float FACTOR = 3.0f; // factor to divide radius, smaller FACTOR has smooth path drawing
        int STEP = 2; // step to decrease argb, larger STEP will let the path glow longer
        int ALPHA = 150; // minimum alpha value of path
        int WHITE_DEC = 20; // decrement of WHITE
        int ALPHA_DEC = 5; // decrement of ALPHA
        int GREEN_DEC = 2; // decrement of GREEN
        int counter = 0; // counter
        ARGB argb; // current argb
        Paint paint = new Paint(); // paint
        Pt path[] = new Pt[] { // path to draw
                new Pt(100, 300),
                new Pt(150, 300),
                new Pt(200, 100),
                new Pt(250, 500),
                new Pt(300, 300),
                new Pt(400, 300),
                new Pt(450, 100),
                new Pt(500, 500),
                new Pt(550, 300),
                new Pt(650, 300),
        };

        // ARGB data holder class
        class ARGB {
            int a; // alpha
            int r; // red
            int g; // green
            int b; // blue
            ARGB(int a, int r, int g, int b) {
                this.a = a;
                this.r = r;
                this.g = g;
                this.b = b;
            }
        }

        // Point data holder class
        // We need float data for the point, otherwise, int data would create gap in the graph.
        class Pt {
            float x;
            float y;
            Pt(float x, float y) {
                this.x = x;
                this.y = y;
            }
        }

        ECGView(Context context) {
            super(context);
            // Generate original data points
            generateData();
            // Start a thread to shift data in the list
            myThread thread = new myThread();
            thread.start();
        }

        // generate data that will be drew on canvas
        void generateData() {
            for (int i = 1; i < path.length; i++) {
                Pt start = path[i - 1]; // start point
                Pt end = path[i]; // end point
                float distanceX = end.x - start.x; // x distance between start and end  
                float distanceY = end.y - start.y; // y distance between start and end
                int step; // step required from start to end
                float incrementX; // step increment of x
                float incrementY; // step increment of y
                float inc = radius / FACTOR; // minimum increment of each step

                // decide step by larger distance
                if (Math.abs(distanceX) > Math.abs(distanceY)) {
                    step = Math.abs((int) (distanceX / inc));
                } else {
                    step = Math.abs((int) (distanceY / inc));
                }
                incrementX = distanceX / step;
                incrementY = distanceY / step;

                float positionX = start.x;
                float positionY = start.y;
                for (int l = 0; l < step; positionX +=incrementX, positionY+= incrementY, l++) {
                    original.add(new Pt(positionX, positionY));
                }
            }
        }

        // control color and brightness value.
        ARGB getRGB() {
            // Adjust argb whenever counter reach the STEP
            if (++counter % STEP == 0) {
                if (argb.r > 0) {
                    // adjust value of the white part
                    argb.r -= WHITE_DEC;
                    argb.b -= WHITE_DEC;
                    if (argb.r < 0) {
                        argb.r = 0;
                        argb.b = 0;
                    }
                }
                else if (argb.a > ALPHA) {
                    // adjust alpha of the green part
                    argb.a-= ALPHA_DEC;
                } else if (argb.g > 0) {
                    // adjust value of the green part
                    argb.g -= GREEN_DEC;
                    if (argb.g < 0)
                        argb.g = 0;
                }
            }
            return argb;
        }

        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            //Debug.startMethodTracing("calc");

            canvas.drawColor(Color.BLACK);

            // head part of the line must be brightest.
            argb = new ARGB(255, 255, 255, 255);
            // sync the points list before get the size.
            int size = 0;
            synchronized (points) {
                size = points.size();
            }
            // draw points in array.
            for (int i = --size; i >= 0; i--) {
                Pt pt = points.get(i);
                // draw the point only when the color is not black yet.
                ARGB check = getRGB();
                if (check.g == 0) break;
                drawCircle(canvas, pt.x, pt.y, check);
            }
            //Debug.stopMethodTracing();
        }

        // draw circle by given argb value.
        void drawCircle(Canvas canvas, float x0, float y0, ARGB rgb) {
            paint.setARGB(rgb.a, rgb.r, rgb.g, rgb.b);
            canvas.drawCircle(x0, y0, radius, paint);
        }

        // thread shifts data in the point list.
        class myThread extends Thread {
            int i = 0;
            @Override
            public void run() {
                try {
                    while (true) {
                        // when the point list is full, remove the first element.
                        if (i >= original.size()) {
                            synchronized (points) {
                                points.remove(0);
                            }
                        }
                        // Add new element from original list to tail of point list.
                        synchronized (points) {
                            points.add(original.get(i++ % original.size()));
                        }
                        // sleep a little time and update.
                        Thread.sleep(10);
                        postInvalidate();
                    }
                } catch (InterruptedException e) {
                }
            }
        }
    }
}

