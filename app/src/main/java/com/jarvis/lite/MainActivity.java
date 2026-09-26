package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int PEDIR_MICROFONE = 100;
    private static final int RECONHECER_VOZ = 101;

    private TextView resposta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout tela = new LinearLayout(this);
        tela.setOrientation(LinearLayout.VERTICAL);
        tela.setPadding(40, 80, 40, 40);

        TextView titulo = new TextView(this);
        titulo.setText("JARVIS LITE");
        titulo.setTextSize(28);

        resposta = new TextView(this);
        resposta.setText(
                "\nSistemas online.\n" +
                "Modo offline ativado.\n\n" +
                "Aguardando ativação..."
        );
        resposta.setTextSize(20);

        Button ativar = new Button(this);
        ativar.setText("🎙️ Ativar JARVIS");

        ativar.setOnClickListener(v -> iniciarVoz());

        tela.addView(titulo);
        tela.addView(resposta);
        tela.addView(ativar);

        setContentView(tela);
    }

    private void iniciarVoz() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PEDIR_MICROFONE
            );

            return;
        }

        ouvir();
    }

    private void ouvir() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "pt-BR"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "JARVIS está ouvindo..."
        );

        startActivityForResult(intent, RECONHECER_VOZ);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RECONHECER_VOZ &&
                resultCode == RESULT_OK &&
                data != null) {

            ArrayList<String> resultados =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (resultados != null && !resultados.isEmpty()) {

                String comando = resultados.get(0);

                resposta.setText(
                        "Comando reconhecido:\n\n" +
                        comando
                );
            }
        }
    }
            }
