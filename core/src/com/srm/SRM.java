package com.srm;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.dialog.Dialogs;

import java.util.Arrays;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

public class SRM implements ApplicationListener {
    private Color[] colors = new Color[]{Color.WHITE, Color.YELLOW, Color.BLUE, Color.GREEN, Color.PINK};

    private CustomShapeRenderer renderer; // to paint
    private Array<Box> levelBoxes = new Array<Box>();
    private Array<Box> candidateBoxes = new Array<Box>();
    private ProgressBar progressBar; // to tell users how much time they have left
    private Stage stage;
    private Stage ui;

    // Inputs
    private int currentLevel = 0;

    // Outpus
    private Array<Boolean> correctness = new Array<Boolean>();
    private Array<Long> latencies = new Array<Long>();

    private String phoneNumber;

    final int PREPLAY = 0;
    final int PLAY = 1;
    final int POSTPLAY = 2;
    private int state = PREPLAY;
    private boolean started = false;
    private boolean finished = false;

    float delta;

    private long lastScreen = TimeUtils.millis();

    private int selectedBox = -1;
    private long lastTap = -1;

    private DownloadedImage boxImage;
    private GameParameters params;

    public class InstructionDialog extends Dialogs.DetailsDialog { //

        public InstructionDialog() {
            super("Click 'details' for to get instructions, and OK to skip.", "Instructions", "First, you will be shown a screen of objects. Whenever the\n progressbar appears yellow, you must memorize the position of\nthe objects. In the following screens, you must click on the box that\nwas shown in the respective initial sequence.");
        }

        @Override
        protected void result(Object object) {
            super.result(object);
            int buttonId = (Integer) object; // Clicked object
            if (buttonId == 1) { // "OK" button
                lastScreen = TimeUtils.millis();
                started = true; // starts the game
            }
        }
    }

    public class CustomTextInputListener implements Input.TextInputListener {
        @Override
        public void input(String number) { // called with user input as parameter
            phoneNumber = number;
            stage.addActor(new InstructionDialog());
        }

        @Override
        public void canceled() {
            Gdx.app.exit(); // exits application
        }
    }

    @Override
    public void create() { // called upon opening the app
        setParams(); // set parameters from online
        VisUI.load(); // prepare dialog box
        CustomTextInputListener listener = new CustomTextInputListener(); // prepare text input
        Gdx.input.getTextInput(listener, "Enter your phone number below: ", "", "Your phone number here");
        stage = new Stage(new ScalingViewport(Scaling.fillX, 480, 800));
        ui = new Stage(new ScalingViewport(Scaling.fillX, 480, 800));
        renderer = new CustomShapeRenderer(); // to draw shapes
        progressBar = new ProgressBar(renderer, 10);
        Gdx.input.setInputProcessor(stage); // prepares stage to detect input
        ui.addActor(progressBar);
        freshTrial();
    }

    @Override
    public void render() { // called every frame
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        delta = Gdx.graphics.getDeltaTime();
        if (started) {
            switch (state) {
                case PREPLAY:
                    prePlay();
                    break;
                case PLAY:
                    play();
                    break;
                case POSTPLAY:
                    postPlay();
                    break;
            }
        }
        renderer.setProjectionMatrix(stage.getBatch().getProjectionMatrix());
        renderer.setTransformMatrix(stage.getBatch().getTransformMatrix());

        stage.act(delta);
        ui.act(delta);
        stage.draw();
        ui.draw();
    }

    public void setParams() { // set parameters from server
        getParamsFromServer(); // obtain parameters from server
        Box.setDrawingParams(params);
    }

    public void prePlay() {
        // more comprehensive tutorial here
        state = PLAY;
    }

    public void play() { // Main logic
        if (currentLevel > params.getNumBoxes() * params.getNumSets()) { // Main game finished
            state = POSTPLAY;
            return;
        }
        if (levelElapsedMillis() >= levelDurationMillis()) {
            for (Box box : candidateBoxes) {
                box.addAction(sequence(Actions.fadeOut(0.5f), Actions.removeActor()));
            }
            candidateBoxes.clear();

            if (!isPresentationStage()) { // Not the presentation stage
                boolean correct = selectedBox != -1 && selectedBox == (currentLevel % params.getNumBoxes() - 1);
                correctness.add(correct);
                latencies.add(lastTap == -1 || (lastTap - lastScreen) < 0 ? (long) (1000L * params.getTrialDuration()) : (lastTap - lastScreen)); // No response from user

                selectedBox = -1;
                lastTap = -1;

                System.out.println("\n" + correctness.size + " Elements.");
                System.out.println("Correct: " + Arrays.toString(correctness.items));
                System.out.println("Latencies: " + Arrays.toString(latencies.items));
                System.out.println("\n");
            }

            if (currentLevel % (params.getNumBoxes() + 1) == params.getNumBoxes()) { // Last level of set
                freshTrial(); // New 'set'
            } else { // Not the last level of a particular set
                for (Box box : levelBoxes) {
                    if (box == levelBoxes.get(currentLevel % params.getNumBoxes())) {
                        box.addAction(parallel(Actions.visible(true), Actions.alpha(0), sequence(delay(0.5f), Actions.fadeIn(0.5f))));
                    } else {
                        box.addAction(sequence(Actions.fadeOut(0.5f), Actions.visible(false)));
                    }
                }
                candidateBoxes.addAll(generateBoxes(params.getNumAlternatives(), colors));

                for (Box candidateBox : candidateBoxes) {
                    stage.addActor(candidateBox);
                    candidateBox.addAction(sequence(Actions.alpha(0), delay(0.5f), Actions.fadeIn(0.5f)));
                }
            }
            currentLevel++; // next screen for a given sequence of boxes
            lastScreen = TimeUtils.millis();
        }
        updateProgressBar();
    }

    public void postPlay() { // runs every frame, at the end
        // could be for a score page
        if (!finished) {
            finish();
        }
    }

    public void finish() { //runs once
        finished = true;
        sendAnalytics();
        Gdx.app.exit(); //exits
    }

    public void freshTrial() {
        for (Box box : levelBoxes) box.remove();
        levelBoxes.clear();
        levelBoxes.addAll(generateBoxes(params.getNumBoxes(), null));
        for (Box box : levelBoxes) {
            stage.addActor(box);
            box.addAction(
                    parallel(
                            sequence(alpha(0), delay(0.5f), fadeIn(0.5f)),
                            repeat(5, sequence(
                                    scaleTo(1.1f, 1.1f, params.getPresentationDuration() / 10),
                                    scaleTo(1f, 1f, params.getPresentationDuration() / 10)
                            ))
                    ));
        }
    }

    public Array<Box> generateBoxes(int count, Color[] colors) {
        Array<Box> generatedBoxes = new Array<Box>();

        Array<Box> allBoxes = new Array<Box>(levelBoxes);
        allBoxes.addAll(candidateBoxes);

        int candidateX = 0, candidateY = 0;
        for (int boxNumber = 1; boxNumber <= count; boxNumber++) { // Ensure blocks don't clash with each other
            boolean unique = false;
            while (!unique) {
                candidateX = MathUtils.random(0, 480 - Box.SHAPE_WIDTH);
                candidateY = MathUtils.random(0, 800 - Box.SHAPE_HEIGHT);

                Rectangle candidateRectangle = new Rectangle(candidateX, candidateY, Box.SHAPE_WIDTH + 20f, Box.SHAPE_HEIGHT + 20f);

                unique = true;
                for (Box box : allBoxes) {
                    if (candidateRectangle.overlaps(new Rectangle(box.getX(), box.getY(), box.getWidth(), box.getHeight()))) {
                        unique = false;
                        break;
                    }
                }
            }

            Box box = new Box(this, candidateX, candidateY);

            generatedBoxes.add(box);
            allBoxes.add(box);
        }
        return generatedBoxes;
    }

    public void getParamsFromServer() {
        params = new GameParameters();
        final Json converter = new Json();
        converter.setOutputType(JsonWriter.OutputType.json);
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        final Net.HttpRequest httpRequest = requestBuilder.newRequest() // get request to get parameters
                .method(Net.HttpMethods.GET).header("Content-Type", "application/json")
                .url("https://test-764cc.firebaseio.com/settings/~askljdf.json")
                .build();
        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String response = httpResponse.getResultAsString();
                if ((httpResponse.getStatus().getStatusCode() / 100) == 1) { //successful
                    System.out.println("UPDATING PARAMS");
                    params = converter.fromJson(GameParameters.class, response); // response saved into params object belonging to this class
                }
                if (!params.isDrawRectManually()) { // if configured to use image from server
                    getImageFromServer();
                }
            }

            @Override
            public void failed(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void cancelled() {
            }
        });
    }

    public void getImageFromServer() { // download image to use in game
        final Json converter = new Json();
        converter.setOutputType(JsonWriter.OutputType.json);

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        final Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET).header("Content-Type", "application/json")
                .url(params.getImageKey())
                .build();
        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String response = httpResponse.getResultAsString();
                if (httpResponse.getStatus().getStatusCode() / 100 == 2) {

                    final DownloadedImage image = converter.fromJson(DownloadedImage.class, response);

                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            image.decodeAndStore();
                            boxImage = image;
                        }
                    });
                }

            }

            @Override
            public void failed(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void cancelled() {
            }
        });
    }

    public void sendAnalytics() { // send scores to server using a POST request
        Json converter = new Json();
        converter.setOutputType(JsonWriter.OutputType.json);
        String json = converter.toJson(new Results(phoneNumber, correctness, latencies), Results.class);

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        final Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.POST).header("Content-Type", "application/json")
                .url("https://alzheimers-early-detection-mas.firebaseio.com/colorsArray.json")
                .content(json).build();
        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                // can print for debugging
            }

            @Override
            public void failed(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void cancelled() {

            }
        });
    }

    public void setSelectedBox(Box selectedBox) { // when a box is clicked
        for (int i = 0; i < levelBoxes.size; i++)
            if (levelBoxes.get(i) == selectedBox) {
                this.selectedBox = i;
                break;
            }
        this.lastTap = TimeUtils.millis();
    }

    public void updateProgressBar() { // give user an indication of how much time is left for the current set
        //progressBar.addAction(Actions.moveTo(currentLevel / (float) (numBoxes * numSets), progressBar.getY(), 0.2f, Interpolation.pow2InInverse));
        progressBar.addAction(scaleTo(((float) levelElapsedMillis()) / ((float) levelDurationMillis()), 1f, 0.5f, Interpolation.pow2InInverse));
        progressBar.addAction(color((isPresentationStage() ? Color.YELLOW : Color.GREEN), 1f));
    }

    public long levelDurationMillis() {
        return 1000 * ((long) (currentLevel % (params.getNumBoxes() + 1) == 0 ? params.getPresentationDuration() : params.getTrialDuration()));
    }

    public long levelElapsedMillis() {
        return TimeUtils.millis() - lastScreen;
    }

    public boolean isPresentationStage() {
        return ((currentLevel % (params.getNumBoxes() + 1)) == 0);
    }

    public Array<Boolean> getCorrectness() {
        return correctness;
    }

    public Array<Long> getLatencies() {
        return latencies;
    }

    public CustomShapeRenderer getRenderer() {
        return this.renderer;
    }

    public GameParameters getParams() {
        return this.params;
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void resize(int width, int height) {
    }

    public DownloadedImage getBoxImage() {
        return boxImage;
    }

    @Override
    public void dispose() {
        stage.dispose();
        renderer.dispose();
    }
}