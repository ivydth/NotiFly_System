package com.example.notifly_system;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin;
    private Button btnCreateAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        // Goes to Login screen
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, login_activity.class);
            startActivity(intent);
        });

        // Goes to Register screen
        btnCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, register_activity.class);
            startActivity(intent);
        });
    }
}