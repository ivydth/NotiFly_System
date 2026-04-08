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
    private static final String SERVICE_ID = "service_f3arbjq";    // 🔁 Replace this
    private static final String TEMPLATE_ID = "template_bkf5619";  // 🔁 Replace this
    private static final String PUBLIC_KEY = "juGO7uo9O6udgw2xl";    // 🔁 Replace this

    public static void sendWelcomeEmail(Context context, String firstName, String toEmail) {
        RequestQueue queue = Volley.newRequestQueue(context);

        try {
            JSONObject templateParams = new JSONObject();
            templateParams.put("user_name", firstName);
            templateParams.put("to_email", toEmail);

            JSONObject data = new JSONObject();
            data.put("service_id", SERVICE_ID);
            data.put("template_id", TEMPLATE_ID);
            data.put("user_id", PUBLIC_KEY);
            data.put("template_params", templateParams);

            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                EMAILJS_URL,
                data,
                response -> android.util.Log.d("EmailJS", "Email sent successfully!"),
                error -> android.util.Log.e("EmailJS", "Failed to send email: " + error.toString())
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
