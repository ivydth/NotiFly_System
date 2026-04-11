package com.example.notifly_system;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class EmailHelper {

    private static final String EMAILJS_URL = "https://api.emailjs.com/api/v1.0/email/send";
    private static final String PUBLIC_KEY  = "juGO7uo9O6udgw2xl";

    // ── Welcome email (existing) ──────────────────────────────────
    private static final String WELCOME_SERVICE_ID  = "service_f3arbjq";
    private static final String WELCOME_TEMPLATE_ID = "template_bkf5619";

    // ── Password reset email (new) ────────────────────────────────
    private static final String RESET_SERVICE_ID    = "service_f3arbjq";
    private static final String RESET_TEMPLATE_ID   = "template_0wzz2sp";

    // ─────────────────────────────────────────────────────────────
    // Send welcome email on registration
    // ─────────────────────────────────────────────────────────────
    public static void sendWelcomeEmail(Context context, String firstName, String toEmail) {
        RequestQueue queue = Volley.newRequestQueue(context);
        try {
            JSONObject templateParams = new JSONObject();
            templateParams.put("user_name", firstName);
            templateParams.put("to_email",  toEmail);

            JSONObject data = new JSONObject();
            data.put("service_id",      WELCOME_SERVICE_ID);
            data.put("template_id",     WELCOME_TEMPLATE_ID);
            data.put("user_id",         PUBLIC_KEY);
            data.put("template_params", templateParams);

            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                EMAILJS_URL,
                data,
                response -> android.util.Log.d("EmailJS", "Welcome email sent!"),
                error    -> android.util.Log.e("EmailJS", "Failed to send welcome email: " + error.toString())
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("origin", "http://localhost");
                    return headers;
                }
            };

            queue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Send password reset notification email
    // ─────────────────────────────────────────────────────────────
    public static void sendPasswordResetEmail(Context context, String username, String toEmail) {
        RequestQueue queue = Volley.newRequestQueue(context);
        try {
            JSONObject templateParams = new JSONObject();
            templateParams.put("username", username);
            templateParams.put("to_email", toEmail);

            JSONObject data = new JSONObject();
            data.put("service_id",      RESET_SERVICE_ID);
            data.put("template_id",     RESET_TEMPLATE_ID);
            data.put("user_id",         PUBLIC_KEY);
            data.put("template_params", templateParams);

            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                EMAILJS_URL,
                data,
                response -> android.util.Log.d("EmailJS", "Password reset email sent!"),
                error    -> android.util.Log.e("EmailJS", "Failed to send reset email: " + error.toString())
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("origin", "http://localhost");
                    return headers;
                }
            };

            queue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
