package com.example.pc.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void mostrarMenuAnalizar(View view){
        Intent intent = new Intent(getApplicationContext(), MenuAnalizar.class);
        startActivity(intent);
    }

    public void mostrarNotificaciones(View view){
        /*
        Intent intent = new Intent(getApplicationContext(), MenuAnalizar.class);
        startActivity(intent);
        */
        Toast.makeText(this, "Accion a implementar", Toast.LENGTH_SHORT).show();
    }

    public void salir(View view){
        System.exit(0);
    }
}
