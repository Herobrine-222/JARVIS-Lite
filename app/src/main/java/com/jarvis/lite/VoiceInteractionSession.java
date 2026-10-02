package com.jarvis.lite;

import android.os.Bundle;

public class VoiceInteractionSession
        extends android.service.voice.VoiceInteractionSession {

    public VoiceInteractionSession(
            android.content.Context context) {

        super(context);
    }

    @Override
    public void onShow(
            Bundle args,
            int showFlags) {

        super.onShow(
                args,
                showFlags
        );
    }

    @Override
    public void onHide() {

        super.onHide();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }
}
