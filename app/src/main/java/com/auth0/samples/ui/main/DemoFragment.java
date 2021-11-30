package com.auth0.samples.ui.main;

import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.authentication.storage.SecureCredentialsManager;
import com.auth0.android.authentication.storage.SharedPreferencesStorage;
import com.auth0.android.callback.Callback;
import com.auth0.android.result.Credentials;
import com.auth0.samples.LoginActivity;
import com.auth0.samples.MainActivity;
import com.auth0.samples.R;

public class DemoFragment extends Fragment {

    private DemoViewModel mViewModel;
    private Auth0 auth0;
    private SecureCredentialsManager credentialsManager;
    private AuthenticationAPIClient apiClient;
    public static final String EXTRA_CLEAR_CREDENTIALS = "com.auth0.CLEAR_CREDENTIALS";
    public static final String EXTRA_ACCESS_TOKEN = "com.auth0.ACCESS_TOKEN";
    public static  final int AUTH_REQ_CODE = 99;
    public static DemoFragment newInstance() {
        return new DemoFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        auth0 = new Auth0(getActivity());
        apiClient = new AuthenticationAPIClient(auth0);
        SharedPreferencesStorage storage = new SharedPreferencesStorage(getActivity());
        credentialsManager = new SecureCredentialsManager(getActivity(),apiClient, storage);
        credentialsManager.requireAuthentication(getActivity(), AUTH_REQ_CODE, "Bio Fragment Test", "Please use device biometrics for Auth");
        refreshToken();
        return inflater.inflate(R.layout.demo_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(DemoViewModel.class);
        // TODO: Use the ViewModel
    }
    private void refreshToken() {
        credentialsManager.getCredentials(new Callback<Credentials, CredentialsManagerException>() {
            @Override
            public void onSuccess(Credentials credentials) {
                credentialsManager.saveCredentials(credentials);
                Log.d("From Fragment AT", credentials.getAccessToken());
                Log.d("From Fragment ID", credentials.getIdToken());
                Log.d("From Fragment RT", credentials.getRefreshToken());
                Log.d("From Fragment Scope", credentials.getScope());
                Log.d("From Fragment Expires At", credentials.getExpiresAt().toString());
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.putExtra(EXTRA_ACCESS_TOKEN, credentials.getAccessToken());
                startActivity(intent);
                getActivity().finish();                    }

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

}