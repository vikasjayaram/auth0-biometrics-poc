package com.auth0.samples;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.authentication.storage.SecureCredentialsManager;
import com.auth0.android.authentication.storage.SharedPreferencesStorage;
import com.auth0.android.callback.Callback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private Auth0 auth0;
    private SecureCredentialsManager credentialsManager;
    private AuthenticationAPIClient apiClient;
    public static final String EXTRA_CLEAR_CREDENTIALS = "com.auth0.CLEAR_CREDENTIALS";
    public static final String EXTRA_ACCESS_TOKEN = "com.auth0.ACCESS_TOKEN";
    public static  final int AUTH_REQ_CODE = 99;
    // Biometrics

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        Button rtAuth0Button = findViewById(R.id.rtAuth0Button);
        rtAuth0Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAuth0Biometric();
            }
        });
        Button rtSystemButton = findViewById(R.id.rtSystemButton);
        rtSystemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestBiometricPrompt();
            }
        });

        Button rtFragmentButton = findViewById(R.id.rtFragmentButton);
        rtFragmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, DemoActivity.class);
                startActivity(intent);
                finish();
            }
        });
        // Biometrics init
        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(LoginActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                        "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(),
                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                refreshToken();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for my app")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password")
                .build();
        // Auth0
        auth0 = new Auth0(this);
        apiClient = new AuthenticationAPIClient(auth0);
        SharedPreferencesStorage storage = new SharedPreferencesStorage(this);
        credentialsManager = new SecureCredentialsManager(this,apiClient, storage);
        //if (credentialsManager.hasValidCredentials()) {
        //}
        //Check if the activity was launched to log the user out
        if (getIntent().getBooleanExtra(EXTRA_CLEAR_CREDENTIALS, false)) {
            logout();
        }
    }

    private void login() {
        WebAuthProvider.login(auth0)
                .withScheme("demo")
                .withAudience("https://api.billing.com")
                .withScope("openid profile email offline_access read:bill")
                .start(this, new Callback<Credentials, AuthenticationException>() {

                    @Override
                    public void onFailure(@NonNull final AuthenticationException exception) {
                        Toast.makeText(LoginActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(@Nullable final Credentials credentials) {
                        credentialsManager.saveCredentials(credentials);
                        Log.d("AT", credentials.getAccessToken());
                        Log.d("ID", credentials.getIdToken());
                        Log.d("RT", credentials.getRefreshToken());
                        Log.d("Scope", credentials.getScope());
                        Log.d("Expires At", credentials.getExpiresAt().toString());
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra(EXTRA_ACCESS_TOKEN, credentials.getAccessToken());
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void logout() {
        WebAuthProvider.logout(auth0)
                .withScheme("demo")
                .start(this, new Callback<Void, AuthenticationException>() {
                    @Override
                    public void onSuccess(@Nullable Void payload) {

                    }

                    @Override
                    public void onFailure(@NonNull AuthenticationException error) {
                        //Log out canceled, keep the user logged in
                        showNextActivity();
                    }
                });
    }

    private void showNextActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void refreshToken() {
        credentialsManager.getCredentials(new Callback<Credentials, CredentialsManagerException>() {
            @Override
            public void onSuccess(Credentials credentials) {
                credentialsManager.saveCredentials(credentials);
                Log.d("AT", credentials.getAccessToken());
                Log.d("ID", credentials.getIdToken());
                Log.d("RT", credentials.getRefreshToken());
                Log.d("Scope", credentials.getScope());
                Log.d("Expires At", credentials.getExpiresAt().toString());
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra(EXTRA_ACCESS_TOKEN, credentials.getAccessToken());
                startActivity(intent);
                finish();                    }

            @Override
            public void onFailure(CredentialsManagerException e) {
                e.printStackTrace();
            }
        });
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (credentialsManager.checkAuthenticationResult(requestCode, resultCode)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void requestAuth0Biometric() {
        credentialsManager.requireAuthentication(this, AUTH_REQ_CODE, "Bio Test", "Please use device biometrics for Auth");
        refreshToken();
    }

    public void requestBiometricPrompt() {
        biometricPrompt.authenticate(promptInfo);
    }
}
