package com.example.emeowtions;

public class BoundingBox {
    private final float x1;
    private final float y1;
    private final float x2;
    private final float y2;
    private final float cx;
    private final float cy;
    private final float width;
    private final float height;
    private final float conf;
    private final int cls;
    private final String clsName;

    public BoundingBox(float x1, float y1, float x2, float y2, float cx, float cy, float width, float height, float conf, int cls, String clsName) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.cx = cx;
        this.cy = cy;
        this.width = width;
        this.height = height;
        this.conf = conf;
        this.cls = cls;
        this.clsName = clsName;
    }

    public float getX1() {
        return x1;
    }

    public float getY1() {
        return y1;
    }

    public float getX2() {
        return x2;
    }

    public float getY2() {
        return y2;
    }

    public float getCx() {
        return cx;
    }

    public float getCy() {
        return cy;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getConf() {
        return conf;
    }

    public int getCls() {
        return cls;
    }

    public String getClsName() {
        return clsName;
    }

    @Override
    public String toString() {
        return "BoundingBox{" +
                "x1=" + x1 +
                ", y1=" + y1 +
                ", x2=" + x2 +
                ", y2=" + y2 +
                ", cx=" + cx +
                ", cy=" + cy +
                ", width=" + width +
                ", height=" + height +
                ", conf=" + conf +
                ", cls=" + cls +
                ", clsName='" + clsName + '\'' +
                '}';
    }
}
