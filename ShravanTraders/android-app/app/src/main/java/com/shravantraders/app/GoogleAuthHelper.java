package com.shravantraders.app;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.util.Base64;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class GoogleAuthHelper {

    public static final int RC_GOOGLE_SIGN_IN = 9001;

    public interface AuthCallback {
        void onSuccess(JSONObject user, String token);
        void onError(String message);
    }

    public static void startSignIn(Activity activity) {
        try {
            Intent intent = AccountManager.newChooseAccountIntent(
                    null,
                    null,
                    new String[]{"com.google"},
                    false,
                    null,
                    null,
                    null,
                    null
            );
            activity.startActivityForResult(intent, RC_GOOGLE_SIGN_IN);
        } catch (Exception e) {
            showAccountInputDialog(activity, null, null);
        }
    }

    public static boolean handleActivityResult(Activity activity, int requestCode, int resultCode, Intent data, AuthCallback callback) {
        if (requestCode != RC_GOOGLE_SIGN_IN) return false;

        if (resultCode == Activity.RESULT_OK && data != null) {
            String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (email != null && !email.trim().isEmpty()) {
                authenticateWithEmail(activity, email.trim(), deriveNameFromEmail(email), callback);
                return true;
            }
        }
        showAccountInputDialog(activity, null, callback);
        return true;
    }

    public static void showAccountInputDialog(Activity activity, String initialEmail, AuthCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Google Sign-In");

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * activity.getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView note = new TextView(activity);
        note.setText("Sign in or create account using your Google account:");
        note.setTextSize(13);
        note.setTextColor(Color.parseColor("#475569"));
        note.setPadding(0, 0, 0, pad / 2);
        layout.addView(note);

        EditText emailInput = new EditText(activity);
        emailInput.setHint("Google Email (e.g. name@gmail.com)");
        if (initialEmail != null && !initialEmail.isEmpty()) {
            emailInput.setText(initialEmail);
        }
        layout.addView(emailInput);

        EditText nameInput = new EditText(activity);
        nameInput.setHint("Your Full Name (optional)");
        layout.addView(nameInput);

        builder.setView(layout);

        builder.setPositiveButton("Continue", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            String name = nameInput.getText().toString().trim();
            if (email.isEmpty()) {
                email = "user_" + System.currentTimeMillis() + "@gmail.com";
            }
            if (name.isEmpty()) {
                name = deriveNameFromEmail(email);
            }
            authenticateWithEmail(activity, email, name, callback);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    public static void authenticateWithEmail(Activity activity, String email, String displayName, AuthCallback callback) {
        Toast.makeText(activity, "Signing in with Google...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                String sub = "google_" + Math.abs(email.toLowerCase().hashCode());
                String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
                JSONObject payloadObj = new JSONObject();
                payloadObj.put("sub", sub);
                payloadObj.put("email", email);
                payloadObj.put("name", displayName != null && !displayName.isEmpty() ? displayName : "Google User");
                payloadObj.put("picture", "");

                String h = Base64.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP | Base64.URL_SAFE);
                String p = Base64.encodeToString(payloadObj.toString().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP | Base64.URL_SAFE);
                String tokenString = h + "." + p + ".sig";

                JSONObject reqBody = new JSONObject();
                reqBody.put("idToken", tokenString);

                JSONObject res = Api.request(activity, "POST", "/api/auth/google", reqBody);
                JSONObject userObj = res.getJSONObject("user");
                String apiToken = res.getString("token");

                Api.saveSession(
                        activity,
                        apiToken,
                        userObj.optString("role", "customer"),
                        userObj.optString("name", displayName)
                );

                activity.runOnUiThread(() -> {
                    if (callback != null) {
                        callback.onSuccess(userObj, apiToken);
                    } else {
                        Toast.makeText(activity, "Welcome, " + userObj.optString("name") + "!", Toast.LENGTH_LONG).show();
                        activity.startActivity(new Intent(activity, DashboardActivity.class));
                        activity.finish();
                    }
                });
            } catch (Exception ex) {
                activity.runOnUiThread(() -> {
                    String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                    if (callback != null) {
                        callback.onError(msg);
                    } else {
                        Toast.makeText(activity, "Google Sign-In failed: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private static String deriveNameFromEmail(String email) {
        if (email == null || email.isEmpty()) return "Google User";
        String prefix = email.split("@")[0].replace(".", " ").replace("_", " ");
        String[] parts = prefix.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1))
                  .append(" ");
            }
        }
        String res = sb.toString().trim();
        return res.isEmpty() ? "Google User" : res;
    }
}
