package com.example.dell_15.final_call;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseObject;

/**
 * Created by pc on 12/1/2015.
 */
public class ParseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.enableLocalDatastore(this);

        Parse.initialize(this, "wuKOMiIyw9mo579ITKCuAR5lz5OoiIG1m5K9krEG", "BmQtKZdNaFr2Mn3Hi4cgFs1JOXLA3JYcB1KKEv8y");

    }
}
