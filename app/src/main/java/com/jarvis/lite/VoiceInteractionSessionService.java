package com.jarvis.lite;

import android.os.Bundle;

public class VoiceInteractionSessionService
        extends android.service.voice.VoiceInteractionSessionService {

    @Override
    public android.service.voice.VoiceInteractionSession onNewSession(
            Bundle args) {

        return new com.jarvis.lite.VoiceInteractionSession(
                this
        );
    }
}
