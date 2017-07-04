package com.srm;

/**
 * Created by Administrator on 26/05/2017.
 */
public class GameParameters {
    private boolean drawRectManually = false;
    private int r = 255; //RED by default
    private int g = 0;
    private int b = 0;
    private int a = 0;
    private String imageKey = "https://test-764cc.firebaseio.com/images/-Kl-s7P8kBdE6Imyk1iu.json";
    private int numBoxes = 3;
    private int numAlternatives = 1;
    private int numSets = 2;
    private float presentationDuration = 10f;
    private float trialDuration = 3f;

    public boolean isDrawRectManually() {
        return drawRectManually;
    }

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }

    public int getA() {
        return a;
    }

    public String getImageKey() {
        return imageKey;
    }

    public int getNumBoxes() {
        return numBoxes;
    }

    public int getNumAlternatives() {
        return numAlternatives;
    }

    public int getNumSets() {
        return numSets;
    }

    public float getPresentationDuration() {
        return presentationDuration;
    }

    public float getTrialDuration() {
        return trialDuration;
    }
}
