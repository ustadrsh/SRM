package com.srm;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Base64Coder;

/**
 * Created by Administrator on 26/05/2017.
 */
public class DownloadedImage {
    private String content; // image encoded as a base64 string, retrieved from the server
    private String name;
    private Texture texture;

    public Texture getTexture() {return texture;}

    public void setTexture(Texture texture) {this.texture = texture;}
    //private jpeg

    public void decodeAndStore(){
        final byte[] imageData = Base64Coder.decode(content.replace("data:image/png;base64,", ""));
        Pixmap pix = new Pixmap(imageData, 0, imageData.length); // convert image from base64 format to a pixmap
        texture = new Texture(pix);
    }

    public String getContent() {
        return content;
    }
}
