package com.shravantraders.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private EditText mobileInput, passwordInput;
    private Button loginButton, googleButton;
    private TextView langToggle, titleView, subTitleView, instructionView, forgotPassLink, registerLink, ownerSetupLink;
    private boolean isPasswordVisible = false;
    private String lang = "mr";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocaleAndTheme(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Api.wakeUpServer(this);

        lang = LocaleHelper.getLanguage(this);

        if (!Api.token(this).isEmpty()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        buildUi();
    }

    private void buildUi() {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(20), dp(24), dp(20));
        root.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));
        root.setGravity(Gravity.CENTER_VERTICAL);

        // Language Switcher Top Right
        langToggle = new TextView(this);
        langToggle.setText("मराठी  |  English");
        langToggle.setTextColor(Color.parseColor("#C94B00"));
        langToggle.setTextSize(14);
        langToggle.setTypeface(null, Typeface.BOLD);
        langToggle.setGravity(Gravity.END);
        langToggle.setPadding(dp(8), dp(4), dp(8), dp(4));
        langToggle.setOnClickListener(v -> {
            lang = lang.equals("mr") ? "en" : "mr";
            LocaleHelper.setLanguage(this, lang);
            buildUi();
        });
        root.addView(langToggle, new LinearLayout.LayoutParams(-1, -2));

        // Header Logo ImageView
        ImageView logoImg = new ImageView(this);
        logoImg.setImageResource(R.drawable.shravan_traders_logo);
        logoImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams lip = new LinearLayout.LayoutParams(dp(110), dp(110));
        lip.gravity = Gravity.CENTER;
        lip.bottomMargin = dp(4);
        logoImg.setLayoutParams(lip);
        root.addView(logoImg);

        titleView = new TextView(this);
        titleView.setText(lang.equals("mr") ? "श्रवण ट्रेडर्स" : "Shravan Traders");
        titleView.setTextSize(26);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView);

        subTitleView = new TextView(this);
        subTitleView.setText(lang.equals("mr") ? "पशुआहार व पेंड केंद्र • केंडल बु., राहुरी" : "Cattle Feed Shop • Kendal Bk., Rahuri");
        subTitleView.setTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        subTitleView.setTextSize(13);
        subTitleView.setGravity(Gravity.CENTER);
        subTitleView.setPadding(0, dp(2), 0, dp(14));
        root.addView(subTitleView);

        // Card Container
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackgroundResource(R.drawable.bg_card);

        TextView loginHeading = new TextView(this);
        loginHeading.setText(lang.equals("mr") ? "ग्राहक लॉगिन (Customer Login)" : "Customer Login");
        loginHeading.setTextSize(18);
        loginHeading.setTypeface(null, Typeface.BOLD);
        loginHeading.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        card.addView(loginHeading);

        instructionView = new TextView(this);
        instructionView.setText(lang.equals("mr") ? "पुन्हा स्वागत आहे! लॉगिन करा:" : "Welcome back! Login to continue:");
        instructionView.setTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        instructionView.setTextSize(13);
        instructionView.setPadding(0, dp(2), 0, dp(14));
        card.addView(instructionView);

        // Mobile Number
        addLabel(lang.equals("mr") ? "मोबाईल नंबर (Mobile Number)" : "Mobile Number", card);
        mobileInput = createEditText(lang.equals("mr") ? "उदा. +91 9876543210" : "+91 9876543210", card);
        mobileInput.setInputType(InputType.TYPE_CLASS_PHONE);

        // Password with Toggle Row
        addLabel(lang.equals("mr") ? "पासवर्ड (Password)" : "Password", card);

        LinearLayout passRow = new LinearLayout(this);
        passRow.setOrientation(LinearLayout.HORIZONTAL);
        passRow.setGravity(Gravity.CENTER_VERTICAL);
        passRow.setBackgroundResource(R.drawable.bg_card);

        passwordInput = new EditText(this);
        passwordInput.setSingleLine(true);
        passwordInput.setHint(lang.equals("mr") ? "तुमचा पासवर्ड टाका" : "Enter password");
        passwordInput.setTextSize(15);
        passwordInput.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        passwordInput.setHintTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        passwordInput.setBackground(null);
        passwordInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        TextView eyeToggle = new TextView(this);
        eyeToggle.setText("👁");
        eyeToggle.setTextSize(18);
        eyeToggle.setTextColor(isDark ? Color.parseColor("#CBD5E1") : Color.parseColor("#64748B"));
        eyeToggle.setPadding(dp(12), dp(12), dp(16), dp(12));
        eyeToggle.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            passwordInput.setSelection(passwordInput.getText().length());
        });

        passRow.addView(passwordInput, new LinearLayout.LayoutParams(0, -2, 1.0f));
        passRow.addView(eyeToggle);

        LinearLayout.LayoutParams prp = new LinearLayout.LayoutParams(-1, dp(48));
        prp.bottomMargin = dp(8);
        card.addView(passRow, prp);

        // Forgot Password Link
        forgotPassLink = new TextView(this);
        forgotPassLink.setText(lang.equals("mr") ? "पासवर्ड विसरलात? (Forgot Password?)" : "Forgot Password?");
        forgotPassLink.setTextColor(Color.parseColor("#2563EB"));
        forgotPassLink.setTextSize(13);
        forgotPassLink.setGravity(Gravity.END);
        forgotPassLink.setPadding(0, 0, 0, dp(12));
        forgotPassLink.setOnClickListener(v -> showForgotPasswordDialog());
        card.addView(forgotPassLink);

        // Login Button
        loginButton = new Button(this);
        loginButton.setText(lang.equals("mr") ? "लॉगिन करा (Login)" : "Login");
        loginButton.setAllCaps(false);
        loginButton.setTextColor(Color.WHITE);
        loginButton.setTextSize(16);
        loginButton.setTypeface(null, Typeface.BOLD);
        loginButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7"))); // Blue button matching mockup
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(50));
        card.addView(loginButton, bp);
        loginButton.setOnClickListener(v -> doLogin());

        // Register Link
        registerLink = new TextView(this);
        registerLink.setText(lang.equals("mr") ? "खाते नाही? येथे नोंदणी करा (Register)" : "Don't have an account? Register");
        registerLink.setTextColor(Color.parseColor("#0284C7"));
        registerLink.setTextSize(14);
        registerLink.setTypeface(null, Typeface.BOLD);
        registerLink.setGravity(Gravity.CENTER);
        registerLink.setPadding(0, dp(14), 0, dp(8));
        registerLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        card.addView(registerLink);

        // OR Divider
        TextView orDivider = new TextView(this);
        orDivider.setText("──────── OR ────────");
        orDivider.setTextColor(Color.parseColor("#94A3B8"));
        orDivider.setTextSize(12);
        orDivider.setGravity(Gravity.CENTER);
        orDivider.setPadding(0, dp(4), 0, dp(8));
        card.addView(orDivider);

        // Continue with Google Button
        googleButton = new Button(this);
        googleButton.setText("G  Continue with Google");
        googleButton.setAllCaps(false);
        googleButton.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        googleButton.setTextSize(14);
        googleButton.setTypeface(null, Typeface.BOLD);
        googleButton.setBackgroundTintList(ColorStateList.valueOf(isDark ? Color.parseColor("#1E293B") : Color.parseColor("#F1F5F9")));
        card.addView(googleButton, new LinearLayout.LayoutParams(-1, dp(46)));
        googleButton.setOnClickListener(v -> doGoogleSignIn());

        root.addView(card);

        // Owner Setup Link
        ownerSetupLink = new TextView(this);
        ownerSetupLink.setText(lang.equals("mr") ? "दुकान मालक? पहिला मालक सेटअप (Owner Setup)" : "Shop Owner? Initial Owner Setup");
        ownerSetupLink.setTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        ownerSetupLink.setTextSize(13);
        ownerSetupLink.setGravity(Gravity.CENTER);
        ownerSetupLink.setPadding(0, dp(14), 0, 0);
        ownerSetupLink.setOnClickListener(v -> showOwnerSetupDialog());
        root.addView(ownerSetupLink);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void addLabel(String text, LinearLayout container) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        label.setTextSize(13);
        label.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(4);
        container.addView(label, p);
    }

    private EditText createEditText(String hint, LinearLayout container) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setTextSize(15);
        e.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        e.setHintTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        e.setBackgroundResource(R.drawable.bg_card);
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48));
        p.bottomMargin = dp(12);
        container.addView(e, p);
        return e;
    }

    private void doLogin() {
        String m = mobileInput.getText().toString().trim().replace(" ", "").replace("+91", "");
        String p = passwordInput.getText().toString().trim();

        if (m.isEmpty() || p.isEmpty()) {
            toast(lang.equals("mr") ? "कृपया मोबाईल नंबर आणि पासवर्ड टाका." : "Please enter mobile number and password.");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText(lang.equals("mr") ? "लॉगिन होत आहे..." : "Logging in...");

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("mobile", m);
                body.put("password", p);

                JSONObject r = Api.request(this, "POST", "/api/auth/login", body);
                JSONObject u = r.getJSONObject("user");

                Api.saveSession(this, r.getString("token"), u.getString("role"), u.getString("name"));

                runOnUiThread(() -> {
                    try {
                        if (u.optBoolean("mustChangePassword", false)) {
                            toast(lang.equals("mr") ? "सुरक्षेसाठी तुम्हाला तुमचा पासवर्ड बदलणे आवश्यक आहे." : "For security, you must change your password.");
                            showChangePasswordDialogForce(p, r.getString("token"), u.getString("role"), u.getString("name"));
                        } else {
                            toast(lang.equals("mr") ? "जी आय " + u.optString("name") + ", स्वागत आहे!" : "Welcome, " + u.optString("name") + "!");
                            startActivity(new Intent(this, DashboardActivity.class));
                            finish();
                        }
                    } catch (Exception ex) {}
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText(lang.equals("mr") ? "लॉगिन करा (Login)" : "Login");
                    toast(e.getMessage() != null ? e.getMessage() : "Login failed.");
                });
            }
        }).start();
    }

    private void showChangePasswordDialogForce(String currPass, String token, String role, String name) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(12));

        TextView tv = new TextView(this);
        tv.setText("You are logging in for the first time. Please update your temporary password.");
        tv.setTextColor(Color.parseColor("#DC2626"));
        tv.setPadding(0, 0, 0, dp(12));
        box.addView(tv);

        EditText nw = new EditText(this);
        nw.setHint("New Password (min 6 chars)");
        nw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(nw);

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("Update Password Required")
                .setView(box)
                .setCancelable(false)
                .setPositiveButton("Update", null)
                .show();

        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nP = nw.getText().toString().trim();
            if (nP.length() < 6) {
                toast("Password must be at least 6 characters.");
                return;
            }

            new Thread(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("currentPassword", currPass);
                    body.put("newPassword", nP);

                    URL url = new URL(Api.getBaseUrl(this) + "/api/auth/change-password");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setDoOutput(true);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body.toString().getBytes("UTF-8"));
                    }

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        Api.saveSession(this, token, role, name);
                        runOnUiThread(() -> {
                            d.dismiss();
                            toast("Password updated successfully!");
                            startActivity(new Intent(this, DashboardActivity.class));
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> toast("Failed to update password. Code: " + code));
                    }
                    conn.disconnect();
                } catch (Exception ex) {
                    runOnUiThread(() -> toast("Error: " + ex.getMessage()));
                }
            }).start();
        });
    }

    private void showOwnerSetupDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(12));

        TextView info = new TextView(this);
        info.setText("Shop Owner Initial Setup:\nRequires Owner Setup Key from backend .env.");
        info.setTextSize(13);
        info.setTextColor(Color.parseColor("#64748B"));
        box.addView(info);

        EditText name = new EditText(this); name.setHint("Owner Name (Vinayak Bhau Tarde)");
        EditText mobile = new EditText(this); mobile.setHint("Owner Mobile Number"); mobile.setInputType(InputType.TYPE_CLASS_PHONE);
        EditText pass = new EditText(this); pass.setHint("Owner Password (min 8 chars)"); pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText key = new EditText(this); key.setHint("Owner Setup Key (from backend .env)"); key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        box.addView(name); box.addView(mobile); box.addView(pass); box.addView(key);

        new AlertDialog.Builder(this)
                .setTitle("Owner Setup / मालक खाते")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create Owner Account", (d, w) -> {
                    String n = name.getText().toString().trim();
                    String m = mobile.getText().toString().trim().replace(" ", "").replace("+91", "");
                    String p = pass.getText().toString().trim();
                    String k = key.getText().toString().trim();

                    if (n.isEmpty() || m.isEmpty() || p.isEmpty() || k.isEmpty()) {
                        toast("All fields including setup key are required.");
                        return;
                    }

                    new Thread(() -> {
                        try {
                            JSONObject body = new JSONObject();
                            body.put("name", n);
                            body.put("mobile", m);
                            body.put("password", p);

                            URL url = new URL(Api.getBaseUrl(this) + "/api/setup/owner");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Type", "application/json");
                            conn.setRequestProperty("x-owner-setup-key", k);
                            conn.setDoOutput(true);

                            try (OutputStream os = conn.getOutputStream()) {
                                os.write(body.toString().getBytes("UTF-8"));
                            }

                            int code = conn.getResponseCode();
                            InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
                            JSONObject res = new JSONObject(baos.toString("UTF-8"));
                            conn.disconnect();

                            if (code >= 200 && code < 300) {
                                JSONObject u = res.getJSONObject("user");
                                Api.saveSession(this, res.getString("token"), u.getString("role"), u.getString("name"));
                                runOnUiThread(() -> {
                                    toast("Owner account created successfully!");
                                    startActivity(new Intent(this, DashboardActivity.class));
                                    finish();
                                });
                            } else {
                                throw new Exception(res.optString("message", "Setup failed (" + code + ")"));
                            }
                        } catch (Exception ex) {
                            runOnUiThread(() -> toast("Error: " + ex.getMessage()));
                        }
                    }).start();
                }).show();
    }

    private void showForgotPasswordDialog() {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        TextView tv = new TextView(this);
        tv.setText(lang.equals("mr") ? "पासवर्ड रिसेट करण्यासाठी नोंदणीकृत मोबाईल किंवा ईमेल टाका:" : "Enter registered mobile number or email to receive password reset code:");
        tv.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        tv.setTextSize(13);
        tv.setPadding(0, 0, 0, dp(12));
        box.addView(tv);

        EditText idInput = new EditText(this);
        idInput.setHint(lang.equals("mr") ? "मोबाईल नंबर किंवा ईमेल" : "Registered Mobile or Email");
        idInput.setTextSize(15);
        idInput.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        idInput.setHintTextColor(Color.parseColor("#94A3B8"));
        idInput.setBackgroundResource(R.drawable.bg_card);
        idInput.setPadding(dp(14), dp(11), dp(14), dp(11));
        box.addView(idInput);

        AlertDialog dlg = new AlertDialog.Builder(this)
            .setTitle(lang.equals("mr") ? "पासवर्ड रिसेट (Forgot Password)" : "Forgot Password")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send Code", null)
            .show();

        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String identifier = idInput.getText().toString().trim();
            if (identifier.isEmpty()) {
                toast("Please enter your mobile number or email.");
                return;
            }

            dlg.dismiss();
            toast(lang.equals("mr") ? "कोड पाठवत आहे..." : "Requesting verification code...");

            new Thread(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("identifier", identifier);

                    JSONObject res = Api.request(this, "POST", "/api/auth/forgot-password", body);
                    String devCode = res.optString("devCode", "");

                    runOnUiThread(() -> {
                        showResetCodeVerificationDialog(identifier, devCode);
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> toast("Error: " + ex.getMessage()));
                }
            }).start();
        });
    }

    private void showResetCodeVerificationDialog(String identifier, String devCode) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        TextView tv = new TextView(this);
        String prompt = "Enter the 6-digit verification code sent to " + identifier + ":";
        if (devCode != null && !devCode.isEmpty()) {
            prompt += "\n\n(Dev Code: " + devCode + ")";
        }
        tv.setText(prompt);
        tv.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        tv.setTextSize(13);
        tv.setPadding(0, 0, 0, dp(12));
        box.addView(tv);

        EditText codeInput = new EditText(this);
        codeInput.setHint("6-digit verification code");
        if (devCode != null && !devCode.isEmpty()) codeInput.setText(devCode);
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        codeInput.setTextSize(15);
        codeInput.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        codeInput.setHintTextColor(Color.parseColor("#94A3B8"));
        codeInput.setBackgroundResource(R.drawable.bg_card);
        codeInput.setPadding(dp(14), dp(11), dp(14), dp(11));
        box.addView(codeInput);

        AlertDialog dlg = new AlertDialog.Builder(this)
            .setTitle("Enter Reset Code")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Verify Code", null)
            .show();

        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String code = codeInput.getText().toString().trim();
            if (code.length() < 4) {
                toast("Please enter the verification code.");
                return;
            }

            dlg.dismiss();

            new Thread(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("identifier", identifier);
                    body.put("code", code);

                    JSONObject res = Api.request(this, "POST", "/api/auth/verify-reset-code", body);
                    String resetToken = res.optString("resetToken", "");

                    runOnUiThread(() -> {
                        showSetNewPasswordDialog(identifier, code, resetToken);
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> toast("Error verifying code: " + ex.getMessage()));
                }
            }).start();
        });
    }

    private void showSetNewPasswordDialog(String identifier, String code, String resetToken) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        TextView tv = new TextView(this);
        tv.setText("Verification confirmed. Create a new secure password:");
        tv.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        tv.setTextSize(13);
        tv.setPadding(0, 0, 0, dp(12));
        box.addView(tv);

        EditText passInput = new EditText(this);
        passInput.setHint("New Password (min 6 characters)");
        passInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passInput.setTextSize(15);
        passInput.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        passInput.setHintTextColor(Color.parseColor("#94A3B8"));
        passInput.setBackgroundResource(R.drawable.bg_card);
        passInput.setPadding(dp(14), dp(11), dp(14), dp(11));
        box.addView(passInput);

        AlertDialog dlg = new AlertDialog.Builder(this)
            .setTitle("Set New Password")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save New Password", null)
            .show();

        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPass = passInput.getText().toString().trim();
            if (newPass.length() < 6) {
                toast("Password must be at least 6 characters.");
                return;
            }

            dlg.dismiss();
            toast("Updating password...");

            new Thread(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("identifier", identifier);
                    body.put("code", code);
                    body.put("resetToken", resetToken);
                    body.put("newPassword", newPass);

                    JSONObject res = Api.request(this, "POST", "/api/auth/reset-password", body);

                    runOnUiThread(() -> {
                        toast("Password reset successfully! Please log in with your new password.");
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> toast("Error resetting password: " + ex.getMessage()));
                }
            }).start();
        });
    }

    private void doGoogleSignIn() {
        GoogleAuthHelper.startSignIn(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (GoogleAuthHelper.handleActivityResult(this, requestCode, resultCode, data, null)) {
            return;
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + .5f);
    }
}
