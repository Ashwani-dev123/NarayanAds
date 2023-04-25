package com.ads.narayan.widget.spinkit;


import com.ads.narayan.widget.spinkit.sprite.Sprite;
import com.ads.narayan.widget.spinkit.style.ChasingDots;
import com.ads.narayan.widget.spinkit.style.Circle;
import com.ads.narayan.widget.spinkit.style.CubeGrid;
import com.ads.narayan.widget.spinkit.style.DoubleBounce;
import com.ads.narayan.widget.spinkit.style.FadingCircle;
import com.ads.narayan.widget.spinkit.style.FoldingCube;
import com.ads.narayan.widget.spinkit.style.MultiplePulse;
import com.ads.narayan.widget.spinkit.style.MultiplePulseRing;
import com.ads.narayan.widget.spinkit.style.Pulse;
import com.ads.narayan.widget.spinkit.style.PulseRing;
import com.ads.narayan.widget.spinkit.style.RotatingCircle;
import com.ads.narayan.widget.spinkit.style.RotatingPlane;
import com.ads.narayan.widget.spinkit.style.ThreeBounce;
import com.ads.narayan.widget.spinkit.style.WanderingCubes;
import com.ads.narayan.widget.spinkit.style.Wave;


public class SpriteFactory {

    public static Sprite create(Style style) {
        Sprite sprite = null;
        switch (style) {
            case ROTATING_PLANE:
                sprite = new RotatingPlane();
                break;
            case DOUBLE_BOUNCE:
                sprite = new DoubleBounce();
                break;
            case WAVE:
                sprite = new Wave();
                break;
            case WANDERING_CUBES:
                sprite = new WanderingCubes();
                break;
            case PULSE:
                sprite = new Pulse();
                break;
            case CHASING_DOTS:
                sprite = new ChasingDots();
                break;
            case THREE_BOUNCE:
                sprite = new ThreeBounce();
                break;
            case CIRCLE:
                sprite = new Circle();
                break;
            case CUBE_GRID:
                sprite = new CubeGrid();
                break;
            case FADING_CIRCLE:
                sprite = new FadingCircle();
                break;
            case FOLDING_CUBE:
                sprite = new FoldingCube();
                break;
            case ROTATING_CIRCLE:
                sprite = new RotatingCircle();
                break;
            case MULTIPLE_PULSE:
                sprite = new MultiplePulse();
                break;
            case PULSE_RING:
                sprite = new PulseRing();
                break;
            case MULTIPLE_PULSE_RING:
                sprite = new MultiplePulseRing();
                break;
            default:
                break;
        }
        return sprite;
    }
}
