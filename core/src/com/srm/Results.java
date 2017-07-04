package com.srm;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Created by Administrator on 25/05/2017.
 */
public class Results implements Json.Serializable {
    private String phoneNumber;
    private Array<Boolean> correctness = new Array<Boolean>();
    private Array<Long> latencies = new Array<Long>();

    public Results(String phoneNumber, Array<Boolean> correctness, Array<Long> latencies) {
        this.phoneNumber = phoneNumber;
        this.correctness = correctness;
        this.latencies = latencies;
    }

    @Override
    public void write(Json json) {
        json.writeFields(this);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        json.readFields(this, jsonData);
    }
}
