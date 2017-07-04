package com.srm;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Align;

/**
 * Created by Administrator on 23/05/2017.
 */
public class Box extends Actor {
    public static final int SHAPE_WIDTH = 100;
    public static final int SHAPE_HEIGHT = 100;

    private static boolean drawRect = true;
    private static boolean imageReady = false;

    private SRM game;
    private CustomShapeRenderer renderer;

    public Box(final SRM game, int x, int y) {
        this.game = game;
        this.renderer = game.getRenderer();

        super.setColor(game.getParams().getR(), game.getParams().getG(), game.getParams().getB(), game.getParams().getA());

        super.setBounds(x, y, SHAPE_WIDTH, SHAPE_HEIGHT);
        super.setOrigin(Align.center);
        super.setTouchable(Touchable.enabled);

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                Box.this.addAction(Actions.sequence(
                        Actions.parallel(Actions.scaleTo(1.2f, 1.2f, 0.05f), Actions.rotateBy(-10f, 0.05f, Interpolation.bounce)),
                        Actions.parallel(Actions.scaleTo(1f, 1f, 0.05f), Actions.rotateBy(10f, 0.05f, Interpolation.bounce))
                ));
                game.setSelectedBox(Box.this);
                return super.touchDown(event, x, y, pointer, button);
            }
        });
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);

        if (drawRect) { // as opposed to using an image downloaded from server
            drawRect();
        } else {
            drawImage(batch);
        }
    }

    public void drawRect() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        renderer.setColor(getColor());
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.rect(getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation());
        //renderer.roundedRect(getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation(), getHeight()/4);
        renderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void drawImage(Batch batch) {
        if (!imageReady && (game.getBoxImage() != null) && (game.getBoxImage() != null)) {
            imageReady = true;
        }
        if (imageReady) { // if the image isn't downloaded yet, don't try to draw it!!!
            Color oldColor = batch.getColor();
            batch.setColor(new Color(255, 255, 255, getColor().a));
            batch.draw(new TextureRegion(game.getBoxImage().getTexture()), getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation());
            batch.setColor(oldColor);
        } else {
            drawRect();
        }
    }

    @Override
    public void act(float delta) { // update state of actors
        super.act(delta);
    }

    public static void setDrawingParams(GameParameters params) { // set drawing parameters
        drawRect = params.isDrawRectManually();
    }
}