package com.shravantraders.app;

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

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput, mobileInput, passwordInput, confirmPasswordInput;
    private CheckBox termsCheckBox;
    private Button registerButton;
    private TextView titleView, subTitleView, langToggle, loginLink;
    private String lang = "mr";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocaleAndTheme(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lang = LocaleHelper.getLanguage(this);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(20), dp(24), dp(20));
        root.setBackgroundColor(Color.parseColor("#FFF9F2"));
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
        titleView.setText(lang.equals("mr") ? "नवीन खाते तयार करा" : "Create Account");
        titleView.setTextSize(26);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(Color.parseColor("#1E293B"));
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView);

        subTitleView = new TextView(this);
        subTitleView.setText("Join Shravan Traders • Kendal Bk., Rahuri");
        subTitleView.setTextColor(Color.parseColor("#64748B"));
        subTitleView.setTextSize(13);
        subTitleView.setGravity(Gravity.CENTER);
        subTitleView.setPadding(0, dp(2), 0, dp(14));
        root.addView(subTitleView);

        // Card Container
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackgroundResource(R.drawable.bg_card);

        addLabel(lang.equals("mr") ? "पूर्ण नाव (Full Name)" : "Full Name", card);
        nameInput = createEditText(lang.equals("mr") ? "उदा. वैभिनव तार्ड" : "e.g. Vaishnav Tarde", card);

        addLabel(lang.equals("mr") ? "मोबाईल नंबर (Mobile Number)" : "Mobile Number", card);
        mobileInput = createEditText(lang.equals("mr") ? "उदा. +91 9876543210" : "+91 9876543210", card);
        mobileInput.setInputType(InputType.TYPE_CLASS_PHONE);

        addLabel(lang.equals("mr") ? "पासवर्ड (Password)" : "Password", card);
        passwordInput = createEditText(lang.equals("mr") ? "पासवर्ड तयार करा" : "Create password", card);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        addLabel(lang.equals("mr") ? "पासवर्डची पुष्टी करा (Confirm Password)" : "Confirm Password", card);
        confirmPasswordInput = createEditText(lang.equals("mr") ? "पासवर्ड पुन्हा प्रविष्ट करा" : "Re-enter password", card);
        confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Terms CheckBox
        termsCheckBox = new CheckBox(this);
        termsCheckBox.setText(lang.equals("mr") ? "मी अटी व शर्तींशी सहमत आहे (Terms & Conditions)" : "I agree to Terms & Conditions");
        termsCheckBox.setTextSize(13);
        termsCheckBox.setTextColor(Color.parseColor("#334155"));
        termsCheckBox.setChecked(true);
        termsCheckBox.setPadding(dp(4), 0, 0, dp(12));
        card.addView(termsCheckBox);

        registerButton = new Button(this);
        registerButton.setText(lang.equals("mr") ? "नोंदणी करा (Register)" : "Register");
        registerButton.setAllCaps(false);
        registerButton.setTextColor(Color.WHITE);
        registerButton.setTextSize(16);
        registerButton.setTypeface(null, Typeface.BOLD);
        registerButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7"))); // Blue button matching mockup
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(50));
        card.addView(registerButton, bp);
        registerButton.setOnClickListener(v -> doRegister());

        // OR Divider
        TextView orDivider = new TextView(this);
        orDivider.setText(lang.equals("mr") ? "──────── किंवा ────────" : "──────── OR ────────");
        orDivider.setTextColor(Color.parseColor("#94A3B8"));
        orDivider.setTextSize(12);
        orDivider.setGravity(Gravity.CENTER);
        orDivider.setPadding(0, dp(10), 0, dp(10));
        card.addView(orDivider);

        // Continue with Google Button
        Button googleButton = new Button(this);
        googleButton.setText("G  " + (lang.equals("mr") ? "Google द्वारे नोंदणी करा" : "Continue with Google"));
        googleButton.setAllCaps(false);
        googleButton.setTextColor(Color.parseColor("#1E293B"));
        googleButton.setTextSize(14);
        googleButton.setTypeface(null, Typeface.BOLD);
        googleButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
        card.addView(googleButton, new LinearLayout.LayoutParams(-1, dp(46)));
        googleButton.setOnClickListener(v -> GoogleAuthHelper.startSignIn(this));

        root.addView(card);

        // Login Link
        loginLink = new TextView(this);
        loginLink.setText(lang.equals("mr") ? "आधीच खाते आहे? लॉगिन करा" : "Already have an account? Login");
        loginLink.setTextColor(Color.parseColor("#0284C7"));
        loginLink.setTextSize(14);
        loginLink.setTypeface(null, Typeface.BOLD);
        loginLink.setGravity(Gravity.CENTER);
        loginLink.setPadding(0, dp(16), 0, dp(10));
        loginLink.setOnClickListener(v -> finish());
        root.addView(loginLink);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void addLabel(String text, LinearLayout container) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(Color.parseColor("#1E293B"));
        label.setTextSize(13);
        label.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(4);
        container.addView(label, p);
    }

    private EditText createEditText(String hint, LinearLayout container) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setTextSize(15);
        e.setTextColor(Color.parseColor("#1E293B"));
        e.setHintTextColor(Color.parseColor("#94A3B8"));
        e.setBackgroundResource(R.drawable.bg_card);
        e.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(46));
        p.bottomMargin = dp(12);
        container.addView(e, p);
        return e;
    }

    private void doRegister() {
        String n = nameInput.getText().toString().trim();
        String m = mobileInput.getText().toString().trim().replace(" ", "").replace("+91", "");
        String p = passwordInput.getText().toString().trim();
        String cp = confirmPasswordInput.getText().toString().trim();

        if (n.isEmpty() || m.isEmpty() || p.isEmpty() || cp.isEmpty()) {
            toast(lang.equals("mr") ? "कृपया सर्व माहिती भरा." : "Please fill in all required fields.");
            return;
        }
        if (p.length() < 6) {
            toast(lang.equals("mr") ? "पासवर्ड कमीत कमी ६ अक्षरांचा असावा." : "Password must be at least 6 characters long.");
            return;
        }
        if (!p.equals(cp)) {
            toast(lang.equals("mr") ? "पासवर्ड जुळत नाहीत." : "Passwords do not match.");
            return;
        }
        if (!termsCheckBox.isChecked()) {
            toast(lang.equals("mr") ? "कृपया अटी व शर्ती स्वीकारा." : "Please agree to Terms & Conditions.");
            return;
        }

        registerButton.setEnabled(false);
        registerButton.setText(lang.equals("mr") ? "नोंदणी होत आहे..." : "Registering...");

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("name", n);
                body.put("mobile", m);
                body.put("password", p);
                body.put("confirmPassword", cp);

                JSONObject r = Api.request(this, "POST", "/api/auth/register", body);
                JSONObject u = r.getJSONObject("user");

                Api.saveSession(this, r.getString("token"), u.getString("role"), u.getString("name"));

                runOnUiThread(() -> {
                    toast(lang.equals("mr") ? "नोंदणी यशस्वी झाली!" : "Registration successful!");
                    Intent intent = new Intent(this, DashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    registerButton.setEnabled(true);
                    registerButton.setText(lang.equals("mr") ? "नोंदणी करा (Register)" : "Register");
                    toast(e.getMessage() != null ? e.getMessage() : "Registration failed.");
                });
            }
        }).start();
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
