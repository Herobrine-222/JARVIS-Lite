package com.jarvis.lite;

import android.os.Bundle;
import android.service.voice.VoiceInteractionSessionService;

public class VoiceInteractionSessionService
        extends VoiceInteractionSessionService {

    @Override
    public android.service.voice.VoiceInteractionSession onNewSession(
            Bundle args) {

        return new com.jarvis.lite.VoiceInteractionSession(
                this
        );
    }
        }
