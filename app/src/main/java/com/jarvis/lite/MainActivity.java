package com.jarvis.lite;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tela = new TextView(this);

        tela.setText(
                "JARVIS LITE\n\n" +
                "Sistemas online.\n" +
                "Modo offline ativado.\n\n" +
                "Aguardando ativação..."
        );

        tela.setTextSize(24);
        tela.setPadding(40, 80, 40, 40);

        setContentView(tela);
    }
}
