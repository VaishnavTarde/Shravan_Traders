package com.shravantraders.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Calendar;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.ColorDrawable;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private LinearLayout rootLayout, mainContentContainer, navBarContainer;
    private TextView statusTextView, shopTitleView, shopSubtitleView, roleBadgeView;
    private String userRole = "customer";
    private String currentLang = "mr";
    private String activeTab = "dashboard";
    private JSONArray lastLoadedProducts = null;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocaleAndTheme(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userRole = Api.role(this);
        currentLang = LocaleHelper.getLanguage(this);

        if (Api.token(this).isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        buildBaseLayout();

        if ("owner".equalsIgnoreCase(userRole)) {
            loadOwnerDashboard();
        } else {
            loadCustomerHome();
        }
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + .5f);
    }

    private void applySalesDropdownStyle(AutoCompleteTextView dropdown) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        dropdown.setTextColor(isDark ? Color.WHITE : Color.BLACK);
        dropdown.setHintTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        dropdown.setBackgroundResource(R.drawable.bg_card);
        dropdown.setDropDownBackgroundDrawable(new ColorDrawable(isDark ? Color.parseColor("#1E293B") : Color.WHITE));
        dropdown.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
        dropdown.setHighlightColor(Color.TRANSPARENT);
        dropdown.setCursorVisible(false);
        dropdown.setFocusable(true);
        dropdown.setFocusableInTouchMode(true);
        dropdown.setInputType(InputType.TYPE_CLASS_TEXT);
        dropdown.setOnClickListener(v -> dropdown.showDropDown());
        dropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) dropdown.showDropDown();
        });
    }

    private void applySearchableDropdownStyle(AutoCompleteTextView dropdown) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        dropdown.setTextColor(isDark ? Color.WHITE : Color.BLACK);
        dropdown.setHintTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        dropdown.setBackgroundResource(R.drawable.bg_card);
        dropdown.setDropDownBackgroundDrawable(new ColorDrawable(isDark ? Color.parseColor("#1E293B") : Color.WHITE));
        dropdown.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
        dropdown.setHighlightColor(Color.TRANSPARENT);
        dropdown.setFocusable(true);
        dropdown.setFocusableInTouchMode(true);
        dropdown.setInputType(InputType.TYPE_CLASS_TEXT);
        dropdown.setOnClickListener(v -> dropdown.showDropDown());
        dropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) dropdown.showDropDown();
        });
    }

    public static class CustomDropdownAdapter<T> extends ArrayAdapter<T> {
        private final List<T> items;

        public CustomDropdownAdapter(Context context, List<T> objects) {
            super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(objects));
            this.items = new ArrayList<>(objects);
        }

        public CustomDropdownAdapter(Context context, T[] objects) {
            super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(Arrays.asList(objects)));
            this.items = new ArrayList<>(Arrays.asList(objects));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                boolean isDark = LocaleHelper.isDarkMode(getContext());
                tv.setTextColor(isDark ? Color.WHITE : Color.BLACK);
                tv.setBackgroundColor(isDark ? Color.parseColor("#1E293B") : Color.WHITE);
                tv.setTextSize(15);
                int p = (int) (12 * getContext().getResources().getDisplayMetrics().density);
                tv.setPadding(p, p, p, p);
            }
            return view;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = items;
                    results.count = items.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }
            };
        }
    }

    public static class SearchableDropdownAdapter<T> extends ArrayAdapter<T> {
        public SearchableDropdownAdapter(Context context, List<T> objects) {
            super(context, android.R.layout.simple_dropdown_item_1line, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                boolean isDark = LocaleHelper.isDarkMode(getContext());
                tv.setTextColor(isDark ? Color.WHITE : Color.BLACK);
                tv.setBackgroundColor(isDark ? Color.parseColor("#1E293B") : Color.WHITE);
                tv.setTextSize(15);
                int p = (int) (12 * getContext().getResources().getDisplayMetrics().density);
                tv.setPadding(p, p, p, p);
            }
            return view;
        }
    }

    private JSONArray sortJsonArrayByDouble(JSONArray arr, String key, boolean descending) {
        List<JSONObject> list = new ArrayList<>();
        try {
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj != null) list.add(obj);
                }
                list.sort((a, b) -> {
                    double va = a.optDouble(key, 0);
                    double vb = b.optDouble(key, 0);
                    return descending ? Double.compare(vb, va) : Double.compare(va, vb);
                });
            }
        } catch (Exception ignored) {}
        return new JSONArray(list);
    }

    private TextView createTextView(String text, int sizeSp, boolean isBold, int colorHex) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sizeSp);
        if (LocaleHelper.isDarkMode(this)) {
            if (colorHex == Color.parseColor("#1E293B")) {
                tv.setTextColor(Color.parseColor("#F8FAFC"));
            } else if (colorHex == Color.parseColor("#64748B")) {
                tv.setTextColor(Color.parseColor("#94A3B8"));
            } else {
                tv.setTextColor(colorHex);
            }
        } else {
            tv.setTextColor(colorHex);
        }
        if (isBold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private Button createStyledButton(String text, int bgColor, Runnable onClick) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        b.setOnClickListener(v -> onClick.run());
        return b;
    }

    private EditText createFormInput(String hint, String initialValue, int inputType) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        if (initialValue != null && !initialValue.isEmpty()) {
            e.setText(initialValue);
        }
        e.setTextSize(15);
        e.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        e.setHintTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        e.setBackgroundResource(R.drawable.bg_card);
        e.setPadding(dp(14), dp(11), dp(14), dp(11));
        if (inputType != 0) {
            e.setInputType(inputType);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
        lp.bottomMargin = dp(10);
        e.setLayoutParams(lp);
        return e;
    }

    private void buildBaseLayout() {
        boolean isDark = LocaleHelper.isDarkMode(this);
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        // Top App Bar
        LinearLayout appBar = new LinearLayout(this);
        appBar.setOrientation(LinearLayout.HORIZONTAL);
        appBar.setPadding(dp(16), dp(16), dp(16), dp(12));
        appBar.setBackgroundColor(Color.parseColor("#E85D04"));
        appBar.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        shopTitleView = new TextView(this);
        shopTitleView.setText(currentLang.equals("mr") ? "श्रवण ट्रेडर्स" : "Shravan Traders");
        shopTitleView.setTextSize(20);
        shopTitleView.setTypeface(null, Typeface.BOLD);
        shopTitleView.setTextColor(Color.WHITE);

        shopSubtitleView = new TextView(this);
        shopSubtitleView.setText(currentLang.equals("mr") ? "केंडल बु. • राहुरी" : "Kendal Bk. • Rahuri");
        shopSubtitleView.setTextSize(12);
        shopSubtitleView.setTextColor(Color.parseColor("#FFEDD5"));

        titleBox.addView(shopTitleView);
        titleBox.addView(shopSubtitleView);

        appBar.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1.0f));

        // Role Badge & Menu
        roleBadgeView = new TextView(this);
        roleBadgeView.setText(userRole.equalsIgnoreCase("owner") ? (currentLang.equals("mr") ? "मालक (OWNER)" : "OWNER") : (currentLang.equals("mr") ? "ग्राहक (CUSTOMER)" : "CUSTOMER"));
        roleBadgeView.setTextSize(12);
        roleBadgeView.setTypeface(null, Typeface.BOLD);
        roleBadgeView.setTextColor(Color.parseColor("#E85D04"));
        roleBadgeView.setBackgroundResource(R.drawable.bg_card);
        roleBadgeView.setPadding(dp(10), dp(5), dp(10), dp(5));
        LinearLayout.LayoutParams rbp = new LinearLayout.LayoutParams(-2, -2);
        rbp.rightMargin = dp(8);
        roleBadgeView.setLayoutParams(rbp);

        TextView langBtn = new TextView(this);
        langBtn.setText(currentLang.equals("mr") ? "EN" : "मराठी");
        langBtn.setTextColor(Color.WHITE);
        langBtn.setTextSize(13);
        langBtn.setTypeface(null, Typeface.BOLD);
        langBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        langBtn.setOnClickListener(v -> {
            String newLang = currentLang.equals("mr") ? "en" : "mr";
            LocaleHelper.setLanguage(this, newLang);
            recreate();
        });

        TextView menuBtn = new TextView(this);
        menuBtn.setText("⚙");
        menuBtn.setTextSize(22);
        menuBtn.setTextColor(Color.WHITE);
        menuBtn.setPadding(dp(8), dp(0), dp(4), dp(0));
        menuBtn.setOnClickListener(v -> loadSettingsView());

        ImageView logoutTopBtn = new ImageView(this);
        logoutTopBtn.setImageResource(R.drawable.ic_logout);
        logoutTopBtn.setColorFilter(Color.WHITE);
        logoutTopBtn.setContentDescription(currentLang.equals("mr") ? "लॉगआउट करा" : "Logout");

        GradientDrawable logoutBg = new GradientDrawable();
        logoutBg.setShape(GradientDrawable.OVAL);
        logoutBg.setColor(Color.parseColor("#B91C1C"));
        logoutTopBtn.setBackground(logoutBg);
        logoutTopBtn.setPadding(dp(7), dp(7), dp(7), dp(7));

        LinearLayout.LayoutParams logoutParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        logoutParams.leftMargin = dp(6);
        logoutTopBtn.setLayoutParams(logoutParams);
        logoutTopBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle(currentLang.equals("mr") ? "लॉगआउट करा" : "Logout")
                .setMessage(currentLang.equals("mr") ? "तुम्हाला खात्री आहे की तुम्ही साइन आउट करू इच्छिता?" : "Are you sure you want to sign out?")
                .setPositiveButton(currentLang.equals("mr") ? "लॉगआउट (Logout)" : "Logout", (d, w) -> {
                    Api.clearSession(this);
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(currentLang.equals("mr") ? "रद्द करा" : "Cancel", null)
                .show();
        });

        appBar.addView(roleBadgeView);
        appBar.addView(langBtn);
        appBar.addView(menuBtn);
        appBar.addView(logoutTopBtn);

        rootLayout.addView(appBar);

        // Status Header Text
        statusTextView = createTextView("", 13, false, Color.parseColor("#64748B"));
        statusTextView.setPadding(dp(16), dp(6), dp(16), dp(2));
        rootLayout.addView(statusTextView);

        // Scrollable Content
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        mainContentContainer = new LinearLayout(this);
        mainContentContainer.setOrientation(LinearLayout.VERTICAL);
        mainContentContainer.setPadding(dp(16), dp(8), dp(16), dp(16));

        scroll.addView(mainContentContainer);
        rootLayout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        // Bottom Navigation Bar
        navBarContainer = new LinearLayout(this);
        navBarContainer.setOrientation(LinearLayout.HORIZONTAL);
        navBarContainer.setBackgroundColor(isDark ? Color.parseColor("#1E293B") : Color.WHITE);
        navBarContainer.setPadding(dp(6), dp(8), dp(6), dp(24));
        navBarContainer.setElevation(dp(12));

        // Apply System Window Insets for BOTH Top App Bar and Bottom Nav Bar
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBar.setPadding(dp(16), dp(14) + systemBars.top, dp(16), dp(12));
            int bottomPadding = dp(20) + systemBars.bottom;
            navBarContainer.setPadding(dp(6), dp(8), dp(6), bottomPadding);
            return insets;
        });

        buildBottomNav();

        rootLayout.addView(navBarContainer);
        setContentView(rootLayout);
    }

    private void buildBottomNav() {
        navBarContainer.removeAllViews();

        if ("owner".equalsIgnoreCase(userRole)) {
            addNavTab("dash", currentLang.equals("mr") ? "🏠\nडॅशबोर्ड" : "🏠\nDashboard", this::loadOwnerDashboard);
            addNavTab("products", currentLang.equals("mr") ? "📦\nउत्पादने" : "📦\nProducts", this::loadProductManagement);
            addNavTab("billing", currentLang.equals("mr") ? "🛒\nविक्री बिल" : "🛒\nSales", this::loadPOSBilling);
            addNavTab("customers", currentLang.equals("mr") ? "👥\nग्राहक बाकी" : "👥\nCustomers", this::loadCustomersAndCredit);
            addNavTab("reports", currentLang.equals("mr") ? "📊\nरिपोर्ट्स" : "📊\nReports", this::loadReportsView);
        } else {
            addNavTab("home", currentLang.equals("mr") ? "🏠\nहोम" : "🏠\nHome", this::loadCustomerHome);
            addNavTab("catalogue", currentLang.equals("mr") ? "📦\nउत्पादने" : "📦\nProducts", this::loadProductManagement);
            addNavTab("my_bills", currentLang.equals("mr") ? "🧾\nमाझे बिल" : "🧾\nMy Bills", this::loadCustomerBills);
            addNavTab("profile", currentLang.equals("mr") ? "👤\nप्रोफाईल" : "👤\nProfile", this::loadCustomerProfile);
        }
    }

    private void addNavTab(String id, String label, Runnable onClick) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(10);
        b.setAllCaps(false);
        b.setPadding(dp(2), dp(2), dp(2), dp(2));

        boolean isDark = LocaleHelper.isDarkMode(this);
        if (id.equals(activeTab)) {
            b.setTextColor(Color.parseColor("#E85D04"));
            b.setTypeface(null, Typeface.BOLD);
            b.setBackgroundTintList(ColorStateList.valueOf(isDark ? Color.parseColor("#3B2312") : Color.parseColor("#FFEDD5")));
        } else {
            b.setTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
            b.setTypeface(null, Typeface.NORMAL);
            b.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        }

        b.setOnClickListener(v -> {
            activeTab = id;
            buildBottomNav();
            onClick.run();
        });

        navBarContainer.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1.0f));
    }

    // ==========================================
    // CUSTOMER HOME & CATALOGUE
    // ==========================================

    private void loadCustomerHome() {
        activeTab = "home";
        buildBottomNav();
        statusTextView.setText("Shravan Traders • Kendal Bk. • Rahuri");

        mainContentContainer.removeAllViews();

        // 📢 Notice Board Banner (Top of customer home page)
        LinearLayout customerNoticeContainer = new LinearLayout(this);
        customerNoticeContainer.setOrientation(LinearLayout.VERTICAL);
        customerNoticeContainer.setVisibility(View.GONE);
        mainContentContainer.addView(customerNoticeContainer);
        fetchAndRenderNoticeBoard(customerNoticeContainer, false);

        TextView welcome = createTextView("नमस्कार, " + Api.userName(this) + " 👋", 22, true, Color.parseColor("#1E293B"));
        TextView subWelcome = createTextView("Welcome to Shravan Traders", 13, false, Color.parseColor("#64748B"));
        mainContentContainer.addView(welcome);
        mainContentContainer.addView(subWelcome);

        // Customer Account Metrics Summary Cards
        LinearLayout metricsCard = new LinearLayout(this);
        metricsCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
        mlp.topMargin = dp(8);
        mlp.bottomMargin = dp(12);
        mainContentContainer.addView(metricsCard, mlp);

        new Thread(() -> {
            try {
                JSONObject res = Api.request(this, "GET", "/api/me/statement", null);
                JSONObject summary = res.optJSONObject("summary");
                JSONObject cust = res.optJSONObject("customer");
                String myId = cust != null ? cust.optString("id") : "";
                String myName = cust != null ? cust.optString("name") : Api.userName(this);

                double billed = summary != null ? summary.optDouble("totalBilled", 0) : 0;
                double paid = summary != null ? summary.optDouble("totalPaid", 0) : 0;
                double bal = summary != null ? summary.optDouble("creditBalance", 0) : 0;

                runOnUiThread(() -> {
                    metricsCard.removeAllViews();
                    GridLayout grid = new GridLayout(this);
                    grid.setColumnCount(3);

                    grid.addView(buildKpiCard(currentLang.equals("mr") ? "एकूण खरेदी\nPurchases" : "Total Purchases", "₹ " + String.format(Locale.US, "%.0f", billed), Color.parseColor("#1E293B"), this::loadCustomerBills), new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(0, 1.0f)));
                    grid.addView(buildKpiCard(currentLang.equals("mr") ? "जमा रक्कम\nTotal Paid" : "Total Paid", "₹ " + String.format(Locale.US, "%.0f", paid), Color.parseColor("#16A34A"), () -> showCustomerStatementDialog(myId, myName)), new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(1, 1.0f)));
                    grid.addView(buildKpiCard(currentLang.equals("mr") ? "उधारी बाकी\nUdhaar Due" : "Udhaar Due", "₹ " + String.format(Locale.US, "%.0f", bal), bal > 0 ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A"), () -> showCustomerStatementDialog(myId, myName)), new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(2, 1.0f)));

                    metricsCard.addView(grid);
                });
            } catch (Exception e) {
                if (e instanceof Api.SessionExpiredException || (e.getMessage() != null && (e.getMessage().contains("Session expired") || e.getMessage().contains("Please log in")))) {
                    onSessionExpired(e.getMessage());
                }
            }
        }).start();

        // Customer Quick Access Buttons
        LinearLayout custActions = new LinearLayout(this);
        custActions.setOrientation(LinearLayout.HORIZONTAL);
        custActions.setPadding(0, 0, 0, dp(12));

        Button btnCat = createStyledButton("📦 Catalogue", Color.parseColor("#C94B00"), this::loadProductManagement);
        Button btnBills = createStyledButton("🧾 My Bills", Color.parseColor("#0284C7"), this::loadCustomerBills);
        Button btnContact = createStyledButton("📞 Contact Shop", Color.parseColor("#16A34A"), this::showContactShopDialog);

        custActions.addView(btnCat, new LinearLayout.LayoutParams(0, dp(40), 1.0f));
        View s1 = new View(this); custActions.addView(s1, new LinearLayout.LayoutParams(dp(4), 1));
        custActions.addView(btnBills, new LinearLayout.LayoutParams(0, dp(40), 1.0f));
        View s2 = new View(this); custActions.addView(s2, new LinearLayout.LayoutParams(dp(4), 1));
        custActions.addView(btnContact, new LinearLayout.LayoutParams(0, dp(40), 1.0f));

        mainContentContainer.addView(custActions);

        EditText searchBar = createFormInput("🔍 Search products...", "", InputType.TYPE_CLASS_TEXT);
        mainContentContainer.addView(searchBar);

        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(dp(18), dp(16), dp(18), dp(16));
        banner.setBackgroundResource(R.drawable.bg_card_primary);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.bottomMargin = dp(16);

        banner.addView(createTextView("उत्तम दर्जाचे पशुआहार 🐄 🐃", 20, true, Color.WHITE));
        banner.addView(createTextView("आता खरेदी करा • योग्य दर व सर्वोत्तम गुणवत्ता", 13, false, Color.parseColor("#FFEDD5")));

        mainContentContainer.addView(banner, blp);

        mainContentContainer.addView(createTextView("Categories / प्रकार", 16, true, Color.parseColor("#1E293B")));

        LinearLayout catRow = new LinearLayout(this);
        catRow.setOrientation(LinearLayout.HORIZONTAL);
        catRow.setPadding(0, dp(8), 0, dp(14));

        String[] categories = {"कांडी", "वालीस", "पेंड", "मक्याचे पीठ"};
        for (String cat : categories) {
            TextView cTv = createTextView("🌾 " + cat, 13, true, Color.parseColor("#1E293B"));
            cTv.setPadding(dp(12), dp(8), dp(12), dp(8));
            cTv.setBackgroundResource(R.drawable.bg_card);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.rightMargin = dp(8);
            catRow.addView(cTv, clp);
            cTv.setOnClickListener(v -> loadProductManagement());
        }

        HorizontalScrollView catScroll = new HorizontalScrollView(this);
        catScroll.addView(catRow);
        mainContentContainer.addView(catScroll);

        LinearLayout popHeader = new LinearLayout(this);
        popHeader.setOrientation(LinearLayout.HORIZONTAL);
        popHeader.setGravity(Gravity.CENTER_VERTICAL);
        popHeader.addView(createTextView("Popular Products", 16, true, Color.parseColor("#1E293B")), new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView viewAll = createTextView("View All >", 13, true, Color.parseColor("#0284C7"));
        viewAll.setOnClickListener(v -> loadProductManagement());
        popHeader.addView(viewAll);

        mainContentContainer.addView(popHeader);

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lcp = new LinearLayout.LayoutParams(-1, -2);
        lcp.topMargin = dp(8);
        mainContentContainer.addView(listContainer, lcp);

        fetchAndRenderProducts(searchBar, listContainer);
    }

    private void showContactShopDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(Color.parseColor("#FFF9F2"));

        box.addView(createTextView("Shravan Traders (श्रवण ट्रेडर्स)", 18, true, Color.parseColor("#E85D04")));
        box.addView(createTextView("Proprietor: Vinayak Bhau Tarde", 14, true, Color.parseColor("#1E293B")));
        box.addView(createTextView("Location: Kendal BK., Taluka - Rahuri, Dist - Ahilyanagar", 13, false, Color.parseColor("#64748B")));
        box.addView(createTextView("Contact: +91 8390529481", 14, true, Color.parseColor("#16A34A")));

        View div = new View(this); div.setBackgroundColor(Color.parseColor("#E2E8F0"));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, dp(1));
        dlp.setMargins(0, dp(12), 0, dp(12));
        box.addView(div, dlp);

        Button callBtn = createStyledButton("📞 Call Shop (+91 8390529481)", Color.parseColor("#16A34A"), () -> {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:+918390529481"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open dialer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        box.addView(callBtn, new LinearLayout.LayoutParams(-1, dp(42)));

        Button waBtn = createStyledButton("💬 WhatsApp Shop", Color.parseColor("#25D366"), () -> {
            try {
                String text = "Namaste Vinayak Bhau, I am " + Api.userName(this) + ". I have a query regarding products / my bill at Shravan Traders.";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://wa.me/918390529481?text=" + Uri.encode(text)));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open WhatsApp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(-1, dp(42));
        wlp.topMargin = dp(8);
        box.addView(waBtn, wlp);

        new AlertDialog.Builder(this)
            .setTitle("Contact Shop / दुकानाशी संपर्क")
            .setView(box)
            .setPositiveButton("Close", null)
            .show();
    }

    private boolean isSessionExpiredShowing = false;

    private void onSessionExpired(String msg) {
        runOnUiThread(() -> {
            if (isSessionExpiredShowing || isFinishing() || isDestroyed()) return;
            isSessionExpiredShowing = true;
            Api.clearSession(this);

            new AlertDialog.Builder(this)
                .setTitle(currentLang.equals("mr") ? "सत्र कालबाह्य झाले (Session Expired)" : "Session Expired")
                .setMessage(currentLang.equals("mr") 
                    ? "तुमचे लॉगिन सत्र कालबाह्य झाले आहे. कृपया पुन्हा लॉगिन करा." 
                    : "Your login session has expired. Please log in again to continue.")
                .setCancelable(false)
                .setPositiveButton(currentLang.equals("mr") ? "लॉगिन करा (Log In)" : "Log In Again", (d, w) -> {
                    isSessionExpiredShowing = false;
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
        });
    }

    private void loadCustomerProfile() {
        activeTab = "profile";
        buildBottomNav();
        statusTextView.setText("My Profile");

        mainContentContainer.removeAllViews();
        mainContentContainer.addView(createTextView("Loading profile...", 14, false, Color.parseColor("#64748B")));

        new Thread(() -> {
            try {
                JSONObject res = Api.request(this, "GET", "/api/me/statement", null);
                JSONObject summary = res.optJSONObject("summary");
                JSONObject cust = res.optJSONObject("customer");

                runOnUiThread(() -> renderCustomerProfileUi(cust, summary));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (e instanceof Api.SessionExpiredException || (e.getMessage() != null && (e.getMessage().contains("Session expired") || e.getMessage().contains("Please log in")))) {
                        onSessionExpired(e.getMessage());
                        return;
                    }

                    mainContentContainer.removeAllViews();
                    mainContentContainer.addView(createTextView("Error loading profile: " + e.getMessage(), 14, false, Color.parseColor("#DC2626")));

                    Button retry = createStyledButton(currentLang.equals("mr") ? "पुन्हा प्रयत्न करा (Retry)" : "Retry", Color.parseColor("#0284C7"), this::loadCustomerProfile);
                    mainContentContainer.addView(retry, new LinearLayout.LayoutParams(-1, dp(46)));

                    View space = new View(this);
                    mainContentContainer.addView(space, new LinearLayout.LayoutParams(1, dp(12)));

                    Button btnSignout = createStyledButton(currentLang.equals("mr") ? "लॉगआउट करा / पुन्हा लॉगिन करा" : "Sign Out / Log In Again", Color.parseColor("#DC2626"), () -> {
                        Api.clearSession(this);
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                    mainContentContainer.addView(btnSignout, new LinearLayout.LayoutParams(-1, dp(46)));
                });
            }
        }).start();
    }

    private void renderCustomerProfileUi(JSONObject cust, JSONObject summary) {
        mainContentContainer.removeAllViews();

        if (cust == null) return;
        String name = cust.optString("name", Api.userName(this));
        String mobile = cust.optString("mobile", "");
        String address = cust.optString("address", "Not provided");
        String createdAt = cust.optString("createdAt", "");
        if (createdAt.length() >= 10) createdAt = createdAt.substring(0, 10);
        String profImg = cust.optString("profileImageUrl", "");

        double totalBilled = summary != null ? summary.optDouble("totalBilled", 0) : 0;
        double totalPaid = summary != null ? summary.optDouble("totalPaid", 0) : 0;
        double bal = summary != null ? summary.optDouble("creditBalance", 0) : 0;
        int billsCount = summary != null ? summary.optInt("billsCount", 0) : 0;

        // B. Profile Identity Section Card
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout profHeader = new LinearLayout(this);
        profHeader.setOrientation(LinearLayout.VERTICAL);
        profHeader.setGravity(Gravity.CENTER);
        profHeader.setPadding(dp(16), dp(20), dp(16), dp(20));
        profHeader.setBackgroundResource(R.drawable.bg_card);

        FrameLayout avatarFrame = new FrameLayout(this);
        ImageView avatar = new ImageView(this);
        avatar.setLayoutParams(new FrameLayout.LayoutParams(dp(80), dp(80)));
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (!profImg.isEmpty()) {
            Glide.with(this).load(Api.resolveImageUrl(this, profImg)).circleCrop().into(avatar);
        } else {
            avatar.setImageResource(R.drawable.shravan_traders_logo);
            avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        avatarFrame.addView(avatar);

        TextView editAvatarBtn = createTextView("✏️", 12, false, Color.WHITE);
        editAvatarBtn.setBackgroundResource(R.drawable.bg_card_primary);
        editAvatarBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        FrameLayout.LayoutParams eap = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        editAvatarBtn.setLayoutParams(eap);
        editAvatarBtn.setOnClickListener(v -> showEditProfileDialog(cust));
        avatarFrame.addView(editAvatarBtn);

        profHeader.addView(avatarFrame);

        TextView nameTv = createTextView(name, 20, true, isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        nameTv.setPadding(0, dp(10), 0, dp(2));
        profHeader.addView(nameTv);
        
        profHeader.addView(createTextView("Mobile: " + mobile + "  •  Shravan Traders", 13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
        
        TextView activeStatus = createTextView("✓ Verified Customer", 11, true, Color.parseColor("#16A34A"));
        activeStatus.setBackgroundResource(R.drawable.bg_badge_in_stock);
        activeStatus.setPadding(dp(12), dp(4), dp(12), dp(4));
        LinearLayout.LayoutParams asp = new LinearLayout.LayoutParams(-2, -2);
        asp.topMargin = dp(8);
        profHeader.addView(activeStatus, asp);

        mainContentContainer.addView(profHeader);

        // C. Account Summary Cards
        GridLayout sumGrid = new GridLayout(this);
        sumGrid.setColumnCount(3);
        sumGrid.setUseDefaultMargins(true);

        LinearLayout c1 = buildProfileKpiCard("🧾\nTotal Bills", String.valueOf(billsCount), Color.parseColor("#1E293B"), Color.parseColor("#F1F5F9"));
        LinearLayout c2 = buildProfileKpiCard("💰\nPurchased", "₹" + String.format(Locale.US, "%.0f", totalBilled), Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"));
        LinearLayout c3 = buildProfileKpiCard("💳\nOutstanding", "₹" + String.format(Locale.US, "%.0f", bal), bal > 0 ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A"), Color.parseColor("#FEE2E2"));

        sumGrid.addView(c1, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(0, 1.0f)));
        sumGrid.addView(c2, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(1, 1.0f)));
        sumGrid.addView(c3, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(2, 1.0f)));
        
        mainContentContainer.addView(sumGrid, new LinearLayout.LayoutParams(-1, -2));

        // D. Personal Information Card
        LinearLayout infoCard = new LinearLayout(this);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        infoCard.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams icp = new LinearLayout.LayoutParams(-1, -2);
        icp.topMargin = dp(16);

        LinearLayout infoHeader = new LinearLayout(this);
        infoHeader.setOrientation(LinearLayout.HORIZONTAL);
        infoHeader.addView(createTextView("Personal Information", 16, true, Color.parseColor("#1E293B")), new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView editBtn = createTextView("Edit", 14, true, Color.parseColor("#E85D04"));
        editBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        editBtn.setOnClickListener(v -> showEditProfileDialog(cust));
        infoHeader.addView(editBtn);
        infoCard.addView(infoHeader);
        
        View iDiv = new View(this); iDiv.setBackgroundColor(Color.parseColor("#E2E8F0"));
        infoCard.addView(iDiv, new LinearLayout.LayoutParams(-1, dp(1)));
        ((LinearLayout.LayoutParams)iDiv.getLayoutParams()).setMargins(0, dp(10), 0, dp(10));

        infoCard.addView(buildInfoRow("👤", "Full Name", name));
        infoCard.addView(buildInfoRow("📱", "Mobile Number", mobile));
        infoCard.addView(buildInfoRow("📍", "Address", address));
        infoCard.addView(buildInfoRow("📅", "Customer Since", createdAt));

        mainContentContainer.addView(infoCard, icp);

        // E. More Options Section
        mainContentContainer.addView(createTextView("More Options", 16, true, Color.parseColor("#1E293B")), new LinearLayout.LayoutParams(-1, -2) {{ topMargin = dp(24); bottomMargin = dp(8); }});

        LinearLayout optsCard = new LinearLayout(this);
        optsCard.setOrientation(LinearLayout.VERTICAL);
        optsCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        optsCard.setBackgroundResource(R.drawable.bg_card);

        optsCard.addView(buildOptionRow("📄", "Digital Passbook PDF", "Download complete account statement & ledger passbook.", () -> {
            Toast.makeText(this, "Generating Digital Passbook PDF...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                try {
                    JSONObject statementRes = Api.request(this, "GET", "/api/me/statement", null);
                    File pdf = PdfGenerator.generateCustomerPassbookPdf(this, statementRes, name);
                    runOnUiThread(() -> promptSharePdf(pdf));
                } catch (Exception ex) {
                    runOnUiThread(() -> Toast.makeText(this, "Error generating passbook: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        }));
        optsCard.addView(buildOptionRow("🧾", "My Orders & Bills", "View, download and share your invoices.", this::loadCustomerBills));
        optsCard.addView(buildOptionRow("💰", "My Payments", "See all your payment records.", () -> showCustomerStatementDialog(cust.optString("id"), name)));
        optsCard.addView(buildOptionRow("💳", "My Udhaar Balance", "Check outstanding amount and invoice details.", () -> showCustomerStatementDialog(cust.optString("id"), name)));
        optsCard.addView(buildOptionRow("🔑", "Change Password", "Update account password securely.", this::showChangePasswordDialog));
        optsCard.addView(buildOptionRow("📦", "Product Catalogue", "View available cattle-feed products.", this::loadProductManagement));
        optsCard.addView(buildOptionRow("📞", "Contact Shop", "Call or WhatsApp Shravan Traders.", this::showContactShopDialog));
        optsCard.addView(buildOptionRow("⚙️", "Settings & Privacy", "Language and account security.", this::loadSettingsView));
        optsCard.addView(buildOptionRow("🚪", "Logout", "Sign out securely.", () -> {
            new AlertDialog.Builder(this).setTitle("Logout").setMessage("Are you sure you want to sign out?").setPositiveButton("Logout", (d,w) -> {
                Api.clearSession(this);
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }).setNegativeButton("Cancel", null).show();
        }));

        mainContentContainer.addView(optsCard, new LinearLayout.LayoutParams(-1, -2));
    }

    private LinearLayout buildProfileKpiCard(String title, String val, int valColor, int bgColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(4), dp(16), dp(4), dp(16));
        card.setGravity(Gravity.CENTER);
        
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dp(12));
        card.setBackground(gd);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        card.setLayoutParams(lp);

        TextView tvVal = createTextView(val, 16, true, valColor);
        TextView tvTitle = createTextView(title, 11, true, Color.parseColor("#475569"));
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, dp(6), 0, 0);

        card.addView(tvVal);
        card.addView(tvTitle);
        return card;
    }

    private LinearLayout buildInfoRow(String icon, String label, String value) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        TextView tvIcon = createTextView(icon, 20, false, isDark ? Color.WHITE : Color.BLACK);
        tvIcon.setPadding(0, 0, dp(16), 0);
        row.addView(tvIcon);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(createTextView(label, 12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
        col.addView(createTextView(value, 15, true, isDark ? Color.parseColor("#F8FAFC") : Color.parseColor("#1E293B")));
        row.addView(col, new LinearLayout.LayoutParams(0, -2, 1.0f));

        return row;
    }

    private LinearLayout buildOptionRow(String icon, String title, String subtitle, Runnable onClick) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));
        row.setOnClickListener(v -> onClick.run());

        if ("🚪".equals(icon)) {
            ImageView ivIcon = new ImageView(this);
            ivIcon.setImageResource(R.drawable.ic_logout);
            ivIcon.setColorFilter(Color.parseColor("#EF4444"));
            GradientDrawable redCircle = new GradientDrawable();
            redCircle.setShape(GradientDrawable.OVAL);
            redCircle.setColor(isDark ? Color.parseColor("#450A0A") : Color.parseColor("#FEE2E2"));
            ivIcon.setBackground(redCircle);
            ivIcon.setPadding(dp(6), dp(6), dp(6), dp(6));
            LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(dp(32), dp(32));
            ivParams.rightMargin = dp(14);
            row.addView(ivIcon, ivParams);
        } else {
            TextView tvIcon = createTextView(icon, 20, false, isDark ? Color.WHITE : Color.BLACK);
            tvIcon.setPadding(0, 0, dp(14), 0);
            row.addView(tvIcon);
        }

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(createTextView(title, 14, true, isDark ? Color.parseColor("#F8FAFC") : Color.parseColor("#1E293B")));
        col.addView(createTextView(subtitle, 12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
        row.addView(col, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView chevron = createTextView("›", 20, true, isDark ? Color.parseColor("#64748B") : Color.parseColor("#94A3B8"));
        row.addView(chevron);

        container.addView(row);

        View div = new View(this);
        div.setBackgroundColor(isDark ? Color.parseColor("#334155") : Color.parseColor("#F1F5F9"));
        container.addView(div, new LinearLayout.LayoutParams(-1, dp(1)));

        return container;
    }

    private void showEditProfileDialog(JSONObject cust) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));

        String profImg = cust.optString("profileImageUrl", "");
        selectedImageUrl = profImg;
        
        ImageView previewImg = new ImageView(this);
        previewImg.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(140)));
        previewImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (!profImg.isEmpty()) {
            previewImg.setVisibility(View.VISIBLE);
            Glide.with(this).load(Api.resolveImageUrl(this, profImg)).into(previewImg);
        } else {
            previewImg.setVisibility(View.GONE);
        }
        box.addView(previewImg);

        Button browseBtn = createStyledButton("📷 Change Photo / Browse Gallery", Color.parseColor("#0284C7"), () -> {
            activeDialogImagePreview = previewImg;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST);
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(42));
        blp.topMargin = dp(6);
        blp.bottomMargin = dp(10);
        box.addView(browseBtn, blp);

        EditText nameInput = createFormInput("Full Name", cust.optString("name"), InputType.TYPE_CLASS_TEXT);
        EditText addressInput = createFormInput("Address", cust.optString("address", ""), InputType.TYPE_CLASS_TEXT);

        box.addView(createTextView("Full Name:", 12, false, Color.parseColor("#64748B")));
        box.addView(nameInput);
        box.addView(createTextView("Address:", 12, false, Color.parseColor("#64748B")));
        box.addView(addressInput);

        new AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save Changes", (dlg, w) -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("name", nameInput.getText().toString().trim());
                    body.put("address", addressInput.getText().toString().trim());
                    body.put("profileImageUrl", selectedImageUrl);

                    new Thread(() -> {
                        try {
                            Api.request(this, "PATCH", "/api/me/profile", body);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                Api.saveSession(this, Api.token(this), userRole, nameInput.getText().toString().trim());
                                loadCustomerProfile();
                            });
                        } catch (Exception ex) {
                            runOnUiThread(() -> Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }).start();
                } catch (Exception ex) {
                    Toast.makeText(this, "Error processing inputs.", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    // ==========================================
    // OWNER DASHBOARD & MANAGEMENT
    // ==========================================

    private void loadOwnerDashboard() {
        activeTab = "dash";
        buildBottomNav();
        statusTextView.setText(currentLang.equals("mr") ? "मुख्य डॅशबोर्ड • पूर्ण प्रवेश" : "Owner Dashboard • Full Administrative Access");

        mainContentContainer.removeAllViews();

        mainContentContainer.addView(createTextView(currentLang.equals("mr") ? "Owner Dashboard" : "Owner Dashboard", 22, true, Color.parseColor("#1E293B")));
        mainContentContainer.addView(createTextView("Welcome back, Vinayak Bhau!", 13, false, Color.parseColor("#64748B")));

        // 📢 Notice Board Banner for Owner
        LinearLayout ownerNoticeContainer = new LinearLayout(this);
        ownerNoticeContainer.setOrientation(LinearLayout.VERTICAL);
        mainContentContainer.addView(ownerNoticeContainer);
        fetchAndRenderNoticeBoard(ownerNoticeContainer, true);

        // Upper Date Filter Dropdown
        String[] dashFiltersEn = {"Current Month", "Last Month", "Today / Daily", "All Time"};
        String[] dashFiltersMr = {"या महिन्यात (Current Month)", "मागील महिन्यात (Last Month)", "आज (Today)", "सर्व वेळ (All Time)"};
        final String[] dashFilters = currentLang.equals("mr") ? dashFiltersMr : dashFiltersEn;

        boolean isDark = LocaleHelper.isDarkMode(this);
        TextView dashFilterSpinner = new TextView(this);
        dashFilterSpinner.setText(dashFilters[0] + " ▾");
        dashFilterSpinner.setTextSize(15);
        dashFilterSpinner.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        dashFilterSpinner.setBackgroundResource(R.drawable.bg_card);
        dashFilterSpinner.setPadding(dp(14), dp(11), dp(14), dp(11));
        dashFilterSpinner.setGravity(Gravity.CENTER_VERTICAL);
        dashFilterSpinner.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);

        LinearLayout.LayoutParams dfLp = new LinearLayout.LayoutParams(-1, dp(46));
        dfLp.topMargin = dp(8);
        mainContentContainer.addView(dashFilterSpinner, dfLp);

        LinearLayout metricsContainer = new LinearLayout(this);
        metricsContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
        mlp.topMargin = dp(10);
        mainContentContainer.addView(metricsContainer, mlp);

        LinearLayout actionsCard = new LinearLayout(this);
        actionsCard.setOrientation(LinearLayout.VERTICAL);
        actionsCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        actionsCard.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams acp = new LinearLayout.LayoutParams(-1, -2);
        acp.topMargin = dp(14);
        mainContentContainer.addView(actionsCard, acp);

        actionsCard.addView(createTextView("Quick Actions", 15, true, Color.parseColor("#1E293B")));

        GridLayout gridAct = new GridLayout(this);
        gridAct.setColumnCount(2);
        gridAct.setPadding(0, dp(8), 0, 0);

        Button btnPOS = createStyledButton("🛒 New Sale", Color.parseColor("#E85D04"), this::loadPOSBilling);
        Button btnAddProd = createStyledButton("➕ Product", Color.parseColor("#C94B00"), this::showAddProductDialog);
        Button btnPurch = createStyledButton("🚚 Purchase", Color.parseColor("#2F855A"), this::showAddPurchaseDialog);
        Button btnPay = createStyledButton("💰 Payments", Color.parseColor("#0284C7"), this::loadPaymentsManagement);

        gridAct.addView(btnPOS, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(0, 1.0f)));
        gridAct.addView(btnAddProd, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(1, 1.0f)));
        gridAct.addView(btnPurch, new GridLayout.LayoutParams(GridLayout.spec(1), GridLayout.spec(0, 1.0f)));
        gridAct.addView(btnPay, new GridLayout.LayoutParams(GridLayout.spec(1), GridLayout.spec(1, 1.0f)));

        actionsCard.addView(gridAct);

        // Dynamic Sections Container for Purchases & Total Products
        LinearLayout dynamicSectionsContainer = new LinearLayout(this);
        dynamicSectionsContainer.setOrientation(LinearLayout.VERTICAL);
        mainContentContainer.addView(dynamicSectionsContainer);

        Runnable fetchDashboard = () -> {
            metricsContainer.removeAllViews();
            metricsContainer.addView(createTextView("Loading metrics...", 13, false, Color.parseColor("#64748B")));

            Object posTag = dashFilterSpinner.getTag();
            int pos = posTag instanceof Integer ? (Integer) posTag : 0;
            String periodCode = "this_month";
            if (pos == 1) periodCode = "last_month";
            else if (pos == 2) periodCode = "today";
            else if (pos == 3) periodCode = "all_time";

            final String finalPeriodCode = periodCode;

            new Thread(() -> {
                try {
                    JSONObject r = Api.request(this, "GET", "/api/reports/summary?period=" + finalPeriodCode, null);
                    JSONObject purRes = Api.request(this, "GET", "/api/purchases", null);
                    JSONObject prodRes = Api.request(this, "GET", "/api/products", null);

                    JSONObject totals = r.getJSONObject("totals");
                    JSONArray purchases = purRes != null ? purRes.optJSONArray("purchases") : null;
                    JSONArray products = prodRes != null ? prodRes.optJSONArray("products") : null;
                    JSONArray lowStock = r.optJSONArray("lowStockProducts");
                    JSONArray topSelling = r.optJSONArray("topSelling");

                    double revenue = totals.optDouble("salesRevenue", 0);
                    double purTotal = totals.optDouble("purchaseTotal", 0);
                    double grossProfit = totals.optDouble("grossProfit", 0);
                    double outstanding = totals.optDouble("totalOutstandingCredit", 0);
                    int stockItems = totals.optInt("productsCount", 0);
                    int totalUnits = totals.optInt("totalStockUnits", 0);
                    String stockValDisplay = totalUnits > 0 ? (totalUnits + " Units (" + stockItems + " Items)") : (stockItems + " Items");

                    runOnUiThread(() -> {
                        metricsContainer.removeAllViews();
                        dynamicSectionsContainer.removeAllViews();

                        GridLayout grid = new GridLayout(this);
                        grid.setColumnCount(2);

                        grid.addView(buildKpiCard(currentLang.equals("mr") ? "एकूण विक्री\nTotal Sales" : "Total Sales", "₹ " + String.format(Locale.US, "%.0f", revenue), Color.parseColor("#16A34A"), this::loadReportsView), new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(0, 1.0f)));
                        grid.addView(buildKpiCard(currentLang.equals("mr") ? "एकूण खरेदी\nPurchases" : "Purchases", "₹ " + String.format(Locale.US, "%.0f", purTotal), Color.parseColor("#9333EA"), null), new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(1, 1.0f)));
                        grid.addView(buildKpiCard(currentLang.equals("mr") ? "स्थूल नफा\nGross Profit" : "Gross Profit", "₹ " + String.format(Locale.US, "%.0f", grossProfit), Color.parseColor("#16A34A"), this::loadReportsView), new GridLayout.LayoutParams(GridLayout.spec(1), GridLayout.spec(0, 1.0f)));
                        grid.addView(buildKpiCard(currentLang.equals("mr") ? "उधारी बाकी\nOutstanding" : "Outstanding", "₹ " + String.format(Locale.US, "%.0f", outstanding), Color.parseColor("#EA580C"), this::fetchAndShowUnpaidCustomers), new GridLayout.LayoutParams(GridLayout.spec(1), GridLayout.spec(1, 1.0f)));
                        grid.addView(buildKpiCard(currentLang.equals("mr") ? "साठा उत्पादने\nStock Items" : "Stock Items", stockValDisplay, Color.parseColor("#0284C7"), this::loadProductManagement), new GridLayout.LayoutParams(GridLayout.spec(2), GridLayout.spec(0, 1.0f)));

                        metricsContainer.addView(grid);

                        // 1. New Purchase Section
                        LinearLayout purchCard = new LinearLayout(this);
                        purchCard.setOrientation(LinearLayout.VERTICAL);
                        purchCard.setPadding(dp(14), dp(14), dp(14), dp(14));
                        purchCard.setBackgroundResource(R.drawable.bg_card);
                        LinearLayout.LayoutParams pcp = new LinearLayout.LayoutParams(-1, -2);
                        pcp.topMargin = dp(14);
                        purchCard.addView(createTextView("📦 Recent Purchases & Supplier Details", 16, true, Color.parseColor("#1E293B")));

                        if (purchases != null && purchases.length() > 0) {
                            for (int i = 0; i < Math.min(purchases.length(), 5); i++) {
                                JSONObject p = purchases.optJSONObject(i);
                                if (p == null) continue;

                                LinearLayout itemBox = new LinearLayout(this);
                                itemBox.setOrientation(LinearLayout.VERTICAL);
                                itemBox.setPadding(dp(8), dp(8), dp(8), dp(8));
                                itemBox.setBackgroundColor(Color.parseColor("#F8FAFC"));
                                LinearLayout.LayoutParams ibp = new LinearLayout.LayoutParams(-1, -2);
                                ibp.topMargin = dp(8);

                                itemBox.addView(createTextView("Supplier: " + p.optString("supplierName") + " (PO: " + p.optString("purchaseNo") + ")", 13, true, Color.parseColor("#1E293B")));

                                JSONArray items = p.optJSONArray("items");
                                if (items != null) {
                                    for (int j = 0; j < items.length(); j++) {
                                        JSONObject it = items.optJSONObject(j);
                                        if (it == null) continue;
                                        String prodName = it.optString("nameEn", it.optString("nameMr", "Product"));
                                        double qty = it.optDouble("quantity", 0);
                                        double cost = it.optDouble("unitPrice", 0);
                                        double lineTot = it.optDouble("lineTotal", qty * cost);
                                        itemBox.addView(createTextView(" • " + prodName + " | Qty: " + qty + " " + it.optString("unit") + " @ ₹" + String.format(Locale.US, "%.0f", cost) + " = ₹" + String.format(Locale.US, "%.0f", lineTot), 12, false, Color.parseColor("#64748B")));
                                    }
                                }

                                double totAmt = p.optDouble("totalAmount", 0);
                                double paidAmt = p.optDouble("paidAmount", totAmt);
                                double remAmt = Math.max(0, totAmt - paidAmt);
                                itemBox.addView(createTextView("Total Cost: ₹" + String.format(Locale.US, "%.0f", totAmt) + " | Paid: ₹" + String.format(Locale.US, "%.0f", paidAmt), 13, true, Color.parseColor("#16A34A")));
                                itemBox.addView(createTextView("Remaining Supplier Due: ₹" + String.format(Locale.US, "%.0f", remAmt), 13, true, remAmt > 0 ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A")));

                                LinearLayout pBtnRow = new LinearLayout(this);
                                pBtnRow.setOrientation(LinearLayout.HORIZONTAL);

                                Button pEdit = new Button(this);
                                pEdit.setText("Edit / बदल");
                                pEdit.setAllCaps(false);
                                pEdit.setTextColor(Color.WHITE);
                                pEdit.setTextSize(13);
                                pEdit.setTypeface(null, Typeface.BOLD);
                                pEdit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7")));
                                pEdit.setOnClickListener(v -> showEditPurchaseDialog(p));

                                Button pDel = new Button(this);
                                pDel.setText("Delete / हटवा");
                                pDel.setAllCaps(false);
                                pDel.setTextColor(Color.WHITE);
                                pDel.setTextSize(13);
                                pDel.setTypeface(null, Typeface.BOLD);
                                pDel.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DC2626")));
                                pDel.setOnClickListener(v -> deletePurchase(p.optString("_id")));

                                LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(38), 1.0f);
                                lp1.rightMargin = dp(4);
                                pBtnRow.addView(pEdit, lp1);

                                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(38), 1.0f);
                                lp2.leftMargin = dp(4);
                                pBtnRow.addView(pDel, lp2);

                                LinearLayout.LayoutParams pbp = new LinearLayout.LayoutParams(-1, -2);
                                pbp.topMargin = dp(10);
                                itemBox.addView(pBtnRow, pbp);

                                purchCard.addView(itemBox, ibp);
                            }
                        } else {
                            purchCard.addView(createTextView("No purchase records found.", 13, false, Color.parseColor("#64748B")));
                        }
                        dynamicSectionsContainer.addView(purchCard, pcp);

                        // 2. Shop Products / Total Products Section
                        LinearLayout prodCard = new LinearLayout(this);
                        prodCard.setOrientation(LinearLayout.VERTICAL);
                        prodCard.setPadding(dp(14), dp(14), dp(14), dp(14));
                        prodCard.setBackgroundResource(R.drawable.bg_card);
                        LinearLayout.LayoutParams prcp = new LinearLayout.LayoutParams(-1, -2);
                        prcp.topMargin = dp(14);
                        prodCard.addView(createTextView("📦 Shop Products & Inventory Stock", 16, true, Color.parseColor("#1E293B")));

                        if (products != null && products.length() > 0) {
                            for (int i = 0; i < products.length(); i++) {
                                JSONObject pr = products.optJSONObject(i);
                                if (pr == null) continue;

                                LinearLayout prRow = new LinearLayout(this);
                                prRow.setOrientation(LinearLayout.HORIZONTAL);
                                prRow.setPadding(0, dp(6), 0, dp(6));

                                String pName = pr.optString("nameMr") + " (" + pr.optString("nameEn") + ")";
                                double currentStock = pr.optDouble("stock", 0);

                                prRow.addView(createTextView(pName, 13, true, Color.parseColor("#1E293B")), new LinearLayout.LayoutParams(0, -2, 1.0f));
                                prRow.addView(createTextView("Stock: " + (int)currentStock + " " + pr.optString("unit"), 13, true, Color.parseColor("#16A34A")));

                                prodCard.addView(prRow);
                                View pDiv = new View(this); pDiv.setBackgroundColor(Color.parseColor("#F1F5F9"));
                                prodCard.addView(pDiv, new LinearLayout.LayoutParams(-1, dp(1)));
                            }
                        } else {
                            prodCard.addView(createTextView("No products found.", 13, false, Color.parseColor("#64748B")));
                        }
                        dynamicSectionsContainer.addView(prodCard, prcp);

                        // 3. Low Stock & Top Selling Section (Slot Filtered)
                        LinearLayout alertCard = new LinearLayout(this);
                        alertCard.setOrientation(LinearLayout.VERTICAL);
                        alertCard.setPadding(dp(14), dp(14), dp(14), dp(14));
                        alertCard.setBackgroundResource(R.drawable.bg_card);
                        LinearLayout.LayoutParams acp2 = new LinearLayout.LayoutParams(-1, -2);
                        acp2.topMargin = dp(14);
                        alertCard.addView(createTextView("⚠️ Low Stock & 🔥 Top-Selling Items (" + dashFilters[pos < 0 ? 0 : pos] + ")", 16, true, Color.parseColor("#1E293B")));

                        alertCard.addView(createTextView("Low Stock Alerts:", 13, true, Color.parseColor("#DC2626")));
                        if (lowStock != null && lowStock.length() > 0) {
                            for (int i = 0; i < lowStock.length(); i++) {
                                JSONObject ls = lowStock.optJSONObject(i);
                                if (ls != null) {
                                    alertCard.addView(createTextView(" • " + ls.optString("productMr") + " (" + ls.optString("product") + ") - Only " + (int)ls.optDouble("stock") + " " + ls.optString("unit") + " left!", 12, false, Color.parseColor("#DC2626")));
                                }
                            }
                        } else {
                            alertCard.addView(createTextView("All items are adequately stocked.", 12, false, Color.parseColor("#16A34A")));
                        }

                        View sDiv = new View(this); sDiv.setBackgroundColor(Color.parseColor("#E2E8F0"));
                        alertCard.addView(sDiv, new LinearLayout.LayoutParams(-1, dp(1)));
                        ((LinearLayout.LayoutParams)sDiv.getLayoutParams()).setMargins(0, dp(10), 0, dp(10));

                        alertCard.addView(createTextView("Top-Selling Products in Selected Period:", 13, true, Color.parseColor("#0284C7")));
                        if (topSelling != null && topSelling.length() > 0) {
                            for (int i = 0; i < topSelling.length(); i++) {
                                JSONObject ts = topSelling.optJSONObject(i);
                                if (ts != null && ts.optDouble("quantitySold", 0) > 0) {
                                    double qSold = ts.optDouble("quantitySold", 0);
                                    double revGen = ts.optDouble("revenueGenerated", 0);
                                    alertCard.addView(createTextView(" " + (i+1) + ". " + ts.optString("productMr") + " — Qty: " + qSold + " " + ts.optString("unit") + " | Rev: ₹" + String.format(Locale.US, "%.0f", revGen), 12, false, Color.parseColor("#1E293B")));
                                }
                            }
                        } else {
                            alertCard.addView(createTextView("No sales recorded for this slot.", 12, false, Color.parseColor("#64748B")));
                        }

                        dynamicSectionsContainer.addView(alertCard, acp2);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        metricsContainer.removeAllViews();
                        metricsContainer.addView(createTextView("Error loading summary: " + e.getMessage(), 13, false, Color.parseColor("#DC2626")));
                    });
                }
            }).start();
        };

        dashFilterSpinner.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle(currentLang.equals("mr") ? "कालावधी निवडा" : "Select Period")
                .setItems(dashFilters, (dlg, which) -> {
                    dashFilterSpinner.setText(dashFilters[which] + " ▾");
                    dashFilterSpinner.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));
                    dashFilterSpinner.setTag(which);
                    fetchDashboard.run();
                })
                .show();
        });

        // Initial fetch for default "Current Month"
        fetchDashboard.run();
    }

    private View buildKpiCard(String title, String val, int valColor, Runnable onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        card.setLayoutParams(lp);

        card.addView(createTextView(title, 11, false, Color.parseColor("#64748B")));
        card.addView(createTextView(val, 18, true, valColor));

        if (onClick != null) {
            card.setOnClickListener(v -> onClick.run());
        }
        return card;
    }

    private void fetchAndShowUnpaidCustomers() {
        new Thread(() -> {
            try {
                JSONObject r = Api.request(this, "GET", "/api/reports/summary", null);
                double totalOutstanding = r.getJSONObject("totals").optDouble("totalOutstandingCredit", 0);
                runOnUiThread(() -> showUnpaidCustomersDialog(totalOutstanding));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void loadProductManagement() {
        activeTab = "products";
        buildBottomNav();
        statusTextView.setText(currentLang.equals("mr") ? "उत्पादन व्यवस्थापन व साठा" : "Product & Inventory Management");

        mainContentContainer.removeAllViews();

        LinearLayout headerBox = new LinearLayout(this);
        headerBox.setOrientation(LinearLayout.HORIZONTAL);
        headerBox.setGravity(Gravity.CENTER_VERTICAL);

        headerBox.addView(createTextView("Products / उत्पादने", 22, true, Color.parseColor("#1E293B")), new LinearLayout.LayoutParams(0, -2, 1.0f));

        Button galleryBtn = createStyledButton("🖼 Gallery", Color.parseColor("#0284C7"), () -> {
            if (lastLoadedProducts != null) {
                showAllProductsImageGallery(lastLoadedProducts);
            } else {
                Toast.makeText(this, "Products still loading...", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams gbp = new LinearLayout.LayoutParams(-2, dp(36));
        gbp.rightMargin = dp(6);
        headerBox.addView(galleryBtn, gbp);

        if ("owner".equalsIgnoreCase(userRole)) {
            Button addBtn = createStyledButton("➕ Add", Color.parseColor("#16A34A"), this::showAddProductDialog);
            headerBox.addView(addBtn, new LinearLayout.LayoutParams(-2, dp(36)));
        }

        mainContentContainer.addView(headerBox);

        EditText searchBar = createFormInput("🔍 Search products...", "", InputType.TYPE_CLASS_TEXT);
        mainContentContainer.addView(searchBar);

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        mainContentContainer.addView(listContainer);

        fetchAndRenderProducts(searchBar, listContainer);
    }

    private void fetchAndRenderProducts(EditText searchBar, LinearLayout listContainer) {
        new Thread(() -> {
            try {
                JSONObject r = Api.request(this, "GET", "/api/products", null);
                JSONArray products = r.getJSONArray("products");
                lastLoadedProducts = products;

                runOnUiThread(() -> {
                    renderProductList(products, searchBar.getText().toString().trim(), listContainer);

                    searchBar.addTextChangedListener(new TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                            renderProductList(products, s.toString().trim(), listContainer);
                        }
                        @Override public void afterTextChanged(Editable s) {}
                    });
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    listContainer.removeAllViews();
                    listContainer.addView(createTextView("Failed to load products: " + e.getMessage(), 14, false, Color.parseColor("#DC2626")));
                });
            }
        }).start();
    }

    private void showAllProductsImageGallery(JSONArray products) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.setBackgroundColor(Color.parseColor("#FFF9F2"));

        ScrollView scroll = new ScrollView(this);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(true);
        scroll.addView(grid);
        box.addView(scroll, new LinearLayout.LayoutParams(-1, dp(420)));

        try {
            for (int i = 0; i < products.length(); i++) {
                JSONObject p = products.optJSONObject(i);
                if (p == null) continue;
                String imageUrl = p.optString("imageUrl", "");
                String nameMr = p.optString("nameMr", "");
                String nameEn = p.optString("nameEn", "");
                double price = p.optDouble("sellingPrice", 0);
                String unit = p.optString("unit", "bag");

                LinearLayout itemCard = new LinearLayout(this);
                itemCard.setOrientation(LinearLayout.VERTICAL);
                itemCard.setPadding(dp(8), dp(8), dp(8), dp(8));
                itemCard.setBackgroundResource(R.drawable.bg_card);

                ImageView img = new ImageView(this);
                img.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(130)));
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setBackgroundColor(Color.parseColor("#F1F5F9"));
                String resImg = Api.resolveImageUrl(this, imageUrl);
                if (!resImg.isEmpty()) {
                    Glide.with(this).load(resImg).placeholder(R.drawable.bg_card).error(android.R.drawable.ic_menu_gallery).into(img);
                } else {
                    img.setImageResource(android.R.drawable.ic_menu_gallery);
                }
                itemCard.addView(img);

                TextView tv = createTextView(nameMr + "\n₹ " + String.format(Locale.US, "%.0f", price) + " / " + unit, 12, true, Color.parseColor("#1E293B"));
                tv.setPadding(0, dp(4), 0, 0);
                itemCard.addView(tv);

                itemCard.setOnClickListener(v -> showProductDetailsDialog(p));

                GridLayout.LayoutParams glp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                );
                glp.width = 0;
                glp.height = -2;
                glp.setMargins(dp(4), dp(4), dp(4), dp(4));
                grid.addView(itemCard, glp);
            }
        } catch (Exception ignored) {}

        new AlertDialog.Builder(this)
            .setTitle(currentLang.equals("mr") ? "सर्व उत्पादनांची चित्र गॅलरी" : "All Products Image Gallery")
            .setView(box)
            .setPositiveButton("Close", null)
            .show();
    }

    private void renderProductList(JSONArray products, String query, LinearLayout listContainer) {
        listContainer.removeAllViews();
        
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(false);

        int count = 0;
        try {
            for (int i = 0; i < products.length(); i++) {
                JSONObject p = products.getJSONObject(i);
                String nameMr = p.optString("nameMr", "");
                String nameEn = p.optString("nameEn", "");
                String cat = p.optString("category", "कांडी");
                String imageUrl = p.optString("imageUrl", "");
                double sellingPrice = p.optDouble("sellingPrice", 0);
                String unit = p.optString("unit", "bag");

                if (!query.isEmpty() && !nameMr.toLowerCase().contains(query.toLowerCase()) && !nameEn.toLowerCase().contains(query.toLowerCase())) {
                    continue;
                }

                count++;
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(10), dp(10), dp(10), dp(10));
                card.setBackgroundResource(R.drawable.bg_card);

                // Product Image
                ImageView imgView = new ImageView(this);
                LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(-1, dp(120));
                imgLp.bottomMargin = dp(8);
                imgView.setLayoutParams(imgLp);
                imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imgView.setBackgroundColor(Color.parseColor("#F1F5F9"));
                String resImg = Api.resolveImageUrl(this, imageUrl);
                if (!resImg.isEmpty()) {
                    Glide.with(this).load(resImg).placeholder(R.drawable.bg_card).error(android.R.drawable.ic_menu_gallery).into(imgView);
                } else {
                    imgView.setImageResource(android.R.drawable.ic_menu_gallery);
                }
                card.addView(imgView);

                // Category Tag
                TextView catTag = createTextView("🏷 " + cat, 10, true, Color.parseColor("#0284C7"));
                card.addView(catTag);

                // Product Name
                TextView title = createTextView(nameMr + "\n(" + nameEn + ")", 13, true, Color.parseColor("#1E293B"));
                title.setPadding(0, dp(4), 0, dp(4));
                card.addView(title);

                // Price
                TextView price = createTextView("₹ " + String.format(Locale.US, "%.0f", sellingPrice) + " / " + unit, 14, true, Color.parseColor("#E85D04"));
                card.addView(price);

                double stock = p.optDouble("stock", 0);
                double lowStockAt = p.optDouble("lowStockAt", 5);

                TextView badge = new TextView(this);
                if ("owner".equalsIgnoreCase(userRole)) {
                    if (stock <= lowStockAt) {
                        badge.setText("LOW STOCK (" + (int)stock + ")");
                        badge.setBackgroundResource(R.drawable.bg_badge_low_stock);
                        badge.setTextColor(Color.parseColor("#DC2626"));
                    } else {
                        badge.setText("Stock: " + (int)stock + " " + unit);
                        badge.setBackgroundResource(R.drawable.bg_badge_in_stock);
                        badge.setTextColor(Color.parseColor("#16A34A"));
                    }
                } else {
                    if (stock > 0) {
                        badge.setText("Available / उपलब्ध");
                        badge.setBackgroundResource(R.drawable.bg_badge_in_stock);
                        badge.setTextColor(Color.parseColor("#16A34A"));
                    } else {
                        badge.setText("Out of Stock / अनुपलब्ध");
                        badge.setBackgroundResource(R.drawable.bg_badge_low_stock);
                        badge.setTextColor(Color.parseColor("#DC2626"));
                    }
                }
                badge.setTextSize(10);
                badge.setTypeface(null, Typeface.BOLD);
                badge.setPadding(dp(6), dp(2), dp(6), dp(2));
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
                bp.topMargin = dp(4);
                bp.bottomMargin = dp(8);
                card.addView(badge, bp);

                // Buttons
                Button detailBtn = createStyledButton("View Details", Color.parseColor("#0284C7"), () -> showProductDetailsDialog(p));
                card.addView(detailBtn, new LinearLayout.LayoutParams(-1, dp(34)));

                if ("owner".equalsIgnoreCase(userRole)) {
                    LinearLayout bRow = new LinearLayout(this);
                    bRow.setOrientation(LinearLayout.HORIZONTAL);
                    
                    Button editBtn = createStyledButton("Edit", Color.parseColor("#C94B00"), () -> showEditProductDialog(p));
                    Button delBtn = createStyledButton("Del", Color.parseColor("#DC2626"), () -> deleteProduct(p.optString("_id")));
                    
                    bRow.addView(editBtn, new LinearLayout.LayoutParams(0, dp(34), 1.0f));
                    View space = new View(this);
                    bRow.addView(space, new LinearLayout.LayoutParams(dp(4), 1));
                    bRow.addView(delBtn, new LinearLayout.LayoutParams(0, dp(34), 1.0f));
                    
                    LinearLayout.LayoutParams brp = new LinearLayout.LayoutParams(-1, -2);
                    brp.topMargin = dp(4);
                    card.addView(bRow, brp);
                }

                GridLayout.LayoutParams glp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                );
                glp.width = 0;
                glp.height = -2;
                glp.setMargins(dp(4), dp(4), dp(4), dp(4));
                grid.addView(card, glp);
            }

            if (count == 0) {
                listContainer.addView(createTextView("No products found.", 14, false, Color.parseColor("#64748B")));
            } else {
                listContainer.addView(grid);
            }
        } catch (Exception e) {
            listContainer.addView(createTextView("Error: " + e.getMessage(), 14, false, Color.parseColor("#DC2626")));
        }
    }

    private void showProductDetailsDialog(JSONObject p) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));

        String imageUrl = p.optString("imageUrl", "");
        if (!imageUrl.isEmpty()) {
            ImageView imgView = new ImageView(this);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(-1, dp(220));
            imgLp.bottomMargin = dp(14);
            imgView.setLayoutParams(imgLp);
            imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgView.setBackgroundColor(Color.parseColor("#F8FAFC"));
            Glide.with(this).load(Api.resolveImageUrl(this, imageUrl)).placeholder(R.drawable.bg_card).error(android.R.drawable.ic_menu_gallery).into(imgView);
            box.addView(imgView);
        }

        TextView name = createTextView(p.optString("nameMr") + " (" + p.optString("nameEn") + ")", 18, true, Color.parseColor("#1E293B"));
        TextView price = createTextView("₹ " + String.format(Locale.US, "%.0f", p.optDouble("sellingPrice")) + " / " + p.optString("unit"), 16, true, Color.parseColor("#E85D04"));
        
        TextView stock;
        if ("owner".equalsIgnoreCase(userRole)) {
            stock = createTextView("In Stock (" + (int)p.optDouble("stock") + " " + p.optString("unit") + ")", 13, false, Color.parseColor("#16A34A"));
        } else {
            double st = p.optDouble("stock", 0);
            stock = createTextView(st > 0 ? "Available / उपलब्ध" : "Out of Stock / सध्या उपलब्ध नाही", 13, true, st > 0 ? Color.parseColor("#16A34A") : Color.parseColor("#DC2626"));
        }
        
        TextView desc = createTextView("उत्कृष्ट दर्जाचे पशुआहार. गाई व म्हशींच्या दुधवाढीसाठी उपयुक्त.", 13, false, Color.parseColor("#64748B"));
        desc.setPadding(0, dp(8), 0, dp(12));

        box.addView(name);
        box.addView(price);
        box.addView(stock);
        box.addView(desc);

        new AlertDialog.Builder(this)
                .setTitle("Product Details")
                .setView(box)
                .setPositiveButton("OK", null)
                .show();
    }

    private static final int PICK_IMAGE_REQUEST = 1001;
    private ImageView activeDialogImagePreview;
    private String selectedImageUrl = "";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            uploadImageUriToServer(uri, activeDialogImagePreview);
        }
    }

    private void uploadImageUriToServer(Uri uri, ImageView previewView) {
        Toast.makeText(this, "Uploading image from gallery...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) inputStream.close();

                int maxDim = 800;
                if (bitmap.getWidth() > maxDim || bitmap.getHeight() > maxDim) {
                    float ratio = Math.min((float) maxDim / bitmap.getWidth(), (float) maxDim / bitmap.getHeight());
                    bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(bitmap.getWidth() * ratio), Math.round(bitmap.getHeight() * ratio), true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] bytes = baos.toByteArray();
                String base64 = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);

                JSONObject body = new JSONObject();
                body.put("image", base64);

                JSONObject res = Api.request(this, "POST", "/api/uploads", body);
                String serverUrl = res.optString("url", "");

                runOnUiThread(() -> {
                    if (!serverUrl.isEmpty()) {
                        selectedImageUrl = serverUrl;
                        Toast.makeText(this, "Gallery image uploaded successfully!", Toast.LENGTH_SHORT).show();
                        if (previewView != null) {
                            previewView.setVisibility(View.VISIBLE);
                            Glide.with(this).load(serverUrl).into(previewView);
                        }
                    } else {
                        Toast.makeText(this, "Upload failed: empty URL", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Image upload error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showAddProductDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(12));

        String[] categories = {"कांडी", "वालीस", "पेंड", "मक्याचे पीठ", "इतर"};
        Spinner catSpinner = new Spinner(this);
        catSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));

        EditText nameMr = createFormInput("Product Name (Marathi)", "पेंड (सरकी पेंड)", InputType.TYPE_CLASS_TEXT);
        EditText nameEn = createFormInput("Product Name (English)", "Cottonseed Cake (Pend)", InputType.TYPE_CLASS_TEXT);

        selectedImageUrl = "";
        ImageView previewImg = new ImageView(this);
        previewImg.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(140)));
        previewImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        previewImg.setVisibility(View.GONE);
        box.addView(previewImg);

        Button browseBtn = createStyledButton("📷 Choose Image / Browse Gallery", Color.parseColor("#0284C7"), () -> {
            activeDialogImagePreview = previewImg;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Product Image"), PICK_IMAGE_REQUEST);
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(42));
        blp.topMargin = dp(6);
        blp.bottomMargin = dp(10);
        box.addView(browseBtn, blp);

        String[] units = {"bag", "kg", "quintal", "piece"};
        Spinner unitSpinner = new Spinner(this);
        unitSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units));

        EditText purchasePrice = createFormInput("Purchase Cost Price (₹)", "1400", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText sellingPrice = createFormInput("Selling Price (₹)", "1650", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText stock = createFormInput("Initial Stock Quantity", "50", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText lowStock = createFormInput("Low Stock Alert Threshold", "5", InputType.TYPE_CLASS_NUMBER);

        box.addView(createTextView("Category / प्रकार:", 13, true, Color.parseColor("#1E293B")));
        box.addView(catSpinner);
        box.addView(createTextView("Product Name (Marathi):", 12, false, Color.parseColor("#64748B")));
        box.addView(nameMr);
        box.addView(createTextView("Product Name (English):", 12, false, Color.parseColor("#64748B")));
        box.addView(nameEn);
        box.addView(createTextView("Unit / एकक:", 13, true, Color.parseColor("#1E293B")));
        box.addView(unitSpinner);
        box.addView(createTextView("Purchase Price (₹):", 12, false, Color.parseColor("#64748B")));
        box.addView(purchasePrice);
        box.addView(createTextView("Selling Price (₹):", 12, false, Color.parseColor("#64748B")));
        box.addView(sellingPrice);
        box.addView(createTextView("Initial Stock Quantity:", 12, false, Color.parseColor("#64748B")));
        box.addView(stock);
        box.addView(createTextView("Low Stock Alert Threshold:", 12, false, Color.parseColor("#64748B")));
        box.addView(lowStock);

        new AlertDialog.Builder(this)
                .setTitle("Add Product / नवीन उत्पादन जोडा")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save Product", (d, w) -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("category", categories[catSpinner.getSelectedItemPosition()]);
                        body.put("nameMr", nameMr.getText().toString().trim());
                        body.put("nameEn", nameEn.getText().toString().trim());
                        body.put("imageUrl", selectedImageUrl);
                        body.put("unit", units[unitSpinner.getSelectedItemPosition()]);
                        body.put("purchasePrice", Double.parseDouble(purchasePrice.getText().toString().trim()));
                        body.put("sellingPrice", Double.parseDouble(sellingPrice.getText().toString().trim()));
                        body.put("stock", Double.parseDouble(stock.getText().toString().trim()));
                        body.put("lowStockAt", Integer.parseInt(lowStock.getText().toString().trim()));

                        new Thread(() -> {
                            try {
                                Api.request(this, "POST", "/api/products", body);
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Product saved successfully!", Toast.LENGTH_SHORT).show();
                                    loadProductManagement();
                                });
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        }).start();
                    } catch (Exception ex) {
                        Toast.makeText(this, "Please enter valid numeric fields.", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void showEditProductDialog(JSONObject p) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(12));

        selectedImageUrl = p.optString("imageUrl", "");
        ImageView previewImg = new ImageView(this);
        previewImg.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(140)));
        previewImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (!selectedImageUrl.isEmpty()) {
            previewImg.setVisibility(View.VISIBLE);
            Glide.with(this).load(selectedImageUrl).into(previewImg);
        } else {
            previewImg.setVisibility(View.GONE);
        }
        box.addView(previewImg);

        Button browseBtn = createStyledButton("📷 Change Image / Browse Gallery", Color.parseColor("#0284C7"), () -> {
            activeDialogImagePreview = previewImg;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Product Image"), PICK_IMAGE_REQUEST);
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(42));
        blp.topMargin = dp(6);
        blp.bottomMargin = dp(10);
        box.addView(browseBtn, blp);

        EditText sellingPrice = createFormInput("Selling Price (₹)", String.valueOf(p.optDouble("sellingPrice")), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText purchasePrice = createFormInput("Cost/Purchase Price (₹)", String.valueOf(p.optDouble("purchasePrice")), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText stock = createFormInput("Stock Quantity", String.valueOf(p.optDouble("stock")), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        box.addView(createTextView("Edit: " + p.optString("nameMr") + " (" + p.optString("nameEn") + ")", 14, true, Color.parseColor("#1E293B")));
        box.addView(createTextView("Selling Price (₹):", 12, false, Color.parseColor("#64748B")));
        box.addView(sellingPrice);
        box.addView(createTextView("Cost Price (₹):", 12, false, Color.parseColor("#64748B")));
        box.addView(purchasePrice);
        box.addView(createTextView("Stock Quantity:", 12, false, Color.parseColor("#64748B")));
        box.addView(stock);

        new AlertDialog.Builder(this)
                .setTitle("Edit Product & Image")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Update", (d, w) -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("sellingPrice", Double.parseDouble(sellingPrice.getText().toString().trim()));
                        body.put("purchasePrice", Double.parseDouble(purchasePrice.getText().toString().trim()));
                        body.put("stock", Double.parseDouble(stock.getText().toString().trim()));
                        body.put("imageUrl", selectedImageUrl);

                        new Thread(() -> {
                            try {
                                Api.request(this, "PATCH", "/api/products/" + p.optString("_id"), body);
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Product and image updated!", Toast.LENGTH_SHORT).show();
                                    loadProductManagement();
                                });
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        }).start();
                    } catch (Exception ex) {
                        Toast.makeText(this, "Please enter valid numeric fields.", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void deleteProduct(String id) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Product / उत्पादन हटवा")
            .setMessage("Are you sure you want to delete this product? It will be removed from all customer and owner product sections. You can restore it anytime from Owner Settings.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", (d, w) -> {
                new Thread(() -> {
                    try {
                        Api.request(this, "DELETE", "/api/products/" + id, null);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Product deleted from catalog successfully.", Toast.LENGTH_SHORT).show();
                            loadProductManagement();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }).start();
            })
            .show();
    }

    private void showRestorableProductsDialog() {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        box.addView(createTextView("Restore Deleted Products / हटवलेली उत्पादने", 18, true, Color.parseColor("#1E293B")));
        box.addView(createTextView("Tap restore to bring a deleted product back to the active catalog.", 12, false, Color.parseColor("#64748B")));

        ScrollView scroll = new ScrollView(this);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);

        listContainer.addView(createTextView("Loading deleted products...", 13, false, Color.parseColor("#64748B")));

        box.addView(scroll, new LinearLayout.LayoutParams(-1, dp(400)));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(box)
            .setPositiveButton("Close", null)
            .show();

        new Thread(() -> {
            try {
                JSONObject res = Api.request(this, "GET", "/api/products/archived", null);
                JSONArray products = res.optJSONArray("products");

                runOnUiThread(() -> {
                    listContainer.removeAllViews();

                    if (products == null || products.length() == 0) {
                        listContainer.addView(createTextView("No deleted products found. All products are active in the catalog.", 14, false, Color.parseColor("#16A34A")));
                        return;
                    }

                    for (int i = 0; i < products.length(); i++) {
                        JSONObject p = products.optJSONObject(i);
                        if (p == null) continue;

                        String pId = p.optString("_id");
                        String nameMr = p.optString("nameMr", "");
                        String nameEn = p.optString("nameEn", "");
                        double price = p.optDouble("sellingPrice", 0);
                        String unit = p.optString("unit", "bag");

                        LinearLayout pCard = new LinearLayout(this);
                        pCard.setOrientation(LinearLayout.VERTICAL);
                        pCard.setPadding(dp(12), dp(10), dp(12), dp(10));
                        pCard.setBackgroundResource(R.drawable.bg_card);
                        LinearLayout.LayoutParams pcp = new LinearLayout.LayoutParams(-1, -2);
                        pcp.bottomMargin = dp(10);

                        pCard.addView(createTextView(nameMr + " (" + nameEn + ")", 15, true, Color.parseColor("#1E293B")));
                        pCard.addView(createTextView("Price: ₹" + String.format(Locale.US, "%.0f", price) + " / " + unit, 13, false, Color.parseColor("#64748B")));

                        LinearLayout btnRow = new LinearLayout(this);
                        btnRow.setOrientation(LinearLayout.HORIZONTAL);
                        btnRow.setPadding(0, dp(8), 0, 0);

                        Button restoreBtn = createStyledButton("🔄 Restore", Color.parseColor("#16A34A"), () -> {
                            new Thread(() -> {
                                try {
                                    Api.request(this, "POST", "/api/products/" + pId + "/restore", null);
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, "Product " + nameMr + " restored successfully!", Toast.LENGTH_SHORT).show();
                                        if (dialog != null) dialog.dismiss();
                                        showRestorableProductsDialog();
                                    });
                                } catch (Exception ex) {
                                    runOnUiThread(() -> Toast.makeText(this, "Error restoring: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
                                }
                            }).start();
                        });

                        Button permDelBtn = createStyledButton("🗑️ Delete Permanently", Color.parseColor("#DC2626"), () -> {
                            new AlertDialog.Builder(this)
                                .setTitle("Permanently Delete Product?")
                                .setMessage("Are you sure you want to permanently delete " + nameMr + " (" + nameEn + ") from the database? This action cannot be undone.")
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Permanently Delete", (d, w) -> {
                                    new Thread(() -> {
                                        try {
                                            Api.request(this, "DELETE", "/api/products/" + pId + "/permanent", null);
                                            runOnUiThread(() -> {
                                                Toast.makeText(this, "Product permanently deleted from database.", Toast.LENGTH_SHORT).show();
                                                if (dialog != null) dialog.dismiss();
                                                showRestorableProductsDialog();
                                            });
                                        } catch (Exception ex) {
                                            runOnUiThread(() -> Toast.makeText(this, "Error deleting: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
                                        }
                                    }).start();
                                })
                                .show();
                        });

                        btnRow.addView(restoreBtn, new LinearLayout.LayoutParams(0, dp(38), 1.0f));
                        View sp = new View(this); btnRow.addView(sp, new LinearLayout.LayoutParams(dp(8), 1));
                        btnRow.addView(permDelBtn, new LinearLayout.LayoutParams(0, dp(38), 1.0f));

                        pCard.addView(btnRow);

                        listContainer.addView(pCard, pcp);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    listContainer.removeAllViews();
                    listContainer.addView(createTextView("Error loading archived products: " + e.getMessage(), 13, false, Color.parseColor("#DC2626")));
                });
            }
        }).start();
    }

    // ==========================================
    // POS BILLING, PURCHASES & CUSTOMERS (PHASE 3)
    // ==========================================

    private void loadPOSBilling() {
        activeTab = "billing";
        buildBottomNav();
        statusTextView.setText("New Sale & POS Billing");

        mainContentContainer.removeAllViews();
        mainContentContainer.addView(createTextView("New Sale", 22, true, Color.parseColor("#1E293B")));

        LinearLayout formCard = new LinearLayout(this);
        formCard.setOrientation(LinearLayout.VERTICAL);
        formCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        formCard.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams fcp = new LinearLayout.LayoutParams(-1, -2);
        fcp.topMargin = dp(10);
        mainContentContainer.addView(formCard, fcp);

        new Thread(() -> {
            try {
                JSONObject prodRes = Api.request(this, "GET", "/api/products", null);
                JSONObject custRes = Api.request(this, "GET", "/api/customers/ledger", null);

                JSONArray products = prodRes.getJSONArray("products");
                JSONArray ledger = custRes.getJSONArray("ledger");

                runOnUiThread(() -> renderPOSForm(formCard, products, ledger));
            } catch (Exception e) {
                runOnUiThread(() -> formCard.addView(createTextView("Failed: " + e.getMessage(), 14, false, Color.parseColor("#DC2626"))));
            }
        }).start();
    }

    private class CartItem {
        JSONObject product;
        double quantity;
        double sellingPrice;
        CartItem(JSONObject p, double q, double sp) { product = p; quantity = q; sellingPrice = sp; }
    }

    private void renderPOSForm(LinearLayout formCard, JSONArray products, JSONArray ledger) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        formCard.removeAllViews();
        List<CartItem> cart = new ArrayList<>();

        // Customer Selection
        List<String> custNames = new ArrayList<>();
        List<String> custIds = new ArrayList<>();
        List<Double> custBalances = new ArrayList<>();

        custNames.add("Walk-in Customer");
        custIds.add("");
        custBalances.add(0.0);

        try {
            for (int i = 0; i < ledger.length(); i++) {
                JSONObject c = ledger.getJSONObject(i);
                custNames.add(c.optString("name") + " (" + c.optString("mobile") + ")");
                custIds.add(c.optString("id"));
                custBalances.add(c.optDouble("creditBalance", 0));
            }
        } catch (Exception ignored) {}

        AutoCompleteTextView custSpinner = new AutoCompleteTextView(this);
        custSpinner.setAdapter(new SearchableDropdownAdapter<>(this, custNames));
        custSpinner.setHint("Type or Click to select Customer");
        custSpinner.setThreshold(1);
        custSpinner.setTextSize(15);
        applySearchableDropdownStyle(custSpinner);
        custSpinner.setPadding(dp(14), dp(11), dp(14), dp(11));
        
        formCard.addView(createTextView("Select Customer (Manual/Dropdown):", 13, true, Color.parseColor("#1E293B")));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, dp(46));
        clp.bottomMargin = dp(10);
        formCard.addView(custSpinner, clp);

        TextView previousBalanceView = createTextView("Previous Outstanding: ₹0", 14, true, Color.parseColor("#DC2626"));
        previousBalanceView.setPadding(0, 0, 0, dp(12));
        previousBalanceView.setVisibility(View.GONE);
        formCard.addView(previousBalanceView);

        // Product Selection
        List<String> prodNames = new ArrayList<>();
        List<JSONObject> prodObjs = new ArrayList<>();
        try {
            for (int i = 0; i < products.length(); i++) {
                JSONObject p = products.getJSONObject(i);
                if (p.optBoolean("active", true)) {
                    prodNames.add(p.optString("nameMr") + " - ₹" + p.optDouble("sellingPrice") + "/" + p.optString("unit"));
                    prodObjs.add(p);
                }
            }
        } catch (Exception ignored) {}

        AutoCompleteTextView prodSpinner = new AutoCompleteTextView(this);
        prodSpinner.setAdapter(new SearchableDropdownAdapter<>(this, prodNames));
        prodSpinner.setHint("Search or Select Product");
        prodSpinner.setThreshold(1);
        prodSpinner.setTextSize(15);
        applySearchableDropdownStyle(prodSpinner);
        prodSpinner.setPadding(dp(14), dp(11), dp(14), dp(11));

        formCard.addView(createTextView("Add Product to Cart:", 13, true, Color.parseColor("#1E293B")));
        
        LinearLayout addProdRow = new LinearLayout(this);
        addProdRow.setOrientation(LinearLayout.HORIZONTAL);
        
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, dp(46), 1.0f);
        plp.rightMargin = dp(8);
        addProdRow.addView(prodSpinner, plp);
        
        Button addToCartBtn = createStyledButton("ADD +", Color.parseColor("#0284C7"), null);
        addProdRow.addView(addToCartBtn, new LinearLayout.LayoutParams(dp(70), dp(46)));
        formCard.addView(addProdRow);

        // Cart Container
        LinearLayout cartContainer = new LinearLayout(this);
        cartContainer.setOrientation(LinearLayout.VERTICAL);
        cartContainer.setPadding(0, dp(12), 0, dp(12));
        formCard.addView(cartContainer);
        formCard.setTag(cartContainer); // store reference


        // Totals & Payment Fields
        EditText discountInput = createFormInput("Discount (₹)", "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText paidInput = createFormInput("Received Amount (₹)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Mixed Payment Fields
        LinearLayout mixedBox = new LinearLayout(this);
        mixedBox.setOrientation(LinearLayout.VERTICAL);
        mixedBox.setVisibility(View.GONE);

        EditText cashInput = createFormInput("Cash Amount Received (₹)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText upiInput = createFormInput("UPI Amount Received (₹)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText upiRefInput = createFormInput("UPI Reference / Ref No (Optional)", "", InputType.TYPE_CLASS_TEXT);

        mixedBox.addView(createTextView("Cash Component (₹):", 12, false, Color.parseColor("#64748B")));
        mixedBox.addView(cashInput);
        mixedBox.addView(createTextView("UPI Component (₹):", 12, false, Color.parseColor("#64748B")));
        mixedBox.addView(upiInput);
        mixedBox.addView(createTextView("UPI Reference No:", 12, false, Color.parseColor("#64748B")));
        mixedBox.addView(upiRefInput);

        String[] paymentMethods = {"Cash only", "Online / UPI only", "Cash + UPI (Mixed)", "Credit (Unpaid)"};
        TextView paymentSpinner = new TextView(this);
        paymentSpinner.setText(paymentMethods[0] + " ▾");
        paymentSpinner.setTextSize(15);
        paymentSpinner.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        paymentSpinner.setBackgroundResource(R.drawable.bg_card);
        paymentSpinner.setPadding(dp(14), dp(11), dp(14), dp(11));
        paymentSpinner.setGravity(Gravity.CENTER_VERTICAL);
        paymentSpinner.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);

        final int[] selectedMethodIdx = new int[]{0};

        paymentSpinner.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Select Payment Method")
                .setItems(paymentMethods, (dlg, which) -> {
                    selectedMethodIdx[0] = which;
                    paymentSpinner.setText(paymentMethods[which] + " ▾");
                    paymentSpinner.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));

                    if (which == 2) { // Mixed Cash + UPI
                        mixedBox.setVisibility(View.VISIBLE);
                        paidInput.setVisibility(View.GONE);
                    } else {
                        mixedBox.setVisibility(View.GONE);
                        paidInput.setVisibility(View.VISIBLE);
                    }
                })
                .show();
        });

        TextView subtotalView = createTextView("Subtotal: ₹0", 14, true, Color.parseColor("#1E293B"));
        TextView totalView = createTextView("New Sale Total: ₹0", 16, true, Color.parseColor("#1E293B"));
        TextView payableView = createTextView("Total Payable (with Old Balance): ₹0", 16, true, Color.parseColor("#DC2626"));

        formCard.addView(subtotalView);
        formCard.addView(createTextView("Discount (₹):", 12, false, Color.parseColor("#64748B")));
        formCard.addView(discountInput);
        formCard.addView(totalView);
        formCard.addView(payableView);
        formCard.addView(createTextView("Payment Method:", 12, false, Color.parseColor("#64748B")));
        
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(46));
        sp.bottomMargin = dp(10);
        formCard.addView(paymentSpinner, sp);
        
        formCard.addView(createTextView("Received Amount Now (₹):", 12, false, Color.parseColor("#64748B")));
        formCard.addView(paidInput);
        formCard.addView(mixedBox);

        custSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            custSpinner.setText(selected, false);
            custSpinner.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.BLACK);
            custSpinner.setSelection(0);
            custSpinner.clearFocus();
            int pos = custNames.indexOf(selected);
            if (pos > 0 && custBalances.get(pos) > 0) {
                previousBalanceView.setVisibility(View.VISIBLE);
                previousBalanceView.setText("Previous Outstanding: ₹" + String.format(Locale.US, "%.0f", custBalances.get(pos)));
            } else {
                previousBalanceView.setVisibility(View.GONE);
            }
            updateTotals(cart, discountInput, subtotalView, totalView, payableView, pos > 0 ? custBalances.get(pos) : 0);
        });

        prodSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            prodSpinner.setText(selected, false);
            prodSpinner.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.BLACK);
            prodSpinner.setSelection(0);
            prodSpinner.clearFocus();
        });
        
        addToCartBtn.setOnClickListener(v -> {
            String selected = prodSpinner.getText().toString();
            int pIdx = prodNames.indexOf(selected);
            if (pIdx >= 0 && pIdx < prodObjs.size()) {
                JSONObject p = prodObjs.get(pIdx);
                boolean exists = false;
                for (CartItem ci : cart) {
                    if (ci.product.optString("_id").equals(p.optString("_id"))) {
                        ci.quantity += 1;
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    cart.add(new CartItem(p, 1, p.optDouble("sellingPrice")));
                }
                
                String cName = custSpinner.getText().toString();
                int cpos = custNames.indexOf(cName);
                updateCartUI(cart, cartContainer, discountInput, subtotalView, totalView, payableView, cpos > 0 ? custBalances.get(cpos) : 0);
                prodSpinner.setText(""); // clear after adding
            } else {
                Toast.makeText(this, "Please select a valid product from list.", Toast.LENGTH_SHORT).show();
            }
        });

        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String cName = custSpinner.getText().toString();
                int cpos = custNames.indexOf(cName);
                updateTotals(cart, discountInput, subtotalView, totalView, payableView, cpos > 0 ? custBalances.get(cpos) : 0);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        discountInput.addTextChangedListener(tw);

        // Save Button
        Button createBillBtn = createStyledButton("Create Bill", Color.parseColor("#16A34A"), () -> {
            try {
                if (cart.isEmpty()) {
                    Toast.makeText(this, "Cart is empty.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String cName = custSpinner.getText().toString();
                int cIdx = custNames.indexOf(cName);
                if (cIdx < 0 && !cName.isEmpty() && !cName.equals("Walk-in Customer")) {
                    Toast.makeText(this, "Please select a valid customer from the dropdown.", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                double disc = discountInput.getText().toString().trim().isEmpty() ? 0 : Double.parseDouble(discountInput.getText().toString().trim());
                
                double sub = 0;
                JSONArray items = new JSONArray();
                for (CartItem ci : cart) {
                    sub += (ci.quantity * ci.sellingPrice);
                    JSONObject it = new JSONObject();
                    it.put("productId", ci.product.optString("_id"));
                    it.put("quantity", ci.quantity);
                    it.put("unitPrice", ci.sellingPrice);
                    items.put(it);
                }
                
                double total = Math.max(0, sub - disc);
                double cashAmt = 0;
                double upiAmt = 0;
                double paid = 0;
                String methodCode = "cash";
                String uRef = "";

                int pMethodWhich = selectedMethodIdx[0];
                if (pMethodWhich == 2) { // Mixed Cash + UPI
                    String cStr = cashInput.getText().toString().trim();
                    String uStr = upiInput.getText().toString().trim();
                    cashAmt = cStr.isEmpty() ? 0 : Double.parseDouble(cStr);
                    upiAmt = uStr.isEmpty() ? 0 : Double.parseDouble(uStr);
                    uRef = upiRefInput.getText().toString().trim();
                    paid = cashAmt + upiAmt;
                    methodCode = "split";
                    if (paid <= 0) {
                        Toast.makeText(this, "Please enter cash or UPI payment amount.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else if (pMethodWhich == 1) { // Online / UPI
                    String paidStr = paidInput.getText().toString().trim();
                    paid = paidStr.isEmpty() ? total : Double.parseDouble(paidStr);
                    upiAmt = paid;
                    methodCode = "upi";
                } else if (pMethodWhich == 3) { // Credit (Unpaid)
                    paid = 0;
                    methodCode = "credit";
                } else { // Cash only
                    String paidStr = paidInput.getText().toString().trim();
                    paid = paidStr.isEmpty() ? total : Double.parseDouble(paidStr);
                    cashAmt = paid;
                    methodCode = "cash";
                }

                JSONObject saleBody = new JSONObject();
                if (cIdx > 0 && !custIds.get(cIdx).isEmpty()) {
                    saleBody.put("customerId", custIds.get(cIdx));
                }
                saleBody.put("items", items);
                saleBody.put("discount", disc);
                saleBody.put("cashAmount", cashAmt);
                saleBody.put("upiAmount", upiAmt);
                saleBody.put("upiRefNo", uRef);
                saleBody.put("paidAmount", paid);
                saleBody.put("paymentMethod", methodCode);
                saleBody.put("previousBalance", cIdx > 0 ? custBalances.get(cIdx) : 0.0);

                new Thread(() -> {
                    try {
                        JSONObject res = Api.request(this, "POST", "/api/sales", saleBody);
                        JSONObject sale = res.getJSONObject("sale");
                        
                        // We must pass the previous balance to PDF generator too.
                        // We can embed it into the sale object temporarily for PDF.
                        sale.put("previousBalance", cIdx > 0 ? custBalances.get(cIdx) : 0.0);

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Bill Created! " + sale.optString("invoiceNo"), Toast.LENGTH_LONG).show();
                            try {
                                File pdf = PdfGenerator.generateInvoicePdf(this, sale, cIdx > 0 ? custNames.get(cIdx) : "Walk-in");
                                promptSharePdf(pdf);
                            } catch (Exception ex) {}
                            loadOwnerDashboard();
                        });
                    } catch (Exception ex) {
                        runOnUiThread(() -> Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }).start();
            } catch (Exception ex) {
                Toast.makeText(this, "Invalid numbers.", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout.LayoutParams cbp = new LinearLayout.LayoutParams(-1, dp(50));
        cbp.topMargin = dp(16);
        formCard.addView(createBillBtn, cbp);
        
        // Initial ui update
        updateCartUI(cart, cartContainer, discountInput, subtotalView, totalView, payableView, 0);
    }
    
    private void updateCartUI(List<CartItem> cart, LinearLayout cartContainer, EditText discountInput, TextView subtotalView, TextView totalView, TextView payableView, double prevBalance) {
        cartContainer.removeAllViews();
        
        for (int i = 0; i < cart.size(); i++) {
            final int idx = i;
            CartItem ci = cart.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));
            
            TextView name = createTextView(ci.product.optString("nameMr"), 14, false, Color.parseColor("#1E293B"));
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1.0f));
            
            Button minusBtn = createStyledButton("-", Color.parseColor("#94A3B8"), () -> {
                if (cart.get(idx).quantity > 1) {
                    cart.get(idx).quantity--;
                } else {
                    cart.remove(idx);
                }
                updateCartUI(cart, cartContainer, discountInput, subtotalView, totalView, payableView, prevBalance);
            });
            row.addView(minusBtn, new LinearLayout.LayoutParams(dp(40), dp(40)));
            
            TextView qty = createTextView(String.valueOf(ci.quantity), 14, true, Color.parseColor("#1E293B"));
            qty.setPadding(dp(8), 0, dp(8), 0);
            row.addView(qty);
            
            Button plusBtn = createStyledButton("+", Color.parseColor("#94A3B8"), () -> {
                cart.get(idx).quantity++;
                updateCartUI(cart, cartContainer, discountInput, subtotalView, totalView, payableView, prevBalance);
            });
            row.addView(plusBtn, new LinearLayout.LayoutParams(dp(40), dp(40)));
            
            TextView lineTotal = createTextView(" ₹" + String.format(Locale.US, "%.0f", ci.quantity * ci.sellingPrice), 14, true, Color.parseColor("#16A34A"));
            lineTotal.setPadding(dp(8), 0, 0, 0);
            row.addView(lineTotal);
            
            cartContainer.addView(row);
        }
        
        updateTotals(cart, discountInput, subtotalView, totalView, payableView, prevBalance);
    }
    
    private void updateTotals(List<CartItem> cart, EditText discountInput, TextView subtotalView, TextView totalView, TextView payableView, double prevBalance) {
        double sub = 0;
        for (CartItem ci : cart) {
            sub += (ci.quantity * ci.sellingPrice);
        }
        
        double disc = 0;
        try { disc = discountInput.getText().toString().isEmpty() ? 0 : Double.parseDouble(discountInput.getText().toString()); } catch (Exception ignored) {}
        
        double total = Math.max(0, sub - disc);
        double payable = total + prevBalance;
        
        subtotalView.setText("Subtotal: ₹" + String.format(Locale.US, "%.0f", sub));
        totalView.setText("New Sale Total: ₹" + String.format(Locale.US, "%.0f", total));
        
        if (prevBalance > 0) {
            payableView.setVisibility(View.VISIBLE);
            payableView.setText("Total Payable (with Old Balance): ₹" + String.format(Locale.US, "%.0f", payable));
        } else {
            payableView.setVisibility(View.GONE);
        }
    }

    private void promptSharePdf(File pdfFile) {
        promptSharePdf(pdfFile, "Shravan Traders Invoice / Bill PDF attached.");
    }

    private void promptSharePdf(File pdfFile, String message) {
        CharSequence[] options = {"View PDF", "Share PDF", "Share on WhatsApp"};
        new AlertDialog.Builder(this)
                .setTitle("PDF Bill Generated / बिल तयार झाले")
                .setItems(options, (d, w) -> {
                    try {
                        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
                        if (w == 0) {
                            // View PDF
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, "application/pdf");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(intent, "Open PDF"));
                        } else if (w == 1) {
                            // Share PDF
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("application/pdf");
                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                            intent.putExtra(Intent.EXTRA_TEXT, message);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(intent, "Share Invoice PDF"));
                        } else if (w == 2) {
                            // Share on WhatsApp
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("application/pdf");
                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                            intent.putExtra(Intent.EXTRA_TEXT, message);
                            intent.setPackage("com.whatsapp");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try {
                                startActivity(intent);
                            } catch (Exception ex) {
                                Toast.makeText(this, "WhatsApp not installed. Opening general share...", Toast.LENGTH_SHORT).show();
                                intent.setPackage(null);
                                startActivity(Intent.createChooser(intent, "Share on WhatsApp"));
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error sharing PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showEditPurchaseDialog(JSONObject p) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(Color.parseColor("#FFF9F2"));

        double totalAmount = p.optDouble("totalAmount", 0);
        double paidAmount = p.optDouble("paidAmount", totalAmount);
        double remainingDue = Math.max(0, totalAmount - paidAmount);

        box.addView(createTextView("PO Ref: " + p.optString("purchaseNo"), 13, false, Color.parseColor("#64748B")));
        box.addView(createTextView("Purchase Total Cost: ₹" + String.format(Locale.US, "%.0f", totalAmount), 14, true, Color.parseColor("#1E293B")));
        box.addView(createTextView("Paid to Supplier So Far: ₹" + String.format(Locale.US, "%.0f", paidAmount), 14, false, Color.parseColor("#16A34A")));
        box.addView(createTextView("Current Remaining Supplier Due: ₹" + String.format(Locale.US, "%.0f", remainingDue), 15, true, Color.parseColor("#DC2626")));

        View div = new View(this); div.setBackgroundColor(Color.parseColor("#E2E8F0"));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, dp(1));
        dlp.setMargins(0, dp(10), 0, dp(10));
        box.addView(div, dlp);

        EditText supplierInput = createFormInput("Supplier Name", p.optString("supplierName"), InputType.TYPE_CLASS_TEXT);
        EditText addPaidInput = createFormInput("Add Payment to Supplier Now (₹)", String.valueOf(remainingDue > 0 ? remainingDue : 0), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText noteInput = createFormInput("Note / Remarks", p.optString("note", ""), InputType.TYPE_CLASS_TEXT);

        box.addView(createTextView("Supplier Name:", 12, false, Color.parseColor("#64748B")));
        box.addView(supplierInput);
        box.addView(createTextView("Add Payment to Supplier Now (₹):", 12, true, Color.parseColor("#16A34A")));
        box.addView(addPaidInput);
        box.addView(createTextView("Note / Remarks:", 12, false, Color.parseColor("#64748B")));
        box.addView(noteInput);

        new AlertDialog.Builder(this)
                .setTitle("Edit Purchase & Supplier Payment / खरेदी अपडेट करा")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save / अपडेट करा", (d, w) -> {
                    try {
                        String addPaidStr = addPaidInput.getText().toString().trim();
                        double addPaid = addPaidStr.isEmpty() ? 0 : Double.parseDouble(addPaidStr);

                        JSONObject body = new JSONObject();
                        body.put("supplierName", supplierInput.getText().toString().trim());
                        body.put("addPaidAmount", addPaid);
                        body.put("note", noteInput.getText().toString().trim());

                        new Thread(() -> {
                            try {
                                Api.request(this, "PATCH", "/api/purchases/" + p.optString("_id"), body);
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Supplier purchase and payment updated!", Toast.LENGTH_SHORT).show();
                                    loadOwnerDashboard();
                                });
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        }).start();
                    } catch (Exception ex) {
                        Toast.makeText(this, "Please enter valid numeric fields.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void deletePurchase(String id) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Purchase / खरेदी हटवा")
            .setMessage("Are you sure you want to delete this purchase record?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", (d, w) -> {
                new Thread(() -> {
                    try {
                        Api.request(this, "DELETE", "/api/purchases/" + id, null);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Purchase deleted successfully.", Toast.LENGTH_SHORT).show();
                            loadOwnerDashboard();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }).start();
            })
            .show();
    }

    private void showAddPurchaseDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(12));

        box.addView(createTextView("Loading products...", 14, false, Color.parseColor("#64748B")));

        new Thread(() -> {
            try {
                JSONObject prodRes = Api.request(this, "GET", "/api/products", null);
                JSONArray products = prodRes.getJSONArray("products");
                
                List<String> prodNames = new ArrayList<>();
                List<String> prodIds = new ArrayList<>();
                for (int i = 0; i < products.length(); i++) {
                    JSONObject p = products.getJSONObject(i);
                    prodNames.add(p.optString("nameMr") + " - " + p.optString("nameEn"));
                    prodIds.add(p.optString("_id"));
                }
                
                runOnUiThread(() -> {
                    box.removeAllViews();
                    AutoCompleteTextView prodSpinner = new AutoCompleteTextView(this);
                    prodSpinner.setAdapter(new SearchableDropdownAdapter<>(this, prodNames));
                    prodSpinner.setHint("Search / Select Product ▾");
                    prodSpinner.setThreshold(1);
                    prodSpinner.setTextSize(15);
                    applySearchableDropdownStyle(prodSpinner);
                    prodSpinner.setPadding(dp(14), dp(11), dp(14), dp(11));

                    EditText supplierInput = createFormInput("Supplier Name (e.g. Godrej Agrovet)", "Godrej Agrovet Ltd", InputType.TYPE_CLASS_TEXT);
                    EditText qtyInput = createFormInput("Quantity Purchased", "20", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    EditText priceInput = createFormInput("Unit Cost Price (₹)", "1400", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    EditText paidInput = createFormInput("Paid Amount to Supplier (₹)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

                    box.addView(createTextView("Select Product to Restock:", 12, false, Color.parseColor("#64748B")));
                    LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, dp(46));
                    plp.bottomMargin = dp(10);
                    box.addView(prodSpinner, plp);
                    
                    box.addView(createTextView("Supplier Name:", 12, false, Color.parseColor("#64748B")));
                    box.addView(supplierInput);
                    box.addView(createTextView("Quantity:", 12, false, Color.parseColor("#64748B")));
                    box.addView(qtyInput);
                    box.addView(createTextView("Unit Cost Price (₹):", 12, false, Color.parseColor("#64748B")));
                    box.addView(priceInput);
                    box.addView(createTextView("Paid Amount to Supplier (₹):", 12, false, Color.parseColor("#64748B")));
                    box.addView(paidInput);

                    new AlertDialog.Builder(this)
                            .setTitle("Add Purchase / खरेदी नोंदवा")
                            .setView(box)
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Save Purchase", (d, w) -> {
                                try {
                                    int pIdx = prodNames.indexOf(prodSpinner.getText().toString());
                                    if (pIdx < 0) {
                                        Toast.makeText(this, "Please select a valid product.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    
                                    double qty = Double.parseDouble(qtyInput.getText().toString().trim());
                                    double unitPrice = Double.parseDouble(priceInput.getText().toString().trim());
                                    double lineTot = qty * unitPrice;
                                    
                                    String paidStr = paidInput.getText().toString().trim();
                                    double paidAmt = paidStr.isEmpty() ? lineTot : Double.parseDouble(paidStr);

                                    JSONObject item = new JSONObject();
                                    item.put("productId", prodIds.get(pIdx));
                                    item.put("quantity", qty);
                                    item.put("unitPrice", unitPrice);
                                    
                                    JSONArray items = new JSONArray();
                                    items.put(item);
                                    
                                    JSONObject body = new JSONObject();
                                    body.put("supplierName", supplierInput.getText().toString().trim());
                                    body.put("items", items);
                                    body.put("paidAmount", paidAmt);
                                    body.put("paymentMethod", paidAmt >= lineTot ? "cash" : "credit");
                                    body.put("note", "Manual Entry");

                                    new Thread(() -> {
                                        try {
                                            Api.request(this, "POST", "/api/purchases", body);
                                            runOnUiThread(() -> {
                                                Toast.makeText(this, "Purchase recorded & stock updated!", Toast.LENGTH_SHORT).show();
                                                if (activeTab.equals("dash")) loadOwnerDashboard();
                                            });
                                        } catch (Exception e) {
                                            runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                        }
                                    }).start();
                                } catch (Exception ex) {
                                    Toast.makeText(this, "Invalid numbers.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to load products: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void loadCustomersAndCredit() {
        activeTab = "customers";
        buildBottomNav();
        statusTextView.setText("Customers & Credit Ledger");

        mainContentContainer.removeAllViews();

        LinearLayout topBox = new LinearLayout(this);
        topBox.setOrientation(LinearLayout.HORIZONTAL);
        topBox.setGravity(Gravity.CENTER_VERTICAL);

        topBox.addView(createTextView("Customers", 22, true, Color.parseColor("#1E293B")), new LinearLayout.LayoutParams(0, -2, 1.0f));

        Button addCustBtn = createStyledButton("+ Add Customer", Color.parseColor("#0284C7"), this::showAddCustomerDialog);
        topBox.addView(addCustBtn);
        mainContentContainer.addView(topBox);

        Button analyticsBtn = createStyledButton("📊 Customer Purchase Analytics", Color.parseColor("#E85D04"), this::showCustomerAnalyticsFilterDialog);
        LinearLayout.LayoutParams abp = new LinearLayout.LayoutParams(-1, dp(44));
        abp.topMargin = dp(8);
        abp.bottomMargin = dp(4);
        mainContentContainer.addView(analyticsBtn, abp);

        EditText searchBar = createFormInput("🔍 Search customer by name or mobile...", "", InputType.TYPE_CLASS_TEXT);
        mainContentContainer.addView(searchBar);

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lcp = new LinearLayout.LayoutParams(-1, -2);
        lcp.topMargin = dp(12);
        mainContentContainer.addView(listContainer, lcp);

        new Thread(() -> {
            try {
                JSONObject r = Api.request(this, "GET", "/api/customers/ledger", null);
                JSONArray ledger = sortJsonArrayByDouble(r.getJSONArray("ledger"), "creditBalance", true);

                runOnUiThread(() -> {
                    renderCustomerLedger(ledger, searchBar.getText().toString().trim(), listContainer);

                    searchBar.addTextChangedListener(new TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                            renderCustomerLedger(ledger, s.toString().trim(), listContainer);
                        }
                        @Override public void afterTextChanged(Editable s) {}
                    });
                });
            } catch (Exception e) {}
        }).start();
    }

    private void renderCustomerLedger(JSONArray ledger, String query, LinearLayout listContainer) {
        listContainer.removeAllViews();
        int count = 0;
        
        for (int i = 0; i < ledger.length(); i++) {
            try {
                JSONObject c = ledger.getJSONObject(i);
                String name = c.optString("name", "");
                String mobile = c.optString("mobile", "");
                
                if (!query.isEmpty() && !name.toLowerCase().contains(query.toLowerCase()) && !mobile.contains(query)) {
                    continue;
                }
                
                count++;
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(14), dp(12), dp(14), dp(12));
                card.setBackgroundResource(R.drawable.bg_card);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                cp.bottomMargin = dp(10);

                card.addView(createTextView(name + " (" + mobile + ")", 16, true, Color.parseColor("#1E293B")));

                double credit = c.optDouble("creditBalance", 0);
                card.addView(createTextView("Balance: ₹" + String.format(Locale.US, "%.0f", credit), 14, true, credit > 0 ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A")));

                // On click, show statement details
                card.setOnClickListener(v -> showCustomerStatementDialog(c.optString("id"), name));

                if ("owner".equalsIgnoreCase(userRole)) {
                    LinearLayout btnRow = new LinearLayout(this);
                    btnRow.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams brp = new LinearLayout.LayoutParams(-1, dp(40));
                    brp.topMargin = dp(8);

                    Button stmtBtn = createStyledButton(currentLang.equals("mr") ? "📋 खातेवही" : "📋 Statement", Color.parseColor("#0284C7"), () -> showCustomerStatementDialog(c.optString("id"), name));
                    btnRow.addView(stmtBtn, new LinearLayout.LayoutParams(0, -1, 1.2f));

                    // Only show Edit & Delete for real customer accounts (not walkin)
                    if (!"walkin".equals(c.optString("id"))) {
                        Button editBtn = createStyledButton(currentLang.equals("mr") ? "✏️ संपादन" : "✏️ Edit", Color.parseColor("#D97706"), () -> showEditCustomerDialog(c));
                        Button delBtn = createStyledButton(currentLang.equals("mr") ? "🗑️ हटवा" : "🗑️ Delete", Color.parseColor("#DC2626"), () -> confirmDeleteCustomer(c.optString("id"), name));
                        View s1 = new View(this); btnRow.addView(s1, new LinearLayout.LayoutParams(dp(6), 1));
                        btnRow.addView(editBtn, new LinearLayout.LayoutParams(0, -1, 1.0f));
                        View s2 = new View(this); btnRow.addView(s2, new LinearLayout.LayoutParams(dp(6), 1));
                        btnRow.addView(delBtn, new LinearLayout.LayoutParams(0, -1, 1.0f));
                    }

                    card.addView(btnRow, brp);
                }

                listContainer.addView(card, cp);
            } catch (Exception ignored) {}
        }
        
        if (count == 0) {
            listContainer.addView(createTextView("No customers found.", 14, false, Color.parseColor("#64748B")));
        }
    }

    private void showEditCustomerDialog(JSONObject c) {
        String id = c.optString("id");
        String currentName = c.optString("name", "");
        String currentMobile = c.optString("mobile", "");
        boolean currentActive = c.optBoolean("active", true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));

        box.addView(createTextView(currentLang.equals("mr") ? "ग्राहकाचे नाव (Customer Name)" : "Customer Name", 13, true, Color.parseColor("#475569")));
        EditText nameInput = createFormInput("Full Name", currentName, InputType.TYPE_CLASS_TEXT);
        box.addView(nameInput);

        box.addView(createTextView(currentLang.equals("mr") ? "मोबाईल नंबर (Mobile Number)" : "Mobile Number", 13, true, Color.parseColor("#475569")));
        EditText mobileInput = createFormInput("Mobile Number", currentMobile, InputType.TYPE_CLASS_PHONE);
        box.addView(mobileInput);

        CheckBox activeBox = new CheckBox(this);
        activeBox.setText(currentLang.equals("mr") ? "सक्रिय खाते (Active Account)" : "Active Customer Account");
        activeBox.setChecked(currentActive);
        activeBox.setTextSize(14);
        activeBox.setTextColor(Color.parseColor("#1E293B"));
        activeBox.setPadding(dp(4), dp(8), 0, dp(12));
        box.addView(activeBox);

        new AlertDialog.Builder(this)
            .setTitle(currentLang.equals("mr") ? "ग्राहक माहिती संपादन (Manage Customer)" : "Manage Customer Account")
            .setView(box)
            .setPositiveButton(currentLang.equals("mr") ? "सेव्ह करा" : "Save Changes", (dialog, which) -> {
                String newName = nameInput.getText().toString().trim();
                String newMobile = mobileInput.getText().toString().trim();
                boolean isActive = activeBox.isChecked();

                if (newName.isEmpty() || newMobile.isEmpty()) {
                    Toast.makeText(this, "Name and mobile cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(() -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("name", newName);
                        body.put("mobile", newMobile);
                        body.put("active", isActive);

                        JSONObject res = Api.request(this, "PATCH", "/api/customers/" + id, body);
                        runOnUiThread(() -> {
                            Toast.makeText(this, res.optString("message", "Customer updated successfully!"), Toast.LENGTH_SHORT).show();
                            loadCustomersAndCredit();
                        });
                    } catch (Exception ex) {
                        runOnUiThread(() -> Toast.makeText(this, "Update failed: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }).start();
            })
            .setNegativeButton(currentLang.equals("mr") ? "रद्द करा" : "Cancel", null)
            .show();
    }

    private void confirmDeleteCustomer(String customerId, String customerName) {
        new AlertDialog.Builder(this)
            .setTitle(currentLang.equals("mr") ? "ग्राहक खाते हटवा (Delete Customer)" : "Delete Customer Account")
            .setMessage((currentLang.equals("mr")
                    ? "तुम्हाला नक्की '" + customerName + "' चे खाते हटवायचे आहे का?\n\nयामुळे ग्राहक खाते हटवले जाईल. मागील व्यवहार हिशोबासाठी सुरक्षित राहतील."
                    : "Are you sure you want to delete customer '" + customerName + "'?\n\nThis will remove the customer account. Past ledger transactions will remain safe for business history."))
            .setPositiveButton(currentLang.equals("mr") ? "हटवा (Delete)" : "Delete", (dialog, which) -> {
                new Thread(() -> {
                    try {
                        JSONObject res = Api.request(this, "DELETE", "/api/customers/" + customerId, null);
                        runOnUiThread(() -> {
                            Toast.makeText(this, res.optString("message", "Customer deleted successfully!"), Toast.LENGTH_SHORT).show();
                            loadCustomersAndCredit();
                        });
                    } catch (Exception ex) {
                        runOnUiThread(() -> Toast.makeText(this, "Delete failed: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }).start();
            })
            .setNegativeButton(currentLang.equals("mr") ? "रद्द करा" : "Cancel", null)
            .show();
    }

    private String selectedDatePeriod = "month";
    private String selectedFilterCustomerId = "";
    private String selectedFilterProductId = "";
    private String selectedFilterStatus = "all";

    private void showCustomerAnalyticsFilterDialog() {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(Color.parseColor("#0F172A"));

        TextView title = createTextView("Customer Purchase Analytics", 20, true, Color.WHITE);
        TextView subtitle = createTextView("Layout and filters for customer purchase records", 12, false, Color.parseColor("#94A3B8"));
        subtitle.setPadding(0, dp(2), 0, dp(14));

        box.addView(title);
        box.addView(subtitle);

        box.addView(createTextView("Date range", 14, true, Color.WHITE));

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setPadding(0, dp(8), 0, dp(14));

        String[] periods = {"today", "week", "month", "year"};
        String[] periodLabels = {"Today", "Week", "Month", "Year"};
        List<TextView> periodPills = new ArrayList<>();

        selectedDatePeriod = "month";

        for (int i = 0; i < periods.length; i++) {
            final int pIdx = i;
            TextView pill = createTextView(periodLabels[i], 13, true, Color.WHITE);
            pill.setPadding(dp(12), dp(8), dp(12), dp(8));
            pill.setGravity(Gravity.CENTER);

            if (periods[i].equals(selectedDatePeriod)) {
                pill.setBackgroundResource(R.drawable.bg_card);
                pill.setTextColor(Color.parseColor("#1E293B"));
            } else {
                pill.setTextColor(Color.parseColor("#94A3B8"));
            }

            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, -2, 1.0f);
            plp.rightMargin = dp(4);
            dateRow.addView(pill, plp);
            periodPills.add(pill);

            pill.setOnClickListener(v -> {
                selectedDatePeriod = periods[pIdx];
                for (int k = 0; k < periodPills.size(); k++) {
                    if (k == pIdx) {
                        periodPills.get(k).setBackgroundResource(R.drawable.bg_card);
                        periodPills.get(k).setTextColor(Color.parseColor("#1E293B"));
                    } else {
                        periodPills.get(k).setBackground(null);
                        periodPills.get(k).setTextColor(Color.parseColor("#94A3B8"));
                    }
                }
            });
        }
        box.addView(dateRow);

        box.addView(createTextView("Customer", 14, true, Color.WHITE));
        TextView custDropdown = new TextView(this);
        custDropdown.setText("All Customers ▾");
        custDropdown.setTextSize(14);
        custDropdown.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        custDropdown.setBackgroundResource(R.drawable.bg_card);
        custDropdown.setPadding(dp(12), dp(10), dp(12), dp(10));
        custDropdown.setGravity(Gravity.CENTER_VERTICAL);
        custDropdown.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);

        LinearLayout.LayoutParams cdlp = new LinearLayout.LayoutParams(-1, dp(44));
        cdlp.topMargin = dp(4);
        cdlp.bottomMargin = dp(12);
        box.addView(custDropdown, cdlp);

        box.addView(createTextView("Product", 14, true, Color.WHITE));
        TextView prodDropdown = new TextView(this);
        prodDropdown.setText("All Products ▾");
        prodDropdown.setTextSize(14);
        prodDropdown.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        prodDropdown.setBackgroundResource(R.drawable.bg_card);
        prodDropdown.setPadding(dp(12), dp(10), dp(12), dp(10));
        prodDropdown.setGravity(Gravity.CENTER_VERTICAL);
        prodDropdown.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);

        LinearLayout.LayoutParams pdlp = new LinearLayout.LayoutParams(-1, dp(44));
        pdlp.topMargin = dp(4);
        pdlp.bottomMargin = dp(12);
        box.addView(prodDropdown, pdlp);

        box.addView(createTextView("Payment status", 14, true, Color.WHITE));

        RadioGroup statusGroup = new RadioGroup(this);
        statusGroup.setOrientation(RadioGroup.VERTICAL);
        statusGroup.setPadding(0, dp(6), 0, dp(14));

        RadioButton rAll = new RadioButton(this); rAll.setText("All purchases"); rAll.setTextColor(Color.WHITE); rAll.setChecked(true);
        RadioButton rPaid = new RadioButton(this); rPaid.setText("Fully paid"); rPaid.setTextColor(Color.WHITE);
        RadioButton rPartial = new RadioButton(this); rPartial.setText("Partially paid"); rPartial.setTextColor(Color.WHITE);
        RadioButton rUnpaid = new RadioButton(this); rUnpaid.setText("Unpaid / Udhaar"); rUnpaid.setTextColor(Color.WHITE);

        statusGroup.addView(rAll); statusGroup.addView(rPaid); statusGroup.addView(rPartial); statusGroup.addView(rUnpaid);
        box.addView(statusGroup);

        selectedFilterCustomerId = "";
        selectedFilterProductId = "";
        selectedFilterStatus = "all";

        statusGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rPaid.getId()) selectedFilterStatus = "paid";
            else if (checkedId == rPartial.getId()) selectedFilterStatus = "partial";
            else if (checkedId == rUnpaid.getId()) selectedFilterStatus = "unpaid";
            else selectedFilterStatus = "all";
        });

        ScrollView resultsScroll = new ScrollView(this);
        LinearLayout resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        resultsScroll.addView(resultsContainer);
        resultsScroll.setVisibility(View.GONE);

        Button applyBtn = createStyledButton("Use these filter requirements", Color.parseColor("#F8FAFC"), () -> {
            fetchFilteredAnalyticsResults(resultsScroll, resultsContainer);
        });
        applyBtn.setTextColor(Color.parseColor("#0F172A"));
        LinearLayout.LayoutParams abpLp = new LinearLayout.LayoutParams(-1, dp(48));
        abpLp.topMargin = dp(10);
        abpLp.bottomMargin = dp(8);
        box.addView(applyBtn, abpLp);

        Button viewListBtn = createStyledButton("📋 View Filtered Purchases List", Color.parseColor("#EA580C"), () -> {
            showFilteredPurchasesListDialog();
        });
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-1, dp(44));
        vlp.bottomMargin = dp(10);
        box.addView(viewListBtn, vlp);

        box.addView(resultsScroll, new LinearLayout.LayoutParams(-1, dp(220)));

        List<String> cNames = new ArrayList<>(); cNames.add("All Customers");
        List<String> cIds = new ArrayList<>(); cIds.add("");

        List<String> pNames = new ArrayList<>(); pNames.add("All Products");
        List<String> pIds = new ArrayList<>(); pIds.add("");

        new Thread(() -> {
            try {
                JSONObject cRes = Api.request(this, "GET", "/api/customers/ledger", null);
                JSONObject pRes = Api.request(this, "GET", "/api/products", null);
                JSONArray cArr = cRes.optJSONArray("ledger");
                JSONArray pArr = pRes.optJSONArray("products");

                if (cArr != null) {
                    for (int i = 0; i < cArr.length(); i++) {
                        JSONObject c = cArr.optJSONObject(i);
                        if (c != null) { cNames.add(c.optString("name") + " (" + c.optString("mobile") + ")"); cIds.add(c.optString("id")); }
                    }
                }
                if (pArr != null) {
                    for (int i = 0; i < pArr.length(); i++) {
                        JSONObject p = pArr.optJSONObject(i);
                        if (p != null) { pNames.add(p.optString("nameMr") + " (" + p.optString("nameEn") + ")"); pIds.add(p.optString("_id")); }
                    }
                }

                runOnUiThread(() -> {
                    custDropdown.setText(cNames.get(0) + " ▾");
                    prodDropdown.setText(pNames.get(0) + " ▾");
                    custDropdown.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
                    prodDropdown.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));

                    custDropdown.setOnClickListener(v -> {
                        if (cNames.isEmpty()) return;
                        new AlertDialog.Builder(this)
                            .setTitle("Select Customer Filter")
                            .setItems(cNames.toArray(new String[0]), (dlg, which) -> {
                                selectedFilterCustomerId = cIds.get(which);
                                custDropdown.setText(cNames.get(which) + " ▾");
                                custDropdown.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));
                            })
                            .show();
                    });

                    prodDropdown.setOnClickListener(v -> {
                        if (pNames.isEmpty()) return;
                        new AlertDialog.Builder(this)
                            .setTitle("Select Product Filter")
                            .setItems(pNames.toArray(new String[0]), (dlg, which) -> {
                                selectedFilterProductId = pIds.get(which);
                                prodDropdown.setText(pNames.get(which) + " ▾");
                                prodDropdown.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));
                            })
                            .show();
                    });
                });
            } catch (Exception ignored) {}
        }).start();

        new AlertDialog.Builder(this)
            .setView(box)
            .setPositiveButton("Close", null)
            .show();
    }

    private void fetchFilteredAnalyticsResults(ScrollView resultsScroll, LinearLayout resultsContainer) {
        resultsScroll.setVisibility(View.VISIBLE);
        resultsContainer.removeAllViews();
        resultsContainer.addView(createTextView("Loading filtered analytics...", 13, false, Color.parseColor("#94A3B8")));

        new Thread(() -> {
            try {
                String query = "?period=" + selectedDatePeriod;
                if (!selectedFilterCustomerId.isEmpty()) query += "&customerId=" + selectedFilterCustomerId;
                if (!selectedFilterProductId.isEmpty()) query += "&productId=" + selectedFilterProductId;
                if (!selectedFilterStatus.equals("all")) query += "&status=" + selectedFilterStatus;

                JSONObject res = Api.request(this, "GET", "/api/sales" + query, null);
                JSONArray sales = res.optJSONArray("sales");

                runOnUiThread(() -> {
                    resultsContainer.removeAllViews();

                    if (sales == null || sales.length() == 0) {
                        resultsContainer.addView(createTextView("No purchase records match the selected filters.", 13, false, Color.parseColor("#94A3B8")));
                        return;
                    }

                    double totalBilled = 0, totalPaid = 0;
                    for (int i = 0; i < sales.length(); i++) {
                        JSONObject s = sales.optJSONObject(i);
                        if (s != null) {
                            totalBilled += s.optDouble("total", 0);
                            totalPaid += s.optDouble("paidAmount", 0);
                        }
                    }
                    double totalDue = Math.max(0, totalBilled - totalPaid);

                    TextView summaryHeader = createTextView("Filtered Results: " + sales.length() + " Sales | Total: ₹" + String.format(Locale.US, "%.0f", totalBilled) + " | Due: ₹" + String.format(Locale.US, "%.0f", totalDue), 13, true, Color.parseColor("#38BDF8"));
                    summaryHeader.setPadding(0, 0, 0, dp(10));
                    resultsContainer.addView(summaryHeader);

                    for (int i = 0; i < sales.length(); i++) {
                        JSONObject s = sales.optJSONObject(i);
                        if (s == null) continue;

                        JSONObject cust = s.optJSONObject("customerId");
                        String cName = cust != null ? cust.optString("name", "Customer") : "Walk-in";
                        String cMobile = cust != null ? cust.optString("mobile", "") : "";

                        double tot = s.optDouble("total", 0);
                        double pd = s.optDouble("paidAmount", 0);
                        double due = Math.max(0, tot - pd);

                        LinearLayout itemBox = new LinearLayout(this);
                        itemBox.setOrientation(LinearLayout.VERTICAL);
                        itemBox.setPadding(dp(10), dp(10), dp(10), dp(10));
                        itemBox.setBackgroundResource(R.drawable.bg_card);
                        LinearLayout.LayoutParams ibp = new LinearLayout.LayoutParams(-1, -2);
                        ibp.bottomMargin = dp(8);

                        itemBox.addView(createTextView(cName + " (" + cMobile + ")", 14, true, Color.parseColor("#1E293B")));
                        itemBox.addView(createTextView("Invoice #" + s.optString("invoiceNo") + " • Total: ₹" + String.format(Locale.US, "%.0f", tot), 13, false, Color.parseColor("#64748B")));
                        itemBox.addView(createTextView("Paid: ₹" + String.format(Locale.US, "%.0f", pd) + " | Remaining Due: ₹" + String.format(Locale.US, "%.0f", due), 13, true, due > 0 ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A")));

                        resultsContainer.addView(itemBox, ibp);
                    }

                    final double finalBilled = totalBilled;
                    final double finalDue = totalDue;

                    Button pdfBtn = createStyledButton("📄 Download Filtered Analytics PDF", Color.parseColor("#0284C7"), () -> {
                        try {
                            JSONObject repData = new JSONObject();
                            repData.put("sales", sales);
                            JSONObject totals = new JSONObject();
                            totals.put("salesRevenue", finalBilled);
                            totals.put("totalOutstandingCredit", finalDue);
                            repData.put("totals", totals);

                            File pdf = PdfGenerator.generateReportPdf(this, repData, selectedDatePeriod.toUpperCase() + " Filtered Analytics");
                            promptSharePdf(pdf);
                        } catch (Exception ex) {
                            Toast.makeText(this, "PDF Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    LinearLayout.LayoutParams pbp = new LinearLayout.LayoutParams(-1, dp(42));
                    pbp.topMargin = dp(10);
                    resultsContainer.addView(pdfBtn, pbp);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    resultsContainer.removeAllViews();
                    resultsContainer.addView(createTextView("Error loading results: " + e.getMessage(), 13, false, Color.parseColor("#DC2626")));
                });
            }
        }).start();
    }

    private void showFilteredPurchasesListDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(Color.parseColor("#FFF9F2"));

        box.addView(createTextView(currentLang.equals("mr") ? "निवडलेल्या खरेदीची यादी" : "Filtered Purchases List", 18, true, Color.parseColor("#1E293B")));

        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(container);

        container.addView(createTextView("Loading filtered list...", 13, false, Color.parseColor("#64748B")));

        box.addView(scroll, new LinearLayout.LayoutParams(-1, dp(400)));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(box)
            .setPositiveButton("Close", null)
            .show();

        new Thread(() -> {
            try {
                String query = "?period=" + selectedDatePeriod;
                if (!selectedFilterCustomerId.isEmpty()) query += "&customerId=" + selectedFilterCustomerId;
                if (!selectedFilterProductId.isEmpty()) query += "&productId=" + selectedFilterProductId;
                if (!selectedFilterStatus.equals("all")) query += "&status=" + selectedFilterStatus;

                JSONObject res = Api.request(this, "GET", "/api/sales" + query, null);
                JSONArray sales = res.optJSONArray("sales");

                runOnUiThread(() -> {
                    container.removeAllViews();

                    if (sales == null || sales.length() == 0) {
                        container.addView(createTextView("No purchase records match the selected filters.", 14, false, Color.parseColor("#64748B")));
                        return;
                    }

                    double totalBilled = 0, totalPaid = 0, totalCogs = 0;
                    for (int i = 0; i < sales.length(); i++) {
                        JSONObject s = sales.optJSONObject(i);
                        if (s != null) {
                            totalBilled += s.optDouble("total", 0);
                            totalPaid += s.optDouble("paidAmount", 0);
                            totalCogs += s.optDouble("costOfGoods", 0);
                        }
                    }
                    double totalDue = Math.max(0, totalBilled - totalPaid);
                    double grossProfit = Math.max(0, totalBilled - totalCogs);

                    double stockVal = 0;
                    try {
                        JSONObject pRes = Api.request(this, "GET", "/api/products", null);
                        JSONArray prods = pRes.optJSONArray("products");
                        if (prods != null) {
                            for (int k = 0; k < prods.length(); k++) {
                                JSONObject p = prods.optJSONObject(k);
                                if (p != null) {
                                    stockVal += (p.optDouble("stock", 0) * p.optDouble("purchasePrice", 0));
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    // Top summary box
                    TextView summaryTv = createTextView(
                        "Filtered Total: ₹" + String.format(Locale.US, "%.0f", totalBilled) + 
                        " | Paid: ₹" + String.format(Locale.US, "%.0f", totalPaid) + 
                        " | Due: ₹" + String.format(Locale.US, "%.0f", totalDue),
                        14, true, Color.parseColor("#E85D04")
                    );
                    summaryTv.setBackgroundResource(R.drawable.bg_card);
                    summaryTv.setPadding(dp(12), dp(10), dp(12), dp(10));
                    container.addView(summaryTv);

                    // PDF Option Buttons (View PDF and Download / Share PDF)
                    final double finalBilled = totalBilled;
                    final double finalPaid = totalPaid;
                    final double finalDue = totalDue;
                    final double finalCogs = totalCogs;
                    final double finalGrossProfit = grossProfit;
                    final double finalStockVal = stockVal;

                    LinearLayout pdfBtnRow = new LinearLayout(this);
                    pdfBtnRow.setOrientation(LinearLayout.HORIZONTAL);
                    pdfBtnRow.setPadding(0, dp(10), 0, dp(10));

                    Button viewPdfBtn = createStyledButton("👁 View PDF", Color.parseColor("#0284C7"), () -> {
                        try {
                            JSONObject repData = new JSONObject();
                            repData.put("sales", sales);
                            JSONObject totals = new JSONObject();
                            totals.put("salesRevenue", finalBilled);
                            totals.put("costOfGoodsSold", finalCogs);
                            totals.put("grossProfit", finalGrossProfit);
                            totals.put("paymentsReceived", finalPaid);
                            totals.put("totalOutstandingCredit", finalDue);
                            totals.put("stockValue", finalStockVal);
                            totals.put("netProfit", finalGrossProfit);
                            repData.put("totals", totals);

                            File pdf = PdfGenerator.generateReportPdf(this, repData, selectedDatePeriod.toUpperCase() + " Filtered Analytics");
                            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdf);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, "application/pdf");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(intent, "Open PDF"));
                        } catch (Exception ex) {
                            Toast.makeText(this, "PDF Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    Button sharePdfBtn = createStyledButton("📥 Download / Share PDF", Color.parseColor("#16A34A"), () -> {
                        try {
                            JSONObject repData = new JSONObject();
                            repData.put("sales", sales);
                            JSONObject totals = new JSONObject();
                            totals.put("salesRevenue", finalBilled);
                            totals.put("costOfGoodsSold", finalCogs);
                            totals.put("grossProfit", finalGrossProfit);
                            totals.put("paymentsReceived", finalPaid);
                            totals.put("totalOutstandingCredit", finalDue);
                            totals.put("stockValue", finalStockVal);
                            totals.put("netProfit", finalGrossProfit);
                            repData.put("totals", totals);

                            File pdf = PdfGenerator.generateReportPdf(this, repData, selectedDatePeriod.toUpperCase() + " Filtered Analytics");
                            promptSharePdf(pdf);
                        } catch (Exception ex) {
                            Toast.makeText(this, "PDF Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    pdfBtnRow.addView(viewPdfBtn, new LinearLayout.LayoutParams(0, dp(40), 1.0f));
                    View sp = new View(this); pdfBtnRow.addView(sp, new LinearLayout.LayoutParams(dp(8), 1));
                    pdfBtnRow.addView(sharePdfBtn, new LinearLayout.LayoutParams(0, dp(40), 1.0f));

                    container.addView(pdfBtnRow);

                    // Sales cards list
                    for (int i = 0; i < sales.length(); i++) {
                        JSONObject s = sales.optJSONObject(i);
                        if (s == null) continue;

                        JSONObject cust = s.optJSONObject("customerId");
                        String cName = cust != null ? cust.optString("name", "Customer") : "Walk-in";
                        String cMobile = cust != null ? cust.optString("mobile", "") : "";

                        double tot = s.optDouble("total", 0);
                        double pd = s.optDouble("paidAmount", 0);
                        double due = Math.max(0, tot - pd);

                        String date = s.optString("createdAt", "");
                        if (date.length() >= 10) date = date.substring(0, 10);

                        LinearLayout itemBox = new LinearLayout(this);
                        itemBox.setOrientation(LinearLayout.VERTICAL);
                        itemBox.setPadding(dp(12), dp(10), dp(12), dp(10));
                        itemBox.setBackgroundResource(R.drawable.bg_card);
                        LinearLayout.LayoutParams ibp = new LinearLayout.LayoutParams(-1, -2);
                        ibp.bottomMargin = dp(10);

                        itemBox.addView(createTextView(cName + " (" + cMobile + ")", 15, true, Color.parseColor("#1E293B")));
                        itemBox.addView(createTextView("Invoice #" + s.optString("invoiceNo") + " • Date: " + date, 13, false, Color.parseColor("#64748B")));

                        // Itemized products list
                        JSONArray items = s.optJSONArray("items");
                        if (items != null) {
                            for (int j = 0; j < items.length(); j++) {
                                JSONObject it = items.optJSONObject(j);
                                if (it == null) continue;
                                String pName = it.optString("nameMr", it.optString("nameEn", "Product"));
                                double qty = it.optDouble("quantity", 0);
                                double lineTot = it.optDouble("lineTotal", 0);
                                itemBox.addView(createTextView(" • " + pName + " x " + (int)qty + " = ₹" + String.format(Locale.US, "%.0f", lineTot), 12, false, Color.parseColor("#1E293B")));
                            }
                        }

                        itemBox.addView(createTextView("Total: ₹" + String.format(Locale.US, "%.0f", tot) + " | Paid: ₹" + String.format(Locale.US, "%.0f", pd) + " | Due: ₹" + String.format(Locale.US, "%.0f", due), 13, true, due > 0 ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A")));

                        if ("owner".equalsIgnoreCase(userRole)) {
                            String saleId = s.optString("_id");
                            Button editBillBtn = createStyledButton("✏️ Edit Bill / बिल दुरुस्त करा", Color.parseColor("#E85D04"), () -> {
                                if (dialog != null) dialog.dismiss();
                                showEditBillDialog(saleId, this::showFilteredPurchasesListDialog);
                            });
                            LinearLayout.LayoutParams ebp = new LinearLayout.LayoutParams(-1, dp(38));
                            ebp.topMargin = dp(6);
                            itemBox.addView(editBillBtn, ebp);
                        }

                        container.addView(itemBox, ibp);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    container.removeAllViews();
                    container.addView(createTextView("Error loading list: " + e.getMessage(), 13, false, Color.parseColor("#DC2626")));
                });
            }
        }).start();
    }
    
    private void showCustomerStatementDialog(String customerId, String customerName) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        
        box.addView(createTextView("Loading Statement...", 14, false, Color.parseColor("#64748B")));
        
        AlertDialog d = new AlertDialog.Builder(this)
            .setTitle(customerName + "'s Ledger")
            .setView(box)
            .setPositiveButton("Close", null)
            .show();
            
        new Thread(() -> {
            try {
                JSONObject res = Api.request(this, "GET", "/api/customers/" + customerId + "/statement", null);
                JSONObject summary = res.optJSONObject("summary");
                JSONArray tx = res.optJSONArray("transactions");
                
                runOnUiThread(() -> {
                    box.removeAllViews();
                    
                    double billed = summary != null ? summary.optDouble("totalBilled", 0) : 0;
                    double paid = summary != null ? summary.optDouble("totalPaid", 0) : 0;
                    double bal = summary != null ? summary.optDouble("creditBalance", 0) : 0;
                    
                    box.addView(createTextView("Total Billed: ₹" + String.format(Locale.US, "%.0f", billed), 14, false, Color.parseColor("#1E293B")));
                    box.addView(createTextView("Total Paid: ₹" + String.format(Locale.US, "%.0f", paid), 14, false, Color.parseColor("#16A34A")));
                    box.addView(createTextView("Remaining Due: ₹" + String.format(Locale.US, "%.0f", bal), 16, true, Color.parseColor("#DC2626")));
                    
                    View div = new View(this); div.setBackgroundColor(Color.parseColor("#E2E8F0"));
                    box.addView(div, new LinearLayout.LayoutParams(-1, dp(1)));
                    ((LinearLayout.LayoutParams)div.getLayoutParams()).setMargins(0, dp(12), 0, dp(12));
                    
                    ScrollView scroll = new ScrollView(this);
                    LinearLayout list = new LinearLayout(this);
                    list.setOrientation(LinearLayout.VERTICAL);
                    scroll.addView(list);
                    box.addView(scroll, new LinearLayout.LayoutParams(-1, dp(300))); // bounded height
                    
                    if (tx != null) {
                        for (int i = 0; i < tx.length(); i++) {
                            JSONObject t = tx.optJSONObject(i);
                            if (t == null) continue;
                            
                            LinearLayout tCard = new LinearLayout(this);
                            tCard.setOrientation(LinearLayout.VERTICAL);
                            tCard.setPadding(0, dp(6), 0, dp(6));
                            
                            String type = t.optString("type");
                            String dt = t.optString("date", "");
                            if (dt.length() >= 10) dt = dt.substring(0, 10);
                            
                            LinearLayout row1 = new LinearLayout(this);
                            row1.setOrientation(LinearLayout.HORIZONTAL);
                            row1.addView(createTextView(dt + " - " + type, 12, true, Color.parseColor("#1E293B")), new LinearLayout.LayoutParams(0, -2, 1.0f));
                            
                            double deb = t.optDouble("debit", 0);
                            double cre = t.optDouble("credit", 0);
                            
                            LinearLayout amtRow = new LinearLayout(this);
                            amtRow.setOrientation(LinearLayout.HORIZONTAL);
                            if (deb > 0) amtRow.addView(createTextView("Billed: ₹" + String.format(Locale.US, "%.0f", deb) + "  ", 12, true, Color.parseColor("#DC2626")));
                            if (type.equals("SALE")) {
                                double billPaid = t.optDouble("totalPaidOnBill", cre);
                                double billDue = t.optDouble("netDue", Math.max(0, deb - billPaid));
                                amtRow.addView(createTextView("Paid: ₹" + String.format(Locale.US, "%.0f", billPaid), 12, true, Color.parseColor("#16A34A")));
                                if (billDue > 0) {
                                    amtRow.addView(createTextView(" (Due: ₹" + String.format(Locale.US, "%.0f", billDue) + ")", 12, true, Color.parseColor("#DC2626")));
                                } else {
                                    amtRow.addView(createTextView(" (Paid)", 12, true, Color.parseColor("#16A34A")));
                                }
                            } else {
                                if (cre > 0) amtRow.addView(createTextView("Paid: ₹" + String.format(Locale.US, "%.0f", cre), 12, true, Color.parseColor("#16A34A")));
                            }
                            row1.addView(amtRow);
                            
                            tCard.addView(row1);
                            tCard.addView(createTextView(t.optString("description", ""), 12, false, Color.parseColor("#64748B")));

                            if (type.equals("PAYMENT") && "owner".equalsIgnoreCase(userRole)) {
                                String payId = t.optString("paymentId");
                                double currentAmt = t.optDouble("credit", 0);
                                Button editPayBtn = new Button(this);
                                editPayBtn.setText("✏️ Edit Payment / सुधारणा करा");
                                editPayBtn.setAllCaps(false);
                                editPayBtn.setTextColor(Color.WHITE);
                                editPayBtn.setTextSize(13);
                                editPayBtn.setTypeface(null, Typeface.BOLD);
                                editPayBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7")));
                                editPayBtn.setOnClickListener(v -> showEditPaymentDialog(payId, currentAmt, customerId, customerName, d));

                                LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(38));
                                ep.topMargin = dp(6);
                                tCard.addView(editPayBtn, ep);
                            }

                            if (type.equals("SALE") && "owner".equalsIgnoreCase(userRole)) {
                                String saleId = t.optString("saleId");
                                double currentPaid = t.optDouble("totalPaidOnBill", t.optDouble("credit", 0));
                                double billTotal = t.optDouble("billTotal", t.optDouble("debit", 0));
                                if (!saleId.isEmpty()) {
                                    LinearLayout saleBtnRow = new LinearLayout(this);
                                    saleBtnRow.setOrientation(LinearLayout.HORIZONTAL);
                                    saleBtnRow.setPadding(0, dp(4), 0, 0);

                                    Button editFullBillBtn = createStyledButton("✏️ Edit Bill", Color.parseColor("#E85D04"), () -> {
                                        if (d != null) d.dismiss();
                                        showEditBillDialog(saleId, () -> showCustomerStatementDialog(customerId, customerName));
                                    });

                                    Button addPaymentBtn = createStyledButton("➕ Add Payment", Color.parseColor("#0284C7"), () -> {
                                        showEditBillPaidDialog(saleId, billTotal, currentPaid, customerId, customerName, d);
                                    });

                                    saleBtnRow.addView(editFullBillBtn, new LinearLayout.LayoutParams(0, dp(38), 1.0f));
                                    View sp3 = new View(this); saleBtnRow.addView(sp3, new LinearLayout.LayoutParams(dp(8), 1));
                                    saleBtnRow.addView(addPaymentBtn, new LinearLayout.LayoutParams(0, dp(38), 1.0f));

                                    tCard.addView(saleBtnRow);
                                }
                            }
                            
                            list.addView(tCard);
                            View tDiv = new View(this); tDiv.setBackgroundColor(Color.parseColor("#F1F5F9"));
                            list.addView(tDiv, new LinearLayout.LayoutParams(-1, dp(1)));
                        }
                    }
                    
                    Button btnPdf = createStyledButton("Download Statement PDF", Color.parseColor("#0284C7"), () -> {
                        try {
                            File pdf = PdfGenerator.generateCustomerStatementPdf(this, res);
                            promptSharePdf(pdf);
                        } catch (Exception ex) {
                            Toast.makeText(this, "PDF Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, dp(48));
                    plp.topMargin = dp(12);
                    box.addView(btnPdf, plp);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    box.removeAllViews();
                    box.addView(createTextView("Error: " + e.getMessage(), 14, false, Color.parseColor("#DC2626")));
                });
            }
        }).start();
    }

    private void loadPaymentsManagement() {
        activeTab = "payments";
        buildBottomNav();
        statusTextView.setText(currentLang.equals("mr") ? "पेमेंट आणि वसुली इतिहास" : "Payments & Collections History");

        mainContentContainer.removeAllViews();
        mainContentContainer.addView(createTextView("Payments Received", 22, true, Color.parseColor("#1E293B")));

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lcp = new LinearLayout.LayoutParams(-1, -2);
        lcp.topMargin = dp(12);
        mainContentContainer.addView(listContainer, lcp);

        new Thread(() -> {
            try {
                JSONObject r = Api.request(this, "GET", "/api/payments", null);
                JSONArray payments = r.getJSONArray("payments");

                runOnUiThread(() -> {
                    listContainer.removeAllViews();
                    if (payments.length() == 0) {
                        listContainer.addView(createTextView("No payments recorded yet.", 14, false, Color.parseColor("#64748B")));
                        return;
                    }

                    for (int i = 0; i < payments.length(); i++) {
                        try {
                            JSONObject p = payments.getJSONObject(i);
                            JSONObject cust = p.optJSONObject("customerId");
                            String custName = cust != null ? cust.optString("name", "Customer") : "Walk-in";
                            String custMobile = cust != null ? cust.optString("mobile", "") : "";
                            double amount = p.optDouble("amount", 0);
                            String method = p.optString("method", "cash");
                            String note = p.optString("note", "");
                            String date = p.optString("createdAt", "");
                            if (date.length() >= 10) date = date.substring(0, 10);
                            String payId = p.optString("_id");

                            LinearLayout card = new LinearLayout(this);
                            card.setOrientation(LinearLayout.VERTICAL);
                            card.setPadding(dp(14), dp(12), dp(14), dp(12));
                            card.setBackgroundResource(R.drawable.bg_card);
                            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                            cp.bottomMargin = dp(10);

                            card.addView(createTextView(custName + " (" + custMobile + ")", 16, true, Color.parseColor("#1E293B")));
                            card.addView(createTextView("Amount Received: ₹" + String.format(Locale.US, "%.2f", amount) + " (" + method.toUpperCase() + ")", 15, true, Color.parseColor("#16A34A")));
                            card.addView(createTextView("Date: " + date + (note.isEmpty() ? "" : " • Note: " + note), 13, false, Color.parseColor("#64748B")));

                            Button editBtn = createStyledButton("✏️ Edit Payment / सुधारणा करा", Color.parseColor("#0284C7"), () -> {
                                showEditPaymentDialog(payId, amount, cust != null ? cust.optString("_id") : "", custName, null);
                            });
                            LinearLayout.LayoutParams ebp = new LinearLayout.LayoutParams(-1, dp(38));
                            ebp.topMargin = dp(8);
                            card.addView(editBtn, ebp);

                            listContainer.addView(card, cp);
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    listContainer.removeAllViews();
                    listContainer.addView(createTextView("Error loading payments: " + e.getMessage(), 14, false, Color.parseColor("#DC2626")));
                });
            }
        }).start();
    }

    private void showEditPaymentDialog(String paymentId, double currentAmount, String customerId, String customerName, AlertDialog parentDialog) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(12));

        EditText amountInput = createFormInput("New Payment Amount (₹)", String.valueOf(currentAmount), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText reasonInput = createFormInput("Correction Reason (e.g. entered wrong amount)", "Correction", InputType.TYPE_CLASS_TEXT);

        box.addView(createTextView("New Payment Amount (₹):", 12, false, Color.parseColor("#64748B")));
        box.addView(amountInput);
        box.addView(createTextView("Reason for Correction:", 12, false, Color.parseColor("#64748B")));
        box.addView(reasonInput);

        new AlertDialog.Builder(this)
            .setTitle("Edit Payment / पेमेंट सुधारणा")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save Changes", (dlg, w) -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("amount", Double.parseDouble(amountInput.getText().toString().trim()));
                    body.put("reason", reasonInput.getText().toString().trim());

                    new Thread(() -> {
                        try {
                            Api.request(this, "PATCH", "/api/payments/" + paymentId, body);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Payment corrected successfully with audit trail!", Toast.LENGTH_LONG).show();
                                if (parentDialog != null) parentDialog.dismiss();
                                if (activeTab.equals("payments")) {
                                    loadPaymentsManagement();
                                } else if (!customerId.isEmpty()) {
                                    showCustomerStatementDialog(customerId, customerName);
                                }
                            });
                        } catch (Exception ex) {
                            runOnUiThread(() -> Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }).start();
                } catch (Exception ex) {
                    Toast.makeText(this, "Invalid amount.", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private void showEditBillPaidDialog(String saleId, double billTotal, double currentPaid, String customerId, String customerName, AlertDialog parentDialog) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        double remainingDue = Math.max(0, billTotal - currentPaid);

        box.addView(createTextView("Customer: " + customerName, 15, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));
        box.addView(createTextView("Bill Total Amount: ₹" + String.format(Locale.US, "%.0f", billTotal), 14, false, isDark ? Color.WHITE : Color.parseColor("#1E293B")));
        box.addView(createTextView("Total Paid So Far: ₹" + String.format(Locale.US, "%.0f", currentPaid), 14, false, Color.parseColor("#16A34A")));
        box.addView(createTextView("Current Remaining Due: ₹" + String.format(Locale.US, "%.0f", remainingDue), 16, true, Color.parseColor("#DC2626")));

        View div = new View(this); div.setBackgroundColor(isDark ? Color.parseColor("#334155") : Color.parseColor("#E2E8F0"));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, dp(1));
        dlp.setMargins(0, dp(10), 0, dp(10));
        box.addView(div, dlp);

        EditText newPaymentInput = createFormInput("Add Payment Amount Now (आज जमा रक्कम ₹)", String.valueOf(remainingDue > 0 ? remainingDue : 0), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText cashInput = createFormInput("Cash Amount (₹)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText upiInput = createFormInput("UPI Amount (₹)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText upiRefInput = createFormInput("UPI Reference No (Optional)", "", InputType.TYPE_CLASS_TEXT);
        EditText noteInput = createFormInput("Note / Payment Reference", "Payment towards bill", InputType.TYPE_CLASS_TEXT);

        LinearLayout mixedBox = new LinearLayout(this);
        mixedBox.setOrientation(LinearLayout.VERTICAL);
        mixedBox.setVisibility(View.GONE);
        mixedBox.addView(createTextView("Cash Amount (₹):", 12, false, Color.parseColor("#64748B")));
        mixedBox.addView(cashInput);
        mixedBox.addView(createTextView("UPI Amount (₹):", 12, false, Color.parseColor("#64748B")));
        mixedBox.addView(upiInput);
        mixedBox.addView(createTextView("UPI Reference No:", 12, false, Color.parseColor("#64748B")));
        mixedBox.addView(upiRefInput);

        String[] paymentMethods = {"Cash only", "Online / UPI only", "Cash + UPI (Mixed)"};
        TextView methodBtn = new TextView(this);
        methodBtn.setText("Payment Method: " + paymentMethods[0] + " ▾");
        methodBtn.setTextSize(14);
        methodBtn.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        methodBtn.setBackgroundResource(R.drawable.bg_card);
        methodBtn.setPadding(dp(12), dp(10), dp(12), dp(10));
        methodBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);

        final int[] methodWhich = new int[]{0};
        methodBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Select Payment Method")
                .setItems(paymentMethods, (d, w) -> {
                    methodWhich[0] = w;
                    methodBtn.setText("Payment Method: " + paymentMethods[w] + " ▾");
                    methodBtn.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));
                    if (w == 2) {
                        mixedBox.setVisibility(View.VISIBLE);
                        newPaymentInput.setVisibility(View.GONE);
                    } else {
                        mixedBox.setVisibility(View.GONE);
                        newPaymentInput.setVisibility(View.VISIBLE);
                    }
                }).show();
        });

        box.addView(createTextView("Payment Method:", 12, false, Color.parseColor("#64748B")));
        box.addView(methodBtn, new LinearLayout.LayoutParams(-1, dp(44)) {{ bottomMargin = dp(8); }});
        box.addView(newPaymentInput);
        box.addView(mixedBox);
        box.addView(createTextView("Note / Payment Reference:", 12, false, Color.parseColor("#64748B")));
        box.addView(noteInput);

        new AlertDialog.Builder(this)
            .setTitle("Record Payment / जमा रक्कम जोडा")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Record Payment", (dlg, w) -> {
                try {
                    double cashAmt = 0, upiAmt = 0, payAmt = 0;
                    String methodCode = "cash";
                    String uRef = "";

                    if (methodWhich[0] == 2) { // Mixed
                        String cStr = cashInput.getText().toString().trim();
                        String uStr = upiInput.getText().toString().trim();
                        cashAmt = cStr.isEmpty() ? 0 : Double.parseDouble(cStr);
                        upiAmt = uStr.isEmpty() ? 0 : Double.parseDouble(uStr);
                        uRef = upiRefInput.getText().toString().trim();
                        payAmt = cashAmt + upiAmt;
                        methodCode = "split";
                    } else {
                        String amtStr = newPaymentInput.getText().toString().trim();
                        payAmt = amtStr.isEmpty() ? 0 : Double.parseDouble(amtStr);
                        if (methodWhich[0] == 1) {
                            upiAmt = payAmt;
                            methodCode = "upi";
                        } else {
                            cashAmt = payAmt;
                            methodCode = "cash";
                        }
                    }

                    if (payAmt <= 0) {
                        Toast.makeText(this, "Please enter a valid payment amount.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONObject body = new JSONObject();
                    body.put("customerId", customerId);
                    body.put("saleId", saleId);
                    body.put("amount", payAmt);
                    body.put("cashAmount", cashAmt);
                    body.put("upiAmount", upiAmt);
                    body.put("upiRefNo", uRef);
                    body.put("method", methodCode);
                    body.put("note", noteInput.getText().toString().trim());

                    new Thread(() -> {
                        try {
                            Api.request(this, "POST", "/api/payments", body);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Payment recorded & balance updated!", Toast.LENGTH_LONG).show();
                                if (parentDialog != null) parentDialog.dismiss();
                                showCustomerStatementDialog(customerId, customerName);
                            });
                        } catch (Exception ex) {
                            runOnUiThread(() -> Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }).start();
                } catch (Exception ex) {
                    Toast.makeText(this, "Invalid payment amount.", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private static class EditCartItem {
        String productId;
        String nameMr;
        String nameEn;
        String unit;
        double quantity;
        double unitPrice;

        EditCartItem(String productId, String nameMr, String nameEn, String unit, double quantity, double unitPrice) {
            this.productId = productId;
            this.nameMr = nameMr;
            this.nameEn = nameEn;
            this.unit = unit;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        double lineTotal() {
            return Math.max(0, quantity * unitPrice);
        }
    }

    private void showEditBillDialog(String saleId, Runnable onComplete) {
        if (saleId == null || saleId.isEmpty()) return;

        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout rootBox = new LinearLayout(this);
        rootBox.setOrientation(LinearLayout.VERTICAL);
        rootBox.setPadding(dp(20), dp(16), dp(20), dp(16));
        rootBox.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        rootBox.addView(createTextView("Loading bill details for correction...", 14, false, Color.parseColor("#64748B")));

        AlertDialog progressDlg = new AlertDialog.Builder(this)
                .setTitle("Edit Bill / बिल दुरुस्त करा")
                .setView(rootBox)
                .setCancelable(true)
                .show();

        new Thread(() -> {
            try {
                JSONObject sRes = Api.request(this, "GET", "/api/sales/" + saleId, null);
                JSONObject pRes = Api.request(this, "GET", "/api/products", null);
                JSONObject cRes = Api.request(this, "GET", "/api/customers/ledger", null);

                JSONObject sale = sRes.optJSONObject("sale");
                JSONArray products = pRes != null ? pRes.optJSONArray("products") : new JSONArray();
                JSONArray ledger = cRes != null ? cRes.optJSONArray("ledger") : new JSONArray();

                if (sale == null) throw new Exception("Invoice record not found.");

                runOnUiThread(() -> {
                    progressDlg.dismiss();
                    renderEditBillForm(sale, products, ledger, onComplete);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    progressDlg.dismiss();
                    Toast.makeText(this, "Error loading invoice: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void renderEditBillForm(JSONObject sale, JSONArray products, JSONArray ledger, Runnable onComplete) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout mainBox = new LinearLayout(this);
        mainBox.setOrientation(LinearLayout.VERTICAL);
        mainBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        mainBox.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        String invoiceNo = sale.optString("invoiceNo", "ST-0000");
        mainBox.addView(createTextView("Correcting Invoice #" + invoiceNo, 16, true, Color.parseColor("#1E293B")));

        // 1. Customer Selection
        List<String> cNames = new ArrayList<>();
        List<String> cIds = new ArrayList<>();
        cNames.add("Walk-in Customer");
        cIds.add("");

        int selectedCustIdx = 0;
        JSONObject currentCustObj = sale.optJSONObject("customerId");
        String currentCustId = currentCustObj != null ? currentCustObj.optString("_id", currentCustObj.optString("id")) : sale.optString("customerId", "");

        try {
            for (int i = 0; i < ledger.length(); i++) {
                JSONObject c = ledger.getJSONObject(i);
                String cid = c.optString("id");
                String cLabel = c.optString("name") + " (" + c.optString("mobile") + ")";
                cNames.add(cLabel);
                cIds.add(cid);
                if (!currentCustId.isEmpty() && currentCustId.equals(cid)) {
                    selectedCustIdx = cNames.size() - 1;
                }
            }
        } catch (Exception ignored) {}

        final int[] custIdxHolder = new int[]{ selectedCustIdx };

        TextView custBtn = new TextView(this);
        custBtn.setText("Customer: " + cNames.get(custIdxHolder[0]) + " ▾");
        custBtn.setTextSize(14);
        custBtn.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        custBtn.setBackgroundResource(R.drawable.bg_card);
        custBtn.setPadding(dp(12), dp(10), dp(12), dp(10));
        custBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);

        custBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Reassign Customer / ग्राहक निवडा")
                .setItems(cNames.toArray(new String[0]), (dlg, which) -> {
                    custIdxHolder[0] = which;
                    custBtn.setText("Customer: " + cNames.get(which) + " ▾");
                    custBtn.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));
                })
                .show();
        });

        mainBox.addView(createTextView("Customer Assignment:", 12, true, Color.parseColor("#64748B")));
        LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(-1, dp(44));
        cblp.bottomMargin = dp(10);
        mainBox.addView(custBtn, cblp);

        // 2. Product Items List Setup
        List<EditCartItem> editCart = new ArrayList<>();
        JSONArray origItems = sale.optJSONArray("items");
        if (origItems != null) {
            for (int i = 0; i < origItems.length(); i++) {
                JSONObject it = origItems.optJSONObject(i);
                if (it != null) {
                    editCart.add(new EditCartItem(
                        it.optString("productId"),
                        it.optString("nameMr", "Product"),
                        it.optString("nameEn", "Product"),
                        it.optString("unit", "bag"),
                        it.optDouble("quantity", 1),
                        it.optDouble("unitPrice", 0)
                    ));
                }
            }
        }

        // Product Catalog mapping
        List<String> prodNames = new ArrayList<>();
        List<JSONObject> prodObjs = new ArrayList<>();
        try {
            for (int i = 0; i < products.length(); i++) {
                JSONObject p = products.getJSONObject(i);
                if (p.optBoolean("active", true)) {
                    prodNames.add(p.optString("nameMr") + " — ₹" + p.optDouble("sellingPrice") + "/" + p.optString("unit"));
                    prodObjs.add(p);
                }
            }
        } catch (Exception ignored) {}

        LinearLayout itemsContainer = new LinearLayout(this);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        mainBox.addView(itemsContainer);

        Button addProdBtn = createStyledButton("➕ Add Missing Product Line", Color.parseColor("#0284C7"), null);
        LinearLayout.LayoutParams apblp = new LinearLayout.LayoutParams(-1, dp(40));
        apblp.topMargin = dp(6);
        apblp.bottomMargin = dp(10);
        mainBox.addView(addProdBtn, apblp);

        // 3. Totals & Payment Fields
        EditText discountInput = createFormInput("Discount (₹)", String.valueOf((int)sale.optDouble("discount", 0)), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText paidInput = createFormInput("Paid / Received Amount (₹)", String.valueOf((int)sale.optDouble("paidAmount", 0)), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText notesInput = createFormInput("Reason / Correction Remarks", sale.optString("notes", "Invoice quantity/price correction"), InputType.TYPE_CLASS_TEXT);

        double origCash = sale.optDouble("cashAmount", 0);
        double origUpi = sale.optDouble("upiAmount", 0);
        String origUpiRef = sale.optString("upiRefNo", "");

        EditText cashInput = createFormInput("Cash Amount (₹)", String.valueOf((int)origCash), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText upiInput = createFormInput("UPI Amount (₹)", String.valueOf((int)origUpi), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText upiRefInput = createFormInput("UPI Reference No (Optional)", origUpiRef, InputType.TYPE_CLASS_TEXT);

        LinearLayout mixedBox = new LinearLayout(this);
        mixedBox.setOrientation(LinearLayout.VERTICAL);
        mixedBox.setVisibility(origCash > 0 && origUpi > 0 ? View.VISIBLE : View.GONE);
        mixedBox.addView(createTextView("Cash Component (₹):", 12, false, Color.parseColor("#64748B")));
        mixedBox.addView(cashInput);
        mixedBox.addView(createTextView("UPI Component (₹):", 12, false, Color.parseColor("#64748B")));
        mixedBox.addView(upiInput);
        mixedBox.addView(createTextView("UPI Reference No:", 12, false, Color.parseColor("#64748B")));
        mixedBox.addView(upiRefInput);

        String[] paymentMethods = {"Cash only", "Online / UPI only", "Cash + UPI (Mixed)", "Credit (Unpaid)"};
        final String[] selectedPayMethod = new String[]{ sale.optString("paymentMethod", "cash") };
        
        int currentPayIdx = 0;
        if ("upi".equalsIgnoreCase(selectedPayMethod[0])) currentPayIdx = 1;
        else if ("split".equalsIgnoreCase(selectedPayMethod[0]) || (origCash > 0 && origUpi > 0)) currentPayIdx = 2;
        else if ("credit".equalsIgnoreCase(selectedPayMethod[0])) currentPayIdx = 3;

        final int[] payIdxHolder = new int[]{ currentPayIdx };

        TextView payMethodBtn = new TextView(this);
        payMethodBtn.setText("Payment Method: " + paymentMethods[currentPayIdx] + " ▾");
        payMethodBtn.setTextSize(14);
        payMethodBtn.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        payMethodBtn.setBackgroundResource(R.drawable.bg_card);
        payMethodBtn.setPadding(dp(12), dp(10), dp(12), dp(10));
        payMethodBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);

        payMethodBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Select Payment Method")
                .setItems(paymentMethods, (dlg, which) -> {
                    payIdxHolder[0] = which;
                    payMethodBtn.setText("Payment Method: " + paymentMethods[which] + " ▾");
                    payMethodBtn.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));
                    if (which == 2) {
                        mixedBox.setVisibility(View.VISIBLE);
                        paidInput.setVisibility(View.GONE);
                    } else {
                        mixedBox.setVisibility(View.GONE);
                        paidInput.setVisibility(View.VISIBLE);
                    }
                })
                .show();
        });

        TextView subtotalTv = createTextView("Subtotal: ₹0", 14, true, Color.parseColor("#1E293B"));
        TextView totalTv = createTextView("Corrected Invoice Total: ₹0", 16, true, Color.parseColor("#1E293B"));
        TextView remainingTv = createTextView("Remaining Balance Due: ₹0", 16, true, Color.parseColor("#DC2626"));

        mainBox.addView(subtotalTv);
        mainBox.addView(createTextView("Discount (₹):", 12, false, Color.parseColor("#64748B")));
        mainBox.addView(discountInput);
        mainBox.addView(totalTv);
        mainBox.addView(createTextView("Payment Method:", 12, false, Color.parseColor("#64748B")));
        mainBox.addView(payMethodBtn, new LinearLayout.LayoutParams(-1, dp(44)) {{ bottomMargin = dp(8); }});
        mainBox.addView(createTextView("Received Amount Now (₹):", 12, false, Color.parseColor("#64748B")));
        mainBox.addView(paidInput);
        mainBox.addView(mixedBox);
        mainBox.addView(remainingTv);
        mainBox.addView(createTextView("Correction Reason / Notes:", 12, false, Color.parseColor("#64748B")));
        mainBox.addView(notesInput);

        // Recalculation Listener
        Runnable refreshEditCartUI = new Runnable() {
            @Override
            public void run() {
                itemsContainer.removeAllViews();
                double subtotal = 0;

                for (int i = 0; i < editCart.size(); i++) {
                    final int idx = i;
                    EditCartItem item = editCart.get(i);
                    subtotal += item.lineTotal();

                    LinearLayout rowCard = new LinearLayout(DashboardActivity.this);
                    rowCard.setOrientation(LinearLayout.VERTICAL);
                    rowCard.setPadding(dp(10), dp(8), dp(10), dp(8));
                    rowCard.setBackgroundResource(R.drawable.bg_card);
                    LinearLayout.LayoutParams rclp = new LinearLayout.LayoutParams(-1, -2);
                    rclp.bottomMargin = dp(8);

                    LinearLayout topRow = new LinearLayout(DashboardActivity.this);
                    topRow.setOrientation(LinearLayout.HORIZONTAL);
                    topRow.setGravity(Gravity.CENTER_VERTICAL);
                    topRow.addView(createTextView(item.nameMr + " (" + item.nameEn + ")", 13, true, Color.parseColor("#1E293B")), new LinearLayout.LayoutParams(0, -2, 1.0f));

                    Button delBtn = createStyledButton("🗑️", Color.parseColor("#DC2626"), () -> {
                        editCart.remove(idx);
                        run();
                    });
                    topRow.addView(delBtn, new LinearLayout.LayoutParams(dp(36), dp(36)));
                    rowCard.addView(topRow);

                    LinearLayout ctrlRow = new LinearLayout(DashboardActivity.this);
                    ctrlRow.setOrientation(LinearLayout.HORIZONTAL);
                    ctrlRow.setGravity(Gravity.CENTER_VERTICAL);
                    ctrlRow.setPadding(0, dp(4), 0, 0);

                    Button minusBtn = createStyledButton("-", Color.parseColor("#94A3B8"), () -> {
                        if (editCart.get(idx).quantity > 1) {
                            editCart.get(idx).quantity--;
                            run();
                        }
                    });
                    ctrlRow.addView(minusBtn, new LinearLayout.LayoutParams(dp(36), dp(36)));

                    TextView qtyTv = createTextView(" " + (int)item.quantity + " " + item.unit + " ", 13, true, Color.parseColor("#1E293B"));
                    ctrlRow.addView(qtyTv);

                    Button plusBtn = createStyledButton("+", Color.parseColor("#94A3B8"), () -> {
                        editCart.get(idx).quantity++;
                        run();
                    });
                    ctrlRow.addView(plusBtn, new LinearLayout.LayoutParams(dp(36), dp(36)));

                    TextView priceLabel = createTextView("  @ ₹", 13, false, Color.parseColor("#64748B"));
                    ctrlRow.addView(priceLabel);

                    EditText priceEdit = new EditText(DashboardActivity.this);
                    priceEdit.setText(String.valueOf((int)item.unitPrice));
                    priceEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    priceEdit.setTextSize(13);
                    priceEdit.setTextColor(isDark ? Color.WHITE : Color.BLACK);
                    priceEdit.setBackgroundResource(R.drawable.bg_card);
                    priceEdit.setPadding(dp(6), dp(4), dp(6), dp(4));
                    priceEdit.setLayoutParams(new LinearLayout.LayoutParams(dp(70), dp(36)));
                    
                    priceEdit.addTextChangedListener(new TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                            try {
                                String pStr = s.toString().trim();
                                if (!pStr.isEmpty()) {
                                    editCart.get(idx).unitPrice = Double.parseDouble(pStr);
                                    double newSub = 0;
                                    for (EditCartItem eci : editCart) newSub += eci.lineTotal();
                                    double disc = discountInput.getText().toString().trim().isEmpty() ? 0 : Double.parseDouble(discountInput.getText().toString().trim());
                                    double tot = Math.max(0, newSub - disc);
                                    double pd = paidInput.getText().toString().trim().isEmpty() ? 0 : Double.parseDouble(paidInput.getText().toString().trim());
                                    double due = Math.max(0, tot - pd);

                                    subtotalTv.setText("Subtotal: ₹" + String.format(Locale.US, "%.0f", newSub));
                                    totalTv.setText("Corrected Invoice Total: ₹" + String.format(Locale.US, "%.0f", tot));
                                    remainingTv.setText("Remaining Balance Due: ₹" + String.format(Locale.US, "%.0f", due));
                                }
                            } catch (Exception ignored) {}
                        }
                        @Override public void afterTextChanged(Editable s) {}
                    });
                    ctrlRow.addView(priceEdit);

                    TextView lineTotTv = createTextView(" = ₹" + String.format(Locale.US, "%.0f", item.lineTotal()), 13, true, Color.parseColor("#16A34A"));
                    ctrlRow.addView(lineTotTv);

                    rowCard.addView(ctrlRow);
                    itemsContainer.addView(rowCard, rclp);
                }

                double disc = discountInput.getText().toString().trim().isEmpty() ? 0 : Double.parseDouble(discountInput.getText().toString().trim());
                double tot = Math.max(0, subtotal - disc);
                double pd = paidInput.getText().toString().trim().isEmpty() ? 0 : Double.parseDouble(paidInput.getText().toString().trim());
                double due = Math.max(0, tot - pd);

                subtotalTv.setText("Subtotal: ₹" + String.format(Locale.US, "%.0f", subtotal));
                totalTv.setText("Corrected Invoice Total: ₹" + String.format(Locale.US, "%.0f", tot));
                remainingTv.setText("Remaining Balance Due: ₹" + String.format(Locale.US, "%.0f", due));
            }
        };

        addProdBtn.setOnClickListener(v -> {
            if (prodObjs.isEmpty()) return;
            new AlertDialog.Builder(this)
                .setTitle("Select Product to Add")
                .setItems(prodNames.toArray(new String[0]), (dlg, which) -> {
                    JSONObject p = prodObjs.get(which);
                    editCart.add(new EditCartItem(
                        p.optString("_id"),
                        p.optString("nameMr"),
                        p.optString("nameEn"),
                        p.optString("unit", "bag"),
                        1,
                        p.optDouble("sellingPrice", 0)
                    ));
                    refreshEditCartUI.run();
                })
                .show();
        });

        TextWatcher recalcTw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshEditCartUI.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        discountInput.addTextChangedListener(recalcTw);
        paidInput.addTextChangedListener(recalcTw);

        refreshEditCartUI.run();

        ScrollView scroll = new ScrollView(this);
        scroll.addView(mainBox);

        AlertDialog formDialog = new AlertDialog.Builder(this)
            .setTitle("Edit Invoice #" + invoiceNo)
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save Bill Corrections", null)
            .show();

        formDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (editCart.isEmpty()) {
                Toast.makeText(this, "Bill must contain at least one item.", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                .setTitle("Confirm Bill Correction / बदल जतन करा")
                .setMessage("Are you sure you want to save corrections for Invoice #" + invoiceNo + "?\n\nStock levels and customer ledger balances will be updated immediately in MongoDB Atlas.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm & Save", (dlg, w) -> {
                    formDialog.dismiss();

                    try {
                        JSONArray newItems = new JSONArray();
                        for (EditCartItem eci : editCart) {
                            JSONObject it = new JSONObject();
                            it.put("productId", eci.productId);
                            it.put("quantity", eci.quantity);
                            it.put("unitPrice", eci.unitPrice);
                            newItems.put(it);
                        }

                        double disc = discountInput.getText().toString().trim().isEmpty() ? 0 : Double.parseDouble(discountInput.getText().toString().trim());
                        
                        double cashAmt = 0;
                        double upiAmt = 0;
                        double paid = 0;
                        String methodCode = "cash";
                        String uRef = "";

                        int pWhich = payIdxHolder[0];
                        if (pWhich == 2) { // Mixed Cash + UPI
                            String cStr = cashInput.getText().toString().trim();
                            String uStr = upiInput.getText().toString().trim();
                            cashAmt = cStr.isEmpty() ? 0 : Double.parseDouble(cStr);
                            upiAmt = uStr.isEmpty() ? 0 : Double.parseDouble(uStr);
                            uRef = upiRefInput.getText().toString().trim();
                            paid = cashAmt + upiAmt;
                            methodCode = "split";
                        } else if (pWhich == 1) { // Online / UPI
                            paid = paidInput.getText().toString().trim().isEmpty() ? 0 : Double.parseDouble(paidInput.getText().toString().trim());
                            upiAmt = paid;
                            methodCode = "upi";
                        } else if (pWhich == 3) { // Credit (Unpaid)
                            paid = 0;
                            methodCode = "credit";
                        } else { // Cash only
                            paid = paidInput.getText().toString().trim().isEmpty() ? 0 : Double.parseDouble(paidInput.getText().toString().trim());
                            cashAmt = paid;
                            methodCode = "cash";
                        }

                        JSONObject body = new JSONObject();
                        if (custIdxHolder[0] > 0) {
                            body.put("customerId", cIds.get(custIdxHolder[0]));
                        } else {
                            body.put("customerId", "");
                        }
                        body.put("items", newItems);
                        body.put("discount", disc);
                        body.put("cashAmount", cashAmt);
                        body.put("upiAmount", upiAmt);
                        body.put("upiRefNo", uRef);
                        body.put("paymentMethod", methodCode);
                        body.put("paidAmount", paid);
                        body.put("notes", notesInput.getText().toString().trim());

                        new Thread(() -> {
                            try {
                                JSONObject res = Api.request(this, "PUT", "/api/sales/" + sale.optString("_id"), body);
                                JSONObject updatedSale = res.optJSONObject("sale");

                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Invoice #" + invoiceNo + " corrected successfully!", Toast.LENGTH_LONG).show();
                                    try {
                                        if (updatedSale != null) {
                                            File pdf = PdfGenerator.generateInvoicePdf(this, updatedSale, cNames.get(custIdxHolder[0]));
                                            promptSharePdf(pdf);
                                        }
                                    } catch (Exception ignored) {}

                                    if (onComplete != null) onComplete.run();
                                    else loadOwnerDashboard();
                                });
                            } catch (Exception ex) {
                                runOnUiThread(() -> Toast.makeText(this, "Error updating bill: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        }).start();
                    } catch (Exception ex) {
                        Toast.makeText(this, "Invalid form values.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
        });
    }

    private void showAddCustomerDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(12));

        EditText name = createFormInput("Customer Name", "", InputType.TYPE_CLASS_TEXT);
        EditText mobile = createFormInput("Mobile Number", "", InputType.TYPE_CLASS_PHONE);
        EditText pass = createFormInput("Password", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        box.addView(createTextView("Customer Name:", 12, false, Color.parseColor("#64748B")));
        box.addView(name);
        box.addView(createTextView("Mobile Number:", 12, false, Color.parseColor("#64748B")));
        box.addView(mobile);
        box.addView(createTextView("Password:", 12, false, Color.parseColor("#64748B")));
        box.addView(pass);

        new AlertDialog.Builder(this)
                .setTitle("Add Customer")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        JSONObject body = new JSONObject();
                        body.put("name", name.getText().toString().trim());
                        body.put("mobile", mobile.getText().toString().trim());
                        body.put("password", pass.getText().toString().trim());

                        new Thread(() -> {
                            try {
                                Api.request(this, "POST", "/api/customers", body);
                                runOnUiThread(() -> loadCustomersAndCredit());
                            } catch (Exception e) {}
                        }).start();
                    } catch (Exception ex) {}
                }).show();
    }

    // ==========================================
    // REPORTS, SETTINGS & BILLS
    // ==========================================

    private void loadReportsView() {
        boolean isDark = LocaleHelper.isDarkMode(this);
        activeTab = "reports";
        buildBottomNav();
        statusTextView.setText(currentLang.equals("mr") ? "आर्थिक अहवाल आणि निर्यात" : "Financial Reports & Exports");

        mainContentContainer.removeAllViews();
        mainContentContainer.addView(createTextView(currentLang.equals("mr") ? "आर्थिक अहवाल" : "Financial Reports", 22, true, Color.parseColor("#1E293B")));
        
        // Date Filter
        String[] dateFiltersEn = {"Today", "This Week", "This Month", "All Time"};
        String[] dateFiltersMr = {"आज (Today)", "या आठवड्यात (This Week)", "या महिन्यात (This Month)", "सर्व वेळ (All Time)"};
        final String[] dateFilters = currentLang.equals("mr") ? dateFiltersMr : dateFiltersEn;
        
        TextView dateSpinner = new TextView(this);
        dateSpinner.setText(dateFilters[0] + " ▾");
        dateSpinner.setTextSize(15);
        dateSpinner.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        dateSpinner.setBackgroundResource(R.drawable.bg_card);
        dateSpinner.setPadding(dp(14), dp(11), dp(14), dp(11));
        dateSpinner.setGravity(Gravity.CENTER_VERTICAL);
        dateSpinner.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
        
        LinearLayout.LayoutParams dpLp = new LinearLayout.LayoutParams(-1, dp(46));
        dpLp.topMargin = dp(8);
        mainContentContainer.addView(dateSpinner, dpLp);

        // Fetch & Show Summary
        LinearLayout summaryCard = new LinearLayout(this);
        summaryCard.setOrientation(LinearLayout.VERTICAL);
        summaryCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        summaryCard.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(-1, -2);
        sclp.topMargin = dp(12);
        mainContentContainer.addView(summaryCard, sclp);
        
        Runnable fetchReports = () -> {
            summaryCard.removeAllViews();
            summaryCard.addView(createTextView(currentLang.equals("mr") ? "अहवाल डेटा लोड करत आहे..." : "Loading report data...", 14, false, Color.parseColor("#64748B")));
            
            new Thread(() -> {
                try {
                    String query = "";
                    
                    Object posTag = dateSpinner.getTag();
                    int pos = posTag instanceof Integer ? (Integer) posTag : 0;
                    
                    long now = System.currentTimeMillis();
                    if (pos == 0) { // Today
                        query = "?from=" + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(now));
                    } else if (pos == 1) { // This Week
                        query = "?from=" + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(now - 7L * 24 * 3600 * 1000));
                    } else if (pos == 2) { // This Month
                        query = "?from=" + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(now - 30L * 24 * 3600 * 1000));
                    }
                    
                    final String safeQuery = query;
                    final int finalPos = pos;
                    JSONObject r = Api.request(this, "GET", "/api/reports/summary" + safeQuery, null);
                    JSONObject t = r.getJSONObject("totals");
                    
                    runOnUiThread(() -> {
                        summaryCard.removeAllViews();
                        
                        String sr = currentLang.equals("mr") ? "विक्री उत्पन्न: ₹ " : "Sales Revenue: ₹ ";
                        summaryCard.addView(createTextView(sr + String.format(Locale.US, "%.2f", t.optDouble("salesRevenue", 0)), 16, true, Color.parseColor("#16A34A")));
                        
                        String cgs = currentLang.equals("mr") ? "माल खरेदीची किंमत (COGS): ₹ " : "Cost of Goods Sold (COGS): ₹ ";
                        summaryCard.addView(createTextView(cgs + String.format(Locale.US, "%.2f", t.optDouble("costOfGoodsSold", 0)), 14, false, Color.parseColor("#1E293B")));
                        
                        String gp = currentLang.equals("mr") ? "स्थूल नफा: ₹ " : "Gross Profit: ₹ ";
                        summaryCard.addView(createTextView(gp + String.format(Locale.US, "%.2f", t.optDouble("grossProfit", 0)), 16, true, Color.parseColor("#0284C7")));
                        
                        View div1 = new View(this); div1.setBackgroundColor(Color.parseColor("#E2E8F0"));
                        summaryCard.addView(div1, new LinearLayout.LayoutParams(-1, dp(1)));
                        ((LinearLayout.LayoutParams)div1.getLayoutParams()).setMargins(0, dp(8), 0, dp(8));
                        
                        String np = currentLang.equals("mr") ? "निव्वळ नफा: ₹ " : "Net Profit: ₹ ";
                        summaryCard.addView(createTextView(np + String.format(Locale.US, "%.2f", t.optDouble("grossProfit", t.optDouble("netProfit", 0))), 18, true, Color.parseColor("#E85D04")));
                        
                        View div2 = new View(this); div2.setBackgroundColor(Color.parseColor("#E2E8F0"));
                        summaryCard.addView(div2, new LinearLayout.LayoutParams(-1, dp(1)));
                        ((LinearLayout.LayoutParams)div2.getLayoutParams()).setMargins(0, dp(8), 0, dp(8));
                        
                        String pc = currentLang.equals("mr") ? "जमा रक्कम (Payments): ₹ " : "Payments Collected: ₹ ";
                        summaryCard.addView(createTextView(pc + String.format(Locale.US, "%.2f", t.optDouble("paymentsReceived", 0)), 14, false, Color.parseColor("#1E293B")));
                        
                        String sp = currentLang.equals("mr") ? "व्यापारी खरेदी (Purchases): ₹ " : "Supplier Purchases: ₹ ";
                        summaryCard.addView(createTextView(sp + String.format(Locale.US, "%.2f", t.optDouble("purchaseTotal", 0)), 14, false, Color.parseColor("#9333EA")));
                        
                        String toc = currentLang.equals("mr") ? "ग्राहकांची एकूण उधारी: ₹ " : "Total Customer Outstanding: ₹ ";
                        summaryCard.addView(createTextView(toc + String.format(Locale.US, "%.2f", t.optDouble("totalOutstandingCredit", 0)), 14, true, Color.parseColor("#DC2626")));
                        
                        String iv = currentLang.equals("mr") ? "शिल्लक साठ्याचे मूल्य: ₹ " : "Current Inventory Value: ₹ ";
                        summaryCard.addView(createTextView(iv + String.format(Locale.US, "%.2f", t.optDouble("stockValue", 0)), 14, false, Color.parseColor("#1E293B")));

                        // Section for Pending Collections (Unpaid Customers Button - ALWAYS VISIBLE)
                        JSONArray custBals = r.optJSONArray("customerBalances");
                        double totalPendingToCollect = t.optDouble("totalOutstandingCredit", 0);

                        final double finalPending = totalPendingToCollect;
                        Button btnUnpaid = createStyledButton(
                            currentLang.equals("mr") ? "थकबाकीदार ग्राहक तपशील पहा (Unpaid Customers)" : "View Unpaid Customers Details",
                            Color.parseColor("#EA580C"),
                            () -> showUnpaidCustomersDialog(finalPending)
                        );
                        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(-1, dp(48));
                        ulp.topMargin = dp(16);
                        summaryCard.addView(btnUnpaid, ulp);
                        
                        Button btnPdf = createStyledButton(currentLang.equals("mr") ? "अहवाल PDF डाउनलोड करा" : "Download PDF Report", Color.parseColor("#0284C7"), () -> {
                            try {
                                File pdf = PdfGenerator.generateReportPdf(this, r, dateFilters[finalPos]);
                                promptSharePdf(pdf);
                            } catch (Exception ex) {
                                Toast.makeText(this, "PDF Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, dp(48));
                        plp.topMargin = dp(16);
                        summaryCard.addView(btnPdf, plp);
                        
                        Button btnExcel = createStyledButton("Export Excel (XLSX)", Color.parseColor("#16A34A"), () -> downloadExcelReport(safeQuery));
                        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(-1, dp(48));
                        elp.topMargin = dp(8);
                        summaryCard.addView(btnExcel, elp);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        summaryCard.removeAllViews();
                        summaryCard.addView(createTextView("Failed to load reports: " + e.getMessage(), 14, false, Color.parseColor("#DC2626")));
                    });
                }
            }).start();
        };

        dateSpinner.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle(currentLang.equals("mr") ? "कालावधी निवडा" : "Select Report Period")
                .setItems(dateFilters, (dlg, which) -> {
                    dateSpinner.setText(dateFilters[which] + " ▾");
                    dateSpinner.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));
                    dateSpinner.setTag(which);
                    fetchReports.run();
                })
                .show();
        });
        
        // Initial Fetch
        fetchReports.run();
    }

    private void showUnpaidCustomersDialog(double totalPending) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(Color.parseColor("#FFF9F2"));

        TextView header = createTextView(
            (currentLang.equals("mr") ? "एकूण गोळा करायची रक्कम: " : "Total Remaining Money to Collect: ") + String.format(Locale.US, "₹ %,.2f", totalPending),
            16, true, Color.parseColor("#DC2626")
        );
        header.setBackgroundResource(R.drawable.bg_card);
        header.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.addView(header);

        View div = new View(this);
        div.setBackgroundColor(Color.parseColor("#E2E8F0"));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, dp(1));
        dlp.setMargins(0, dp(10), 0, dp(10));
        box.addView(div, dlp);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        box.addView(scroll, new LinearLayout.LayoutParams(-1, dp(380)));

        list.addView(createTextView(currentLang.equals("mr") ? "थकबाकीदार ग्राहक लोड होत आहेत..." : "Loading unpaid customers...", 14, false, Color.parseColor("#64748B")));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(currentLang.equals("mr") ? "थकबाकीदार ग्राहकांचा तपशील" : "Unpaid Customers Breakdown")
            .setView(box)
            .setPositiveButton(currentLang.equals("mr") ? "बंद करा (Close)" : "Close", null)
            .show();

        new Thread(() -> {
            try {
                // Fetch the default summary to get customer balances
                JSONObject r = Api.request(this, "GET", "/api/reports/summary", null);
                JSONArray custBals = r.optJSONArray("customerBalances");
                JSONArray ledger = sortJsonArrayByDouble(custBals, "balance", true);

                runOnUiThread(() -> {
                    list.removeAllViews();
                    int unpaidCount = 0;
                    try {
                        for (int i = 0; i < ledger.length(); i++) {
                            JSONObject c = ledger.optJSONObject(i);
                            if (c == null) continue;

                            double bal = c.optDouble("balance", 0);
                            if (bal <= 0) continue;

                            unpaidCount++;
                            LinearLayout card = new LinearLayout(this);
                            card.setOrientation(LinearLayout.VERTICAL);
                            card.setPadding(dp(14), dp(12), dp(14), dp(12));
                            card.setBackgroundResource(R.drawable.bg_card);
                            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                            cp.bottomMargin = dp(10);

                            String cName = c.optString("name", "Customer");
                            String cMobile = c.optString("mobile", "");
                            String cId = c.optString("id", "");

                            TextView name = createTextView(cName + " (" + cMobile + ")", 15, true, Color.parseColor("#1E293B"));
                            TextView billed = createTextView((currentLang.equals("mr") ? "एकूण बिल रक्कम: " : "Total Billed: ") + String.format(Locale.US, "₹ %,.2f", c.optDouble("billed", 0)), 13, false, Color.parseColor("#64748B"));
                            TextView paid = createTextView((currentLang.equals("mr") ? "जमा रक्कम: " : "Total Paid: ") + String.format(Locale.US, "₹ %,.2f", c.optDouble("paid", 0)), 13, false, Color.parseColor("#16A34A"));
                            TextView remaining = createTextView((currentLang.equals("mr") ? "बाकी रक्कम: " : "Remaining Balance: ") + String.format(Locale.US, "₹ %,.2f", bal), 15, true, Color.parseColor("#DC2626"));

                            Button viewDetailsBtn = createStyledButton(
                                currentLang.equals("mr") ? "तपशील पहा (View Details)" : "View Details",
                                Color.parseColor("#0284C7"),
                                () -> {
                                    dialog.dismiss();
                                    showCustomerStatementDialog(cId, cName);
                                }
                            );
                            LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-1, dp(38));
                            vlp.topMargin = dp(8);

                            card.addView(name);
                            card.addView(billed);
                            card.addView(paid);
                            card.addView(remaining);
                            card.addView(viewDetailsBtn, vlp);

                            list.addView(card, cp);
                        }
                    } catch (Exception ignored) {}

                    if (unpaidCount == 0) {
                        list.addView(createTextView(
                            currentLang.equals("mr") ? "कोणत्याही ग्राहकाची थकबाकी नाही." : "No unpaid customers found.",
                            14, false, Color.parseColor("#16A34A")
                        ));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    list.removeAllViews();
                    list.addView(createTextView("Failed to load: " + e.getMessage(), 14, false, Color.parseColor("#DC2626")));
                });
            }
        }).start();
    }

    private void downloadExcelReport(String queryStr) {
        Toast.makeText(this, "Downloading Excel report...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                URL url = new URL(Api.getBaseUrl(this) + "/api/reports/excel" + queryStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                String token = Api.token(this);
                if (!token.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.connect();

                int code = conn.getResponseCode();
                if (code == 200) {
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, "Shravan_Traders_Report_" + System.currentTimeMillis() + ".xlsx");

                    try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(file)) {
                        byte[] buf = new byte[4096];
                        int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }

                    runOnUiThread(() -> Toast.makeText(this, "Excel downloaded: " + file.getName(), Toast.LENGTH_LONG).show());
                }
                conn.disconnect();
            } catch (Exception e) {}
        }).start();
    }

    private void showStockAnalyticsDialog() {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        TextView title = createTextView(
            currentLang.equals("mr") ? "साठा व विक्री तपासणी (Stock & Sell/Unsell)" : "Stock & Sold/Unsold Analytics",
            18, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")
        );
        TextView subtitle = createTextView(
            currentLang.equals("mr") ? "उत्पादनांनुसार विक्री (Sell), शिल्लक साठा (Unsell) व कालावधी फिल्टर" : "Check product-wise sold, unsold stock & period analytics",
            12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")
        );
        subtitle.setPadding(0, dp(2), 0, dp(10));

        box.addView(title);
        box.addView(subtitle);

        // Period Selection Dropdown Button
        String[] periodLabelsEn = {"This Month", "Last Month", "Today / Daily", "This Week", "All Time"};
        String[] periodLabelsMr = {"या महिन्यात (This Month)", "मागील महिन्यात (Last Month)", "आज (Today)", "या आठवड्यात (This Week)", "सर्व वेळ (All Time)"};
        String[] periodKeys = {"this_month", "last_month", "today", "week", "all"};
        final String[] periodLabels = currentLang.equals("mr") ? periodLabelsMr : periodLabelsEn;

        final int[] selectedPeriodIdx = new int[]{0};

        TextView periodSpinner = new TextView(this);
        periodSpinner.setText("Period: " + periodLabels[0] + " ▾");
        periodSpinner.setTextSize(14);
        periodSpinner.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        periodSpinner.setBackgroundResource(R.drawable.bg_card);
        periodSpinner.setPadding(dp(12), dp(10), dp(12), dp(10));
        periodSpinner.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);

        LinearLayout.LayoutParams psp = new LinearLayout.LayoutParams(-1, dp(44));
        psp.bottomMargin = dp(10);
        box.addView(periodSpinner, psp);

        // Real-time Search Input
        EditText searchInput = createFormInput("Search product name...", "", InputType.TYPE_CLASS_TEXT);
        box.addView(searchInput);

        // Summary Bar Container
        LinearLayout summaryBar = new LinearLayout(this);
        summaryBar.setOrientation(LinearLayout.VERTICAL);
        summaryBar.setPadding(dp(12), dp(10), dp(12), dp(10));
        summaryBar.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams sbp = new LinearLayout.LayoutParams(-1, -2);
        sbp.bottomMargin = dp(10);
        box.addView(summaryBar, sbp);

        // Products List Container (Scrollable)
        ScrollView scroll = new ScrollView(this);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);

        listContainer.addView(createTextView("Loading stock & sales analytics...", 13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));

        box.addView(scroll, new LinearLayout.LayoutParams(-1, dp(380)));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(box)
            .setPositiveButton("Close", null)
            .show();

        // Runnable to fetch & render list
        Runnable loadStockData = () -> {
            listContainer.removeAllViews();
            listContainer.addView(createTextView("Loading analytics...", 13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));

            new Thread(() -> {
                try {
                    String periodKey = periodKeys[selectedPeriodIdx[0]];
                    String query = "?period=" + periodKey;
                    if (periodKey.equals("today")) query = "?period=today";
                    else if (periodKey.equals("this_month")) query = "?period=this_month";
                    else if (periodKey.equals("last_month")) query = "?period=last_month";
                    else if (periodKey.equals("all")) query = "";

                    JSONObject res = Api.request(this, "GET", "/api/reports/summary" + query, null);
                    JSONArray stockArr = res.optJSONArray("stock");

                    runOnUiThread(() -> {
                        listContainer.removeAllViews();
                        summaryBar.removeAllViews();

                        if (stockArr == null || stockArr.length() == 0) {
                            listContainer.addView(createTextView("No product stock data found.", 14, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
                            return;
                        }

                        int totalProductsCount = stockArr.length();
                        double totalSoldUnits = 0;
                        double totalUnsoldStockUnits = 0;
                        double totalStockValuation = 0;
                        double totalRevenue = 0;

                        List<JSONObject> stockList = new ArrayList<>();
                        for (int i = 0; i < stockArr.length(); i++) {
                            JSONObject item = stockArr.optJSONObject(i);
                            if (item != null) {
                                stockList.add(item);
                                totalSoldUnits += item.optDouble("quantitySold", 0);
                                totalUnsoldStockUnits += item.optDouble("stock", 0);
                                totalStockValuation += item.optDouble("stockValue", 0);
                                totalRevenue += item.optDouble("revenueGenerated", 0);
                            }
                        }

                        // Populate Top Summary Bar
                        summaryBar.addView(createTextView(
                            "Total Products: " + totalProductsCount + 
                            " | Sold (Sell): " + (int)totalSoldUnits + " units" +
                            " | Unsold (In Hand): " + (int)totalUnsoldStockUnits + " units",
                            13, true, Color.parseColor("#E85D04")
                        ));
                        summaryBar.addView(createTextView(
                            "Unsold Stock Valuation: ₹" + String.format(Locale.US, "%.0f", totalStockValuation) + 
                            " | Period Revenue: ₹" + String.format(Locale.US, "%.0f", totalRevenue),
                            12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")
                        ));

                        // Render Filterable Product Cards
                        Runnable renderFilteredCards = () -> {
                            listContainer.removeAllViews();
                            String filterText = searchInput.getText().toString().trim().toLowerCase();

                            int renderedCount = 0;
                            for (JSONObject p : stockList) {
                                String nameMr = p.optString("productMr", "");
                                String nameEn = p.optString("product", "");
                                String cat = p.optString("category", "कांडी");
                                String unit = p.optString("unit", "bag");

                                if (!filterText.isEmpty()) {
                                    if (!nameMr.toLowerCase().contains(filterText) && !nameEn.toLowerCase().contains(filterText) && !cat.toLowerCase().contains(filterText)) {
                                        continue;
                                    }
                                }

                                renderedCount++;
                                double sold = p.optDouble("quantitySold", 0);
                                double unsold = p.optDouble("stock", 0);
                                double price = p.optDouble("sellingPrice", 0);
                                double rev = p.optDouble("revenueGenerated", 0);
                                double stockVal = p.optDouble("stockValue", 0);
                                boolean isLow = p.optBoolean("lowStock", false);

                                LinearLayout card = new LinearLayout(this);
                                card.setOrientation(LinearLayout.VERTICAL);
                                card.setPadding(dp(12), dp(10), dp(12), dp(10));
                                card.setBackgroundResource(R.drawable.bg_card);
                                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                                cp.bottomMargin = dp(8);

                                // Title Row
                                LinearLayout titleRow = new LinearLayout(this);
                                titleRow.setOrientation(LinearLayout.HORIZONTAL);
                                titleRow.setGravity(Gravity.CENTER_VERTICAL);

                                titleRow.addView(
                                    createTextView(nameMr + " (" + nameEn + ")", 14, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")),
                                    new LinearLayout.LayoutParams(0, -2, 1.0f)
                                );

                                TextView badge = createTextView(isLow ? "⚠️ Low Stock" : "In Stock", 10, true, isLow ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A"));
                                badge.setBackgroundResource(isLow ? R.drawable.bg_badge_low_stock : R.drawable.bg_badge_in_stock);
                                badge.setPadding(dp(6), dp(2), dp(6), dp(2));
                                titleRow.addView(badge);

                                card.addView(titleRow);

                                // Metrics Row (Sold vs Unsold)
                                LinearLayout metricsRow = new LinearLayout(this);
                                metricsRow.setOrientation(LinearLayout.HORIZONTAL);
                                metricsRow.setPadding(0, dp(6), 0, dp(4));

                                TextView soldTv = createTextView("🛒 Sold (Sell): " + (int)sold + " " + unit, 13, true, Color.parseColor("#16A34A"));
                                TextView unsoldTv = createTextView("📦 Unsold (In Hand): " + (int)unsold + " " + unit, 13, true, Color.parseColor("#0284C7"));

                                metricsRow.addView(soldTv, new LinearLayout.LayoutParams(0, -2, 1.0f));
                                metricsRow.addView(unsoldTv, new LinearLayout.LayoutParams(0, -2, 1.0f));
                                card.addView(metricsRow);

                                // Details Line
                                String detailsStr = "Price: ₹" + (int)price + "/" + unit + 
                                    " | Period Revenue: ₹" + (int)rev + 
                                    " | Stock Value: ₹" + (int)stockVal;
                                card.addView(createTextView(detailsStr, 11, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));

                                listContainer.addView(card, cp);
                            }

                            if (renderedCount == 0) {
                                listContainer.addView(createTextView("No products match search filter.", 13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
                            }
                        };

                        renderFilteredCards.run();

                        // Add Search TextWatcher
                        searchInput.addTextChangedListener(new TextWatcher() {
                            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                                renderFilteredCards.run();
                            }
                            @Override public void afterTextChanged(Editable s) {}
                        });
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        listContainer.removeAllViews();
                        listContainer.addView(createTextView("Error loading stock analytics: " + e.getMessage(), 13, false, Color.parseColor("#DC2626")));
                    });
                }
            }).start();
        };

        periodSpinner.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Select Filter Period / कालावधी निवडा")
                .setItems(periodLabels, (dlg, which) -> {
                    selectedPeriodIdx[0] = which;
                    periodSpinner.setText("Period: " + periodLabels[which] + " ▾");
                    periodSpinner.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));
                    loadStockData.run();
                })
                .show();
        });

        loadStockData.run();
    }

    private void showCollectionsAnalyticsDialog() {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        TextView title = createTextView(
            currentLang.equals("mr") ? "पैसे जमा व बाकी विश्लेषण (Collections & Outstanding)" : "Money Collections & Outstanding Analytics",
            18, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")
        );
        TextView subtitle = createTextView(
            currentLang.equals("mr") ? "नगद (Cash), ऑनलाइन (UPI) जमा रक्कम व ग्राहकांची बाकी उधारी" : "Track cash collections, online UPI, total received & remaining balance to collect",
            12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")
        );
        subtitle.setPadding(0, dp(2), 0, dp(10));

        box.addView(title);
        box.addView(subtitle);

        // Period Selection Dropdown
        String[] periodLabelsEn = {"Today / Day-wise", "This Week", "This Month", "This Year", "Last Month", "All Time"};
        String[] periodLabelsMr = {"आज (Today)", "या आठवड्यात (This Week)", "या महिन्यात (This Month)", "या वर्षात (This Year)", "मागील महिन्यात (Last Month)", "सर्व वेळ (All Time)"};
        String[] periodKeys = {"today", "this_week", "this_month", "this_year", "last_month", "all"};
        final String[] periodLabels = currentLang.equals("mr") ? periodLabelsMr : periodLabelsEn;

        final int[] selectedPeriodIdx = new int[]{0};

        TextView periodSpinner = new TextView(this);
        periodSpinner.setText("Filter Period: " + periodLabels[0] + " ▾");
        periodSpinner.setTextSize(14);
        periodSpinner.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        periodSpinner.setBackgroundResource(R.drawable.bg_card);
        periodSpinner.setPadding(dp(12), dp(10), dp(12), dp(10));
        periodSpinner.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);

        LinearLayout.LayoutParams psp = new LinearLayout.LayoutParams(-1, dp(44));
        psp.bottomMargin = dp(10);
        box.addView(periodSpinner, psp);

        // KPI Summary Box
        LinearLayout kpiContainer = new LinearLayout(this);
        kpiContainer.setOrientation(LinearLayout.VERTICAL);
        kpiContainer.setPadding(dp(12), dp(10), dp(12), dp(10));
        kpiContainer.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams kclp = new LinearLayout.LayoutParams(-1, -2);
        kclp.bottomMargin = dp(10);
        box.addView(kpiContainer, kclp);

        // Tab Selector (Collections History vs Outstanding Balances to Collect)
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(0, dp(4), 0, dp(10));

        Button btnTabCollections = createStyledButton("💳 Payment Collections", Color.parseColor("#16A34A"), null);
        Button btnTabOutstanding = createStyledButton("🔴 Remaining to Collect", Color.parseColor("#DC2626"), null);

        LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(0, dp(38), 1.0f);
        tabLp.rightMargin = dp(4);
        tabRow.addView(btnTabCollections, tabLp);
        tabRow.addView(btnTabOutstanding, new LinearLayout.LayoutParams(0, dp(38), 1.0f));

        box.addView(tabRow);

        // Content Scrollable Container
        ScrollView scroll = new ScrollView(this);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);

        box.addView(scroll, new LinearLayout.LayoutParams(-1, dp(360)));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(box)
            .setPositiveButton("Close", null)
            .show();

        final int[] activeSubTab = new int[]{0}; // 0 = Collections, 1 = Remaining Outstanding

        Runnable fetchAndRender = () -> {
            listContainer.removeAllViews();
            listContainer.addView(createTextView("Loading collections analytics...", 13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));

            new Thread(() -> {
                try {
                    String pKey = periodKeys[selectedPeriodIdx[0]];
                    String query = pKey.isEmpty() || pKey.equals("all") ? "" : "?period=" + pKey;

                    JSONObject res = Api.request(this, "GET", "/api/reports/summary" + query, null);
                    JSONObject totals = res.optJSONObject("totals");
                    JSONArray salesArr = res.optJSONArray("sales");
                    JSONArray paymentsArr = res.optJSONArray("payments");
                    JSONArray customerBalsArr = res.optJSONArray("customerBalances");

                    double cashCol = totals != null ? totals.optDouble("cashCollected", 0) : 0;
                    double upiCol = totals != null ? totals.optDouble("upiCollected", 0) : 0;
                    double totRec = totals != null ? totals.optDouble("paymentsReceived", 0) : 0;
                    double remDue = totals != null ? totals.optDouble("totalOutstandingCredit", 0) : 0;

                    runOnUiThread(() -> {
                        kpiContainer.removeAllViews();
                        listContainer.removeAllViews();

                        // Render Top KPI Breakdown
                        kpiContainer.addView(createTextView(
                            "💵 Cash Received: ₹" + String.format(Locale.US, "%.0f", cashCol) + 
                            "  |  📲 Online / UPI: ₹" + String.format(Locale.US, "%.0f", upiCol),
                            14, true, Color.parseColor("#16A34A")
                        ));
                        kpiContainer.addView(createTextView(
                            "💰 Total Collected in Period: ₹" + String.format(Locale.US, "%.0f", totRec),
                            15, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")
                        ));
                        kpiContainer.addView(createTextView(
                            "🔴 Remaining Payment to Collect (Udhaar): ₹" + String.format(Locale.US, "%.0f", remDue),
                            14, true, Color.parseColor("#DC2626")
                        ));

                        if (activeSubTab[0] == 0) { // Collections List
                            listContainer.addView(createTextView("Recorded Payment Transactions in Selected Period:", 13, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));

                            int count = 0;

                            // 1. Render Sales with Payments
                            if (salesArr != null) {
                                for (int i = 0; i < salesArr.length(); i++) {
                                    JSONObject s = salesArr.optJSONObject(i);
                                    if (s == null) continue;

                                    double pd = s.optDouble("paidAmount", 0);
                                    if (pd <= 0) continue;

                                    count++;
                                    double cAmt = s.optDouble("cashAmount", 0);
                                    double uAmt = s.optDouble("upiAmount", 0);
                                    String uRef = s.optString("upiRefNo", "");
                                    String pm = s.optString("paymentMethod", "cash");
                                    String inv = s.optString("invoiceNo", "");
                                    String date = s.optString("createdAt", "");
                                    if (date.length() >= 10) date = date.substring(0, 10);

                                    JSONObject cust = s.optJSONObject("customerId");
                                    String cName = cust != null ? cust.optString("name", "Customer") : "Walk-in";
                                    String cMobile = cust != null ? cust.optString("mobile", "") : "";

                                    LinearLayout card = new LinearLayout(this);
                                    card.setOrientation(LinearLayout.VERTICAL);
                                    card.setPadding(dp(12), dp(10), dp(12), dp(10));
                                    card.setBackgroundResource(R.drawable.bg_card);
                                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                                    cp.bottomMargin = dp(8);

                                    card.addView(createTextView("Invoice #" + inv + " • " + cName + (cMobile.isEmpty() ? "" : " (" + cMobile + ")"), 14, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));

                                    String pMethodLabel = "";
                                    if ("split".equalsIgnoreCase(pm) || (cAmt > 0 && uAmt > 0)) {
                                        pMethodLabel = "Cash: ₹" + (int)cAmt + " + UPI: ₹" + (int)uAmt + (uRef.isEmpty() ? "" : " (Ref: " + uRef + ")");
                                    } else if ("upi".equalsIgnoreCase(pm) || uAmt > 0) {
                                        pMethodLabel = "UPI / Online: ₹" + (int)pd + (uRef.isEmpty() ? "" : " (Ref: " + uRef + ")");
                                    } else {
                                        pMethodLabel = "Cash: ₹" + (int)pd;
                                    }

                                    card.addView(createTextView(pMethodLabel + " | Date: " + date, 12, true, Color.parseColor("#16A34A")));
                                    listContainer.addView(card, cp);
                                }
                            }

                            // 2. Render Direct Account Payments
                            if (paymentsArr != null) {
                                for (int i = 0; i < paymentsArr.length(); i++) {
                                    JSONObject p = paymentsArr.optJSONObject(i);
                                    if (p == null) continue;

                                    count++;
                                    double amt = p.optDouble("amount", 0);
                                    double cAmt = p.optDouble("cashAmount", 0);
                                    double uAmt = p.optDouble("upiAmount", 0);
                                    String uRef = p.optString("upiRefNo", "");
                                    String pm = p.optString("method", "cash");
                                    String note = p.optString("note", "Account Payment");
                                    String date = p.optString("createdAt", "");
                                    if (date.length() >= 10) date = date.substring(0, 10);

                                    JSONObject cust = p.optJSONObject("customerId");
                                    String cName = cust != null ? cust.optString("name", "Customer") : "Customer";
                                    String cMobile = cust != null ? cust.optString("mobile", "") : "";

                                    LinearLayout card = new LinearLayout(this);
                                    card.setOrientation(LinearLayout.VERTICAL);
                                    card.setPadding(dp(12), dp(10), dp(12), dp(10));
                                    card.setBackgroundResource(R.drawable.bg_card);
                                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                                    cp.bottomMargin = dp(8);

                                    card.addView(createTextView("Direct Payment • " + cName + (cMobile.isEmpty() ? "" : " (" + cMobile + ")"), 14, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));

                                    String pMethodLabel = "";
                                    if ("split".equalsIgnoreCase(pm) || (cAmt > 0 && uAmt > 0)) {
                                        pMethodLabel = "Cash: ₹" + (int)cAmt + " + UPI: ₹" + (int)uAmt + (uRef.isEmpty() ? "" : " (Ref: " + uRef + ")");
                                    } else if ("upi".equalsIgnoreCase(pm) || uAmt > 0) {
                                        pMethodLabel = "UPI / Online: ₹" + (int)amt + (uRef.isEmpty() ? "" : " (Ref: " + uRef + ")");
                                    } else {
                                        pMethodLabel = "Cash: ₹" + (int)amt;
                                    }

                                    card.addView(createTextView(pMethodLabel + " | " + note + " | " + date, 12, true, Color.parseColor("#16A34A")));
                                    listContainer.addView(card, cp);
                                }
                            }

                            if (count == 0) {
                                listContainer.addView(createTextView("No payment collections recorded in this period.", 13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
                            }
                        } else { // Remaining Outstanding List
                            listContainer.addView(createTextView("Customers with Remaining Balance to Collect (Udhaar):", 13, true, Color.parseColor("#DC2626")));

                            if (customerBalsArr == null || customerBalsArr.length() == 0) {
                                listContainer.addView(createTextView("No outstanding customer balances! All accounts clear.", 14, false, Color.parseColor("#16A34A")));
                            } else {
                                for (int i = 0; i < customerBalsArr.length(); i++) {
                                    JSONObject c = customerBalsArr.optJSONObject(i);
                                    if (c == null) continue;

                                    String cId = c.optString("id");
                                    String cName = c.optString("name", "Customer");
                                    String cMobile = c.optString("mobile", "");
                                    double bal = c.optDouble("balance", 0);

                                    if (bal <= 0) continue;

                                    LinearLayout card = new LinearLayout(this);
                                    card.setOrientation(LinearLayout.VERTICAL);
                                    card.setPadding(dp(12), dp(10), dp(12), dp(10));
                                    card.setBackgroundResource(R.drawable.bg_card);
                                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                                    cp.bottomMargin = dp(8);

                                    card.addView(createTextView(cName + (cMobile.isEmpty() ? "" : " (" + cMobile + ")"), 14, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));
                                    card.addView(createTextView("Remaining Balance Due: ₹" + String.format(Locale.US, "%.0f", bal), 14, true, Color.parseColor("#DC2626")));

                                    Button recBtn = createStyledButton("💰 Record Payment Now", Color.parseColor("#16A34A"), () -> {
                                        if (dialog != null) dialog.dismiss();
                                        showCustomerStatementDialog(cId, cName);
                                    });
                                    LinearLayout.LayoutParams rbp = new LinearLayout.LayoutParams(-1, dp(36));
                                    rbp.topMargin = dp(6);
                                    card.addView(recBtn, rbp);

                                    listContainer.addView(card, cp);
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        listContainer.removeAllViews();
                        listContainer.addView(createTextView("Error loading collections: " + e.getMessage(), 13, false, Color.parseColor("#DC2626")));
                    });
                }
            }).start();
        };

        btnTabCollections.setOnClickListener(v -> {
            activeSubTab[0] = 0;
            btnTabCollections.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#16A34A")));
            btnTabOutstanding.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
            fetchAndRender.run();
        });

        btnTabOutstanding.setOnClickListener(v -> {
            activeSubTab[0] = 1;
            btnTabOutstanding.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DC2626")));
            btnTabCollections.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
            fetchAndRender.run();
        });

        periodSpinner.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Select Filter Period / कालावधी निवडा")
                .setItems(periodLabels, (dlg, which) -> {
                    selectedPeriodIdx[0] = which;
                    periodSpinner.setText("Filter Period: " + periodLabels[which] + " ▾");
                    periodSpinner.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));
                    fetchAndRender.run();
                })
                .show();
        });

        fetchAndRender.run();
    }

    private interface OnDateRangeSelectedListener {
        void onRangeSelected(String startDate, String endDate);
    }

    private String selectedCustPeriod = "all";
    private String custCustomStartDate = "";
    private String custCustomEndDate = "";

    private void loadCustomerBills() {
        activeTab = "my_bills";
        buildBottomNav();
        boolean isDark = LocaleHelper.isDarkMode(this);
        statusTextView.setText(currentLang.equals("mr") ? "माझे खरेदी व पेमेंट इतिहास" : "My Purchase History");

        mainContentContainer.removeAllViews();

        // Title Box
        mainContentContainer.addView(createTextView(
            currentLang.equals("mr") ? "माझे खरेदी व पेमेंट इतिहास" : "My Purchase History",
            22, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")
        ));
        mainContentContainer.addView(createTextView(
            "Customer: " + Api.userName(this),
            13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")
        ));

        // Date Filter Selection Controls
        String[] periodLabelsEn = {"Today", "This Week", "This Month", "This Year", "All Time", "Custom Date Range"};
        String[] periodLabelsMr = {"आज (Today)", "या आठवड्यात (This Week)", "या महिन्यात (This Month)", "या वर्षात (This Year)", "सर्व वेळ (All Time)", "विशिष्ट तारीख (Custom Date)"};
        String[] periodKeys = {"today", "week", "month", "year", "all", "custom"};
        final String[] periodLabels = currentLang.equals("mr") ? periodLabelsMr : periodLabelsEn;

        TextView filterSpinner = new TextView(this);
        filterSpinner.setText("Date Filter: " + periodLabels[4] + " ▾");
        filterSpinner.setTextSize(14);
        filterSpinner.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
        filterSpinner.setBackgroundResource(R.drawable.bg_card);
        filterSpinner.setPadding(dp(12), dp(10), dp(12), dp(10));
        filterSpinner.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);

        LinearLayout.LayoutParams fslp = new LinearLayout.LayoutParams(-1, dp(44));
        fslp.topMargin = dp(8);
        fslp.bottomMargin = dp(10);
        mainContentContainer.addView(filterSpinner, fslp);

        // Summary Card Container
        LinearLayout summaryCard = new LinearLayout(this);
        summaryCard.setOrientation(LinearLayout.VERTICAL);
        summaryCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        summaryCard.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(-1, -2);
        sclp.bottomMargin = dp(12);
        mainContentContainer.addView(summaryCard, sclp);

        // History Items Container
        LinearLayout historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        mainContentContainer.addView(historyContainer, new LinearLayout.LayoutParams(-1, -2));

        Runnable fetchHistory = () -> {
            summaryCard.removeAllViews();
            historyContainer.removeAllViews();
            summaryCard.addView(createTextView("Loading purchase & payment history...", 13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));

            new Thread(() -> {
                try {
                    String query = "?period=" + selectedCustPeriod;
                    if ("custom".equals(selectedCustPeriod) && !custCustomStartDate.isEmpty() && !custCustomEndDate.isEmpty()) {
                        query += "&startDate=" + custCustomStartDate + "&endDate=" + custCustomEndDate;
                    }

                    JSONObject res = Api.request(this, "GET", "/api/me/history" + query, null);
                    JSONObject summary = res.optJSONObject("summary");
                    JSONArray billsArr = res.optJSONArray("bills");
                    JSONArray paymentsArr = res.optJSONArray("payments");

                    double purchases = summary != null ? summary.optDouble("totalPurchases", 0) : 0;
                    double cashPaid = summary != null ? summary.optDouble("cashPaid", 0) : 0;
                    double upiPaid = summary != null ? summary.optDouble("upiPaid", 0) : 0;
                    double totalPaid = summary != null ? summary.optDouble("totalPaid", 0) : 0;
                    double endingDue = summary != null ? summary.optDouble("endingOutstanding", 0) : 0;
                    int billsCount = summary != null ? summary.optInt("billsCount", 0) : 0;

                    runOnUiThread(() -> {
                        summaryCard.removeAllViews();
                        historyContainer.removeAllViews();

                        // Populate Summary Card
                        summaryCard.addView(createTextView(
                            "Invoices Count: " + billsCount + " Bills" +
                            "  |  Total Purchases: ₹" + String.format(Locale.US, "%.0f", purchases),
                            14, true, Color.parseColor("#16A34A")
                        ));
                        summaryCard.addView(createTextView(
                            "💵 Cash Paid: ₹" + String.format(Locale.US, "%.0f", cashPaid) +
                            "  |  📲 UPI Paid: ₹" + String.format(Locale.US, "%.0f", upiPaid),
                            13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")
                        ));
                        summaryCard.addView(createTextView(
                            "💰 Total Payments in Period: ₹" + String.format(Locale.US, "%.0f", totalPaid),
                            14, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")
                        ));

                        if (endingDue > 0) {
                            summaryCard.addView(createTextView(
                                "🔴 Net Outstanding Balance: ₹" + String.format(Locale.US, "%.0f", endingDue),
                                15, true, Color.parseColor("#DC2626")
                            ));
                        } else if (endingDue < 0) {
                            summaryCard.addView(createTextView(
                                "🟢 Advance / Overpaid Credit Balance: ₹" + String.format(Locale.US, "%.0f", Math.abs(endingDue)),
                                15, true, Color.parseColor("#16A34A")
                            ));
                        } else {
                            summaryCard.addView(createTextView(
                                "🟢 Net Ending Balance: ₹0 (Account Fully Clear)",
                                15, true, Color.parseColor("#16A34A")
                            ));
                        }

                        // Populate Chronological History
                        historyContainer.addView(createTextView("Invoices & Payments History:", 14, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));

                        int totalRecords = 0;

                        // 1. Invoices
                        if (billsArr != null && billsArr.length() > 0) {
                            for (int i = 0; i < billsArr.length(); i++) {
                                JSONObject b = billsArr.optJSONObject(i);
                                if (b == null) continue;
                                totalRecords++;

                                String inv = b.optString("invoiceNo", "");
                                double tot = b.optDouble("total", 0);
                                double pd = b.optDouble("paidAmount", 0);
                                double diff = tot - pd;
                                String pm = b.optString("paymentMethod", "cash");
                                String dateRaw = b.optString("createdAt", "");

                                String formattedDate = dateRaw.length() >= 10 ? dateRaw.substring(0, 10) : dateRaw;
                                try {
                                    SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                    Date parsedD = sdfIn.parse(formattedDate);
                                    if (parsedD != null) {
                                        SimpleDateFormat sdfOut = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US);
                                        formattedDate = sdfOut.format(parsedD);
                                    }
                                } catch (Exception ignored) {}

                                // Item Summary
                                StringBuilder itemsSummary = new StringBuilder();
                                JSONArray itemsArr = b.optJSONArray("items");
                                if (itemsArr != null) {
                                    for (int k = 0; k < itemsArr.length(); k++) {
                                        JSONObject it = itemsArr.optJSONObject(k);
                                        if (it != null) {
                                            if (itemsSummary.length() > 0) itemsSummary.append(", ");
                                            itemsSummary.append(it.optString("nameMr", it.optString("nameEn")))
                                                        .append(" x ").append((int)it.optDouble("quantity", 1)).append(" ").append(it.optString("unit", "bag"));
                                        }
                                    }
                                }

                                LinearLayout card = new LinearLayout(this);
                                card.setOrientation(LinearLayout.VERTICAL);
                                card.setPadding(dp(14), dp(12), dp(14), dp(12));
                                card.setBackgroundResource(R.drawable.bg_card);
                                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                                cp.topMargin = dp(8);

                                // Title Row
                                LinearLayout titleRow = new LinearLayout(this);
                                titleRow.setOrientation(LinearLayout.HORIZONTAL);
                                titleRow.setGravity(Gravity.CENTER_VERTICAL);
                                titleRow.addView(createTextView("🧾 Bill #" + inv, 15, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")), new LinearLayout.LayoutParams(0, -2, 1.0f));

                                String statusLabel = "PAID";
                                int statusColor = Color.parseColor("#16A34A");
                                String amtDetails = "";

                                if (diff > 0) {
                                    statusLabel = pd > 0 ? "PARTIAL" : "UNPAID";
                                    statusColor = pd > 0 ? Color.parseColor("#0284C7") : Color.parseColor("#DC2626");
                                    amtDetails = "Total: ₹" + (int)tot + " | Paid: ₹" + (int)pd + " | Due: ₹" + (int)diff;
                                } else if (diff < 0) {
                                    statusLabel = "OVERPAID";
                                    statusColor = Color.parseColor("#16A34A");
                                    amtDetails = "Total: ₹" + (int)tot + " | Paid: ₹" + (int)pd + " | Overpaid: ₹" + (int)Math.abs(diff);
                                } else {
                                    statusLabel = "PAID";
                                    statusColor = Color.parseColor("#16A34A");
                                    amtDetails = "Total: ₹" + (int)tot + " | Paid: ₹" + (int)pd + " | Due: ₹0";
                                }

                                TextView statusBadge = createTextView(statusLabel, 10, true, statusColor);
                                statusBadge.setBackgroundResource(diff > 0 ? R.drawable.bg_badge_low_stock : R.drawable.bg_badge_in_stock);
                                statusBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
                                titleRow.addView(statusBadge);

                                card.addView(titleRow);
                                card.addView(createTextView("📅 " + formattedDate + (itemsSummary.length() > 0 ? "  |  " + itemsSummary : ""), 12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
                                card.addView(createTextView(amtDetails, 13, true, diff > 0 ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A")));

                                Button pdfBtn = createStyledButton("📄 Download PDF Invoice", Color.parseColor("#0284C7"), () -> {
                                    try {
                                        File pdf = PdfGenerator.generateInvoicePdf(this, b, Api.userName(this));
                                        promptSharePdf(pdf);
                                    } catch (Exception ex) {
                                        Toast.makeText(this, "PDF Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });

                                if ("owner".equalsIgnoreCase(userRole)) {
                                    String saleId = b.optString("_id");
                                    Button editBillBtn = createStyledButton("✏️ Edit Bill", Color.parseColor("#E85D04"), () -> {
                                        showEditBillDialog(saleId, this::loadCustomerBills);
                                    });

                                    LinearLayout btnRow = new LinearLayout(this);
                                    btnRow.setOrientation(LinearLayout.HORIZONTAL);
                                    btnRow.setPadding(0, dp(6), 0, 0);
                                    btnRow.addView(pdfBtn, new LinearLayout.LayoutParams(0, dp(38), 1.0f));
                                    View sp2 = new View(this); btnRow.addView(sp2, new LinearLayout.LayoutParams(dp(8), 1));
                                    btnRow.addView(editBillBtn, new LinearLayout.LayoutParams(0, dp(38), 1.0f));
                                    card.addView(btnRow);
                                } else {
                                    LinearLayout.LayoutParams pbp = new LinearLayout.LayoutParams(-1, dp(38));
                                    pbp.topMargin = dp(8);
                                    card.addView(pdfBtn, pbp);
                                }

                                historyContainer.addView(card, cp);
                            }
                        }

                        // 2. Direct Account Payments
                        if (paymentsArr != null && paymentsArr.length() > 0) {
                            for (int i = 0; i < paymentsArr.length(); i++) {
                                JSONObject p = paymentsArr.optJSONObject(i);
                                if (p == null) continue;
                                totalRecords++;

                                double amt = p.optDouble("amount", 0);
                                double cAmt = p.optDouble("cashAmount", 0);
                                double uAmt = p.optDouble("upiAmount", 0);
                                String uRef = p.optString("upiRefNo", "");
                                String pm = p.optString("method", "cash");
                                String note = p.optString("note", "Account Payment");
                                String dateRaw = p.optString("createdAt", "");

                                String formattedDate = dateRaw.length() >= 10 ? dateRaw.substring(0, 10) : dateRaw;
                                try {
                                    SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                    Date parsedD = sdfIn.parse(formattedDate);
                                    if (parsedD != null) {
                                        SimpleDateFormat sdfOut = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US);
                                        formattedDate = sdfOut.format(parsedD);
                                    }
                                } catch (Exception ignored) {}

                                LinearLayout card = new LinearLayout(this);
                                card.setOrientation(LinearLayout.VERTICAL);
                                card.setPadding(dp(14), dp(12), dp(14), dp(12));
                                card.setBackgroundResource(R.drawable.bg_card);
                                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                                cp.topMargin = dp(8);

                                card.addView(createTextView("💳 Payment Receipt • ₹ " + (int)amt, 15, true, Color.parseColor("#16A34A")));

                                String pMethodLabel = "";
                                if ("split".equalsIgnoreCase(pm) || (cAmt > 0 && uAmt > 0)) {
                                    pMethodLabel = "Cash: ₹" + (int)cAmt + " + UPI: ₹" + (int)uAmt + (uRef.isEmpty() ? "" : " (Ref: " + uRef + ")");
                                } else if ("upi".equalsIgnoreCase(pm) || uAmt > 0) {
                                    pMethodLabel = "UPI / Online: ₹" + (int)amt + (uRef.isEmpty() ? "" : " (Ref: " + uRef + ")");
                                } else {
                                    pMethodLabel = "Cash: ₹" + (int)amt;
                                }

                                card.addView(createTextView(pMethodLabel + " | " + note, 12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
                                card.addView(createTextView("📅 " + formattedDate, 12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));

                                historyContainer.addView(card, cp);
                            }
                        }

                        if (totalRecords == 0) {
                            historyContainer.addView(createTextView("No purchase or payment records found for the selected period.", 13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        summaryCard.removeAllViews();
                        summaryCard.addView(createTextView("Error loading history: " + e.getMessage(), 13, false, Color.parseColor("#DC2626")));
                    });
                }
            }).start();
        };

        filterSpinner.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Select Date Filter")
                .setItems(periodLabels, (dlg, which) -> {
                    if (which == 5) { // Custom Date Range
                        showCustomDatePickerDialog((startD, endD) -> {
                            selectedCustPeriod = "custom";
                            custCustomStartDate = startD;
                            custCustomEndDate = endD;
                            filterSpinner.setText("Date Filter: " + startD + " to " + endD + " ▾");
                            filterSpinner.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));
                            fetchHistory.run();
                        });
                    } else {
                        selectedCustPeriod = periodKeys[which];
                        filterSpinner.setText("Date Filter: " + periodLabels[which] + " ▾");
                        filterSpinner.setTextColor(LocaleHelper.isDarkMode(this) ? Color.WHITE : Color.parseColor("#1E293B"));
                        fetchHistory.run();
                    }
                })
                .show();
        });

        fetchHistory.run();
    }

    private void showCustomDatePickerDialog(OnDateRangeSelectedListener onSelected) {
        Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR), m = cal.get(Calendar.MONTH), d = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog startPicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String startFormatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);

            DatePickerDialog endPicker = new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                String endFormatted = String.format(Locale.US, "%04d-%02d-%02d", year2, month2 + 1, dayOfMonth2);
                if (endFormatted.compareTo(startFormatted) < 0) {
                    Toast.makeText(this, "End date cannot be before start date.", Toast.LENGTH_LONG).show();
                    return;
                }
                onSelected.onRangeSelected(startFormatted, endFormatted);
            }, year, month, dayOfMonth);
            endPicker.setTitle("Select End Date");
            endPicker.show();
        }, y, m, d);
        startPicker.setTitle("Select Start Date");
        startPicker.show();
    }

    private void loadSettingsView() {
        activeTab = "settings";
        buildBottomNav();
        statusTextView.setText(currentLang.equals("mr") ? "मालक प्रोफाईल" : "Owner Profile");
        mainContentContainer.removeAllViews();

        if ("owner".equalsIgnoreCase(userRole)) {
            // Profile Header
            LinearLayout profHeader = new LinearLayout(this);
            profHeader.setOrientation(LinearLayout.HORIZONTAL);
            profHeader.setGravity(Gravity.CENTER_VERTICAL);
            profHeader.setPadding(dp(16), dp(16), dp(16), dp(16));
            profHeader.setBackgroundResource(R.drawable.bg_card);

            FrameLayout avatarFrame = new FrameLayout(this);
            ImageView avatar = new ImageView(this);
            avatar.setLayoutParams(new FrameLayout.LayoutParams(dp(70), dp(70)));
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatar.setImageResource(android.R.drawable.ic_menu_camera);
            avatar.setBackgroundResource(R.drawable.bg_badge_low_stock);
            avatar.setPadding(dp(16), dp(16), dp(16), dp(16));
            avatarFrame.addView(avatar);

            TextView editAvatarBtn = createTextView("✏️", 12, false, Color.WHITE);
            editAvatarBtn.setBackgroundResource(R.drawable.bg_card_primary);
            editAvatarBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
            FrameLayout.LayoutParams eap = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
            editAvatarBtn.setLayoutParams(eap);
            avatarFrame.addView(editAvatarBtn);

            profHeader.addView(avatarFrame);

            LinearLayout profInfo = new LinearLayout(this);
            profInfo.setOrientation(LinearLayout.VERTICAL);
            profInfo.setPadding(dp(16), 0, 0, 0);

            profInfo.addView(createTextView("Vinayak Bhau Tarde", 18, true, Color.parseColor("#1E293B")));
            profInfo.addView(createTextView("Owner — Shravan Traders", 13, false, Color.parseColor("#64748B")));
            profInfo.addView(createTextView("Owner ID: OWNER001", 12, false, Color.parseColor("#94A3B8")));

            TextView activeStatus = createTextView("Active", 11, true, Color.parseColor("#16A34A"));
            activeStatus.setBackgroundResource(R.drawable.bg_badge_in_stock);
            activeStatus.setPadding(dp(8), dp(2), dp(8), dp(2));
            LinearLayout.LayoutParams asp = new LinearLayout.LayoutParams(-2, -2);
            asp.topMargin = dp(4);
            profInfo.addView(activeStatus, asp);

            profHeader.addView(profInfo, new LinearLayout.LayoutParams(0, -2, 1.0f));
            
            TextView chevron1 = createTextView(">", 18, true, Color.parseColor("#94A3B8"));
            profHeader.addView(chevron1);
            
            profHeader.setOnClickListener(v -> showShopSettingsDialog()); // Open settings edit

            mainContentContainer.addView(profHeader, new LinearLayout.LayoutParams(-1, -2));

            // Shop Details View
            SharedPreferences prefs = getSharedPreferences("ShopPrefs", MODE_PRIVATE);
            LinearLayout shopDetails = new LinearLayout(this);
            shopDetails.setOrientation(LinearLayout.VERTICAL);
            shopDetails.setPadding(dp(16), dp(12), dp(16), dp(12));
            shopDetails.setBackgroundResource(R.drawable.bg_card);
            LinearLayout.LayoutParams sdp = new LinearLayout.LayoutParams(-1, -2);
            sdp.topMargin = dp(12);
            
            shopDetails.addView(buildInfoRow("🏪", "Shop Name", prefs.getString("shopName", "Shravan Traders (श्रवण ट्रेडर्स)")));
            shopDetails.addView(buildInfoRow("📍", "Address", prefs.getString("shopDesc", "Cattle Feed Shop • Kendal Bk., Taluka - Rahuri, Dist - Ahilyanagar")));
            shopDetails.addView(buildInfoRow("📞", "Proprietor & Contact", prefs.getString("shopContact", "Proprietor: Vinayak Bhau Tarde | Contact: +91 8390529481")));

            mainContentContainer.addView(shopDetails, sdp);

            // Fetch and Show KPIs
            LinearLayout kpiBox = new LinearLayout(this);
            kpiBox.setOrientation(LinearLayout.HORIZONTAL);
            kpiBox.setPadding(0, dp(12), 0, dp(12));
            mainContentContainer.addView(kpiBox, new LinearLayout.LayoutParams(-1, -2));

            new Thread(() -> {
                try {
                    JSONObject r = Api.request(this, "GET", "/api/reports/summary", null);
                    JSONObject totals = r.getJSONObject("totals");
                    double revenue = totals.optDouble("salesRevenue", 0);
                    double outstanding = totals.optDouble("totalOutstandingCredit", 0);
                    int customersCount = totals.optInt("bills", 0); // Using bills as proxy for customers if needed, or better fetch /api/customers
                    int productsCount = totals.optInt("productsCount", 0);

                    runOnUiThread(() -> {
                        kpiBox.removeAllViews();
                        kpiBox.addView(buildSmallKpiCard("₹ " + String.format(Locale.US, "%.0f", revenue), "Today's Sales", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7")), new LinearLayout.LayoutParams(0, -2, 1.0f));
                        kpiBox.addView(buildSmallKpiCard(String.valueOf(customersCount), "Customers", Color.parseColor("#0284C7"), Color.parseColor("#E0F2FE")), new LinearLayout.LayoutParams(0, -2, 1.0f));
                        kpiBox.addView(buildSmallKpiCard(String.valueOf(productsCount), "Products", Color.parseColor("#E85D04"), Color.parseColor("#FFEDD5")), new LinearLayout.LayoutParams(0, -2, 1.0f));
                        kpiBox.addView(buildSmallKpiCard("₹ " + String.format(Locale.US, "%.0f", outstanding), "Outstanding", Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2")), new LinearLayout.LayoutParams(0, -2, 1.0f));
                    });
                } catch (Exception e) {}
            }).start();

            // Options List
            LinearLayout optsCard = new LinearLayout(this);
            optsCard.setOrientation(LinearLayout.VERTICAL);
            optsCard.setPadding(dp(16), dp(16), dp(16), dp(16));
            optsCard.setBackgroundResource(R.drawable.bg_card);

            optsCard.addView(buildOptionRow("⚙️", "Shop & Profile Settings", "Shop details, logo, contact, invoice settings", this::showShopSettingsDialog));
            optsCard.addView(buildOptionRow("📢", currentLang.equals("mr") ? "दुकान सूचना फलक (Notice Board)" : "Shop Notice Board", currentLang.equals("mr") ? "सर्व ग्राहकांसाठी सूचना फलक तयार करा, संपादित करा किंवा हटवा (फक्त मालक)" : "Post, edit or clear announcement notice for customers (Owner only)", this::showNoticeBoardEditorDialog));
            optsCard.addView(buildOptionRow("🧾", currentLang.equals("mr") ? "ग्राहक बिले डाउनलोड केंद्र" : "Customer Bills & Invoices Download Center", currentLang.equals("mr") ? "प्रत्येक ग्राहकाची खरेदी बिले (PDF) स्वतंत्रपणे व सर्व एकत्र डाउनलोड करा" : "Download individual purchase bills (PDF) of each customer, or all bills together", this::loadCustomerBillsDownloadCenter));
            optsCard.addView(buildOptionRow("🔑", "Change Password", "Change owner account password securely.", this::showChangePasswordDialog));
            optsCard.addView(buildOptionRow("💰", currentLang.equals("mr") ? "पैसे जमा व बाकी विश्लेषण (Collections)" : "Collections & Outstanding Analytics", currentLang.equals("mr") ? "नगद (Cash), ऑनलाइन (UPI) जमा व एकूण बाकी उधारी (दिवस/आठवडा/महिना/वर्ष फिल्टर)" : "Check cash collected (₹ X), online UPI (₹ Y), total received & remaining balance to collect with period filters", this::showCollectionsAnalyticsDialog));
            optsCard.addView(buildOptionRow("📊", currentLang.equals("mr") ? "साठा व विक्री तपासणी (Stock & Sell/Unsell)" : "Stock & Sold/Unsold Analytics", currentLang.equals("mr") ? "उत्पादनांनुसार विक्री, शिल्लक साठा व कालावधी फिल्टर" : "Check product-wise current stock, sold & unsold quantity with month/period filters", this::showStockAnalyticsDialog));
            optsCard.addView(buildOptionRow("🔄", "Restore Deleted Products", "View and restore deleted products back to catalog", this::showRestorableProductsDialog));
            optsCard.addView(buildOptionRow("📦", "Product Management", "Add, edit, stock, prices", this::loadProductManagement));
            optsCard.addView(buildOptionRow("👥", "Customer Management", "View all customers and purchase details", this::loadCustomersAndCredit));
            optsCard.addView(buildOptionRow("🛒", "Sales & Billing", "Create bill, view sales history", this::loadPOSBilling));
            optsCard.addView(buildOptionRow("🚚", "Purchase & Supplier Management", "Manage suppliers and purchase stock", this::loadOwnerDashboard));
            optsCard.addView(buildOptionRow("📊", "Reports & Analytics", "Daily, weekly, monthly, yearly reports", this::loadReportsView));
            optsCard.addView(buildOptionRow("💳", "Expenses Management", "Record shop expenses", () -> Toast.makeText(this, "Expense management pending.", Toast.LENGTH_SHORT).show()));
            optsCard.addView(buildOptionRow("☁️", "Backup & Sync", "Cloud backup, data sync", () -> Toast.makeText(this, "Data is synced to MongoDB Atlas.", Toast.LENGTH_SHORT).show()));
            optsCard.addView(buildOptionRow("🌐", "App Settings", "Language, user management", () -> {
                LocaleHelper.setLanguage(this, currentLang.equals("mr") ? "en" : "mr");
                recreate();
            }));
            optsCard.addView(buildOptionRow("🚪", "Logout", "Sign out from owner account", () -> {
                new AlertDialog.Builder(this).setTitle("Logout").setMessage("Are you sure you want to sign out?").setPositiveButton("Logout", (d,w) -> {
                    Api.clearSession(this);
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                }).setNegativeButton("Cancel", null).show();
            }));

            mainContentContainer.addView(optsCard, new LinearLayout.LayoutParams(-1, -2));
        } else {
            activeTab = "settings";
            buildBottomNav();
            statusTextView.setText(currentLang.equals("mr") ? "सेटिंग्ज व प्रोफाईल" : "Settings & Profile");
            mainContentContainer.removeAllViews();

            LinearLayout custOptsCard = new LinearLayout(this);
            custOptsCard.setOrientation(LinearLayout.VERTICAL);
            custOptsCard.setPadding(dp(16), dp(16), dp(16), dp(16));
            custOptsCard.setBackgroundResource(R.drawable.bg_card);

            custOptsCard.addView(buildOptionRow("👤", currentLang.equals("mr") ? "माझे प्रोफाईल" : "My Profile", currentLang.equals("mr") ? "नाव, संपर्क, पत्ता व खाते तपशील" : "View account details & statement", this::loadCustomerProfile));
            custOptsCard.addView(buildOptionRow("🧾", currentLang.equals("mr") ? "माझी खरेदी बिले" : "My Purchase Bills", currentLang.equals("mr") ? "खरेदी इतिहास व बिले तपासा" : "View invoices & purchase history", this::loadCustomerBills));
            custOptsCard.addView(buildOptionRow("🌐", currentLang.equals("mr") ? "भाषा बदला (Change Language)" : "Change Language", currentLang.equals("mr") ? "सध्या: मराठी ➔ इंग्रजीत बदला" : "Currently: English ➔ Change to Marathi", () -> {
                LocaleHelper.setLanguage(this, currentLang.equals("mr") ? "en" : "mr");
                recreate();
            }));

            custOptsCard.addView(buildOptionRow("🚪", currentLang.equals("mr") ? "लॉगआउट करा (Sign Out)" : "Logout", currentLang.equals("mr") ? "खात्यातून सुरक्षितपणे बाहेर पडा" : "Sign out securely from customer account", () -> {
                new AlertDialog.Builder(this)
                    .setTitle(currentLang.equals("mr") ? "लॉगआउट" : "Logout")
                    .setMessage(currentLang.equals("mr") ? "तुम्हाला खात्री आहे की तुम्ही साइन आउट करू इच्छिता?" : "Are you sure you want to sign out?")
                    .setPositiveButton(currentLang.equals("mr") ? "लॉगआउट" : "Logout", (d, w) -> {
                        Api.clearSession(this);
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton(currentLang.equals("mr") ? "रद्द करा" : "Cancel", null)
                    .show();
            }));

            mainContentContainer.addView(custOptsCard, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private LinearLayout buildSmallKpiCard(String val, String title, int valColor, int bgColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(12), dp(8), dp(12));
        card.setGravity(Gravity.CENTER);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dp(12));
        card.setBackground(gd);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        card.setLayoutParams(lp);

        card.addView(createTextView(val, 14, true, valColor));
        TextView tvTitle = createTextView(title, 10, false, Color.parseColor("#475569"));
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, dp(4), 0, 0);
        card.addView(tvTitle);

        return card;
    }

    private void showShopSettingsDialog() {
        boolean isDark = LocaleHelper.isDarkMode(this);
        SharedPreferences prefs = getSharedPreferences("ShopPrefs", MODE_PRIVATE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        box.addView(createTextView("Shop Invoice Branding", 16, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));
        EditText shopName = createFormInput("Shop Name", prefs.getString("shopName", "SHRAVAN TRADERS (श्रवण ट्रेडर्स)"), InputType.TYPE_CLASS_TEXT);
        EditText shopDesc = createFormInput("Tagline/Address", prefs.getString("shopDesc", "Cattle Feed Shop • Kendal Bk., Taluka - Rahuri, Dist - Ahilyanagar"), InputType.TYPE_CLASS_TEXT);
        EditText shopContact = createFormInput("Proprietor & Contact", prefs.getString("shopContact", "Proprietor: Vinayak Bhau Tarde | Contact: +91 8390529481"), InputType.TYPE_CLASS_TEXT);
        
        box.addView(shopName);
        box.addView(shopDesc);
        box.addView(shopContact);

        new AlertDialog.Builder(this)
            .setTitle("Shop Settings")
            .setView(box)
            .setPositiveButton("Save", (d, w) -> {
                prefs.edit()
                    .putString("shopName", shopName.getText().toString().trim())
                    .putString("shopDesc", shopDesc.getText().toString().trim())
                    .putString("shopContact", shopContact.getText().toString().trim())
                    .apply();
                Toast.makeText(this, "Shop settings saved!", Toast.LENGTH_SHORT).show();
                loadSettingsView(); // refresh
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ==========================================
    // NOTICE BOARD (OWNER EDIT, CUSTOMER READ-ONLY)
    // ==========================================

    private void fetchAndRenderNoticeBoard(LinearLayout container, boolean isOwner) {
        new Thread(() -> {
            try {
                JSONObject res = Api.request(this, "GET", "/api/notice", null);
                JSONObject notice = res != null ? res.optJSONObject("notice") : null;
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    renderNoticeBoardUi(container, notice, isOwner);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isOwner) {
                        container.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    private void renderNoticeBoardUi(LinearLayout container, JSONObject notice, boolean isOwner) {
        container.removeAllViews();
        boolean isDark = LocaleHelper.isDarkMode(this);

        boolean isActive = notice != null && notice.optBoolean("active", false);
        String message = notice != null ? notice.optString("message", "").trim() : "";
        String title = notice != null ? notice.optString("title", "").trim() : "";
        String updatedAt = notice != null ? notice.optString("updatedAt", "") : "";
        if (updatedAt.length() >= 10) updatedAt = updatedAt.substring(0, 10);

        if (!isActive || message.isEmpty()) {
            if (!isOwner) {
                container.setVisibility(View.GONE);
                return;
            }
            // If owner and no notice is active, show prompt card with "+ Post Notice"
            container.setVisibility(View.VISIBLE);
            LinearLayout promptCard = new LinearLayout(this);
            promptCard.setOrientation(LinearLayout.VERTICAL);
            promptCard.setPadding(dp(14), dp(10), dp(14), dp(10));

            GradientDrawable pd = new GradientDrawable();
            pd.setColor(isDark ? Color.parseColor("#1E293B") : Color.parseColor("#F8FAFC"));
            pd.setCornerRadius(dp(12));
            pd.setStroke(dp(1), isDark ? Color.parseColor("#334155") : Color.parseColor("#CBD5E1"));
            promptCard.setBackground(pd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(8);
            lp.bottomMargin = dp(10);
            promptCard.setLayoutParams(lp);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView tv = createTextView("📢 " + (currentLang.equals("mr") ? "दुकान सूचना फलक (सध्या कोणतीही सूचना सक्रिय नाही)" : "Notice Board (No active notice)"), 13, true, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
            row.addView(tv, new LinearLayout.LayoutParams(0, -2, 1.0f));

            Button btnAdd = createStyledButton(currentLang.equals("mr") ? "+ सूचना लिहा" : "+ Post Notice", Color.parseColor("#E85D04"), this::showNoticeBoardEditorDialog);
            btnAdd.setTextSize(12);
            row.addView(btnAdd, new LinearLayout.LayoutParams(-2, dp(34)));

            promptCard.addView(row);
            container.addView(promptCard);
            return;
        }

        // Active Notice Card
        container.setVisibility(View.VISIBLE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable gd = new GradientDrawable();
        if (isDark) {
            gd.setColor(Color.parseColor("#2A1B0E"));
            gd.setStroke(dp(2), Color.parseColor("#B45309"));
        } else {
            gd.setColor(Color.parseColor("#FFFBEB"));
            gd.setStroke(dp(2), Color.parseColor("#F59E0B"));
        }
        gd.setCornerRadius(dp(14));
        card.setBackground(gd);

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.topMargin = dp(6);
        clp.bottomMargin = dp(14);
        card.setLayoutParams(clp);

        // Header Row: [📢 दुकान सूचना फलक] ... [Date / Edit Button]
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(8));

        // Badge Container
        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER_VERTICAL);
        badge.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable bgBadge = new GradientDrawable();
        bgBadge.setColor(Color.parseColor("#EA580C"));
        bgBadge.setCornerRadius(dp(6));
        badge.setBackground(bgBadge);

        TextView badgeTv = createTextView(currentLang.equals("mr") ? "📢 दुकान सूचना फलक" : "📢 SHOP NOTICE BOARD", 11, true, Color.WHITE);
        badge.addView(badgeTv);
        headerRow.addView(badge, new LinearLayout.LayoutParams(-2, -2));

        View space = new View(this);
        headerRow.addView(space, new LinearLayout.LayoutParams(0, 1, 1.0f));

        if (!updatedAt.isEmpty()) {
            TextView dateTv = createTextView(updatedAt, 11, false, isDark ? Color.parseColor("#D97706") : Color.parseColor("#B45309"));
            dateTv.setPadding(0, 0, dp(8), 0);
            headerRow.addView(dateTv);
        }

        // Only owner can edit it!
        if (isOwner) {
            Button btnEdit = createStyledButton("✏️ " + (currentLang.equals("mr") ? "बदला" : "Edit"), Color.parseColor("#C2410C"), this::showNoticeBoardEditorDialog);
            btnEdit.setTextSize(11);
            headerRow.addView(btnEdit, new LinearLayout.LayoutParams(-2, dp(32)));
        }

        card.addView(headerRow);

        // Title if available
        if (!title.isEmpty()) {
            TextView titleTv = createTextView(title, 16, true, isDark ? Color.parseColor("#FDE68A") : Color.parseColor("#92400E"));
            titleTv.setPadding(0, dp(2), 0, dp(6));
            card.addView(titleTv);
        }

        // Message text
        TextView msgTv = createTextView(message, 14, false, isDark ? Color.parseColor("#FEF3C7") : Color.parseColor("#78350F"));
        msgTv.setLineSpacing(dp(2), 1.15f);
        msgTv.setTextIsSelectable(true);
        card.addView(msgTv);

        container.addView(card);
    }

    private void showNoticeBoardEditorDialog() {
        if (!"owner".equalsIgnoreCase(userRole)) {
            Toast.makeText(this, "Only owner can edit the notice board.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                JSONObject res = Api.request(this, "GET", "/api/notice", null);
                JSONObject notice = res != null ? res.optJSONObject("notice") : null;
                String currTitle = notice != null ? notice.optString("title", "") : "";
                String currMessage = notice != null ? notice.optString("message", "") : "";
                boolean currActive = notice != null && notice.optBoolean("active", true);

                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;

                    boolean isDark = LocaleHelper.isDarkMode(this);
                    ScrollView scroll = new ScrollView(this);
                    LinearLayout box = new LinearLayout(this);
                    box.setOrientation(LinearLayout.VERTICAL);
                    box.setPadding(dp(20), dp(16), dp(20), dp(16));
                    scroll.addView(box);

                    TextView subTitle = createTextView(
                        currentLang.equals("mr") 
                            ? "ही सूचना सर्व ग्राहकांच्या मुख्य पृष्ठावर (Home Page) सर्वात वर दिसेल. (फक्त मालक बदलू शकतात)" 
                            : "This announcement notice will appear at the top of customer home page. (Only owner can edit)",
                        13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")
                    );
                    subTitle.setPadding(0, 0, 0, dp(14));
                    box.addView(subTitle);

                    box.addView(createTextView(currentLang.equals("mr") ? "सूचना शीर्षक (ऐच्छिक / Title):" : "Notice Title (Optional):", 13, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));
                    EditText titleInput = createFormInput(currentLang.equals("mr") ? "उदा. दिवाळी सुट्टी / नवीन स्टॉक / दर बदल" : "e.g. Festival Holiday / New Stock Available", currTitle, InputType.TYPE_CLASS_TEXT);
                    box.addView(titleInput);

                    box.addView(createTextView(currentLang.equals("mr") ? "सूचना संदेश (Message): *" : "Notice Message: *", 13, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));
                    EditText msgInput = new EditText(this);
                    msgInput.setHint(currentLang.equals("mr") ? "ग्राहकांसाठी सूचना येथे सविस्तर लिहा..." : "Write notice message for customers here...");
                    msgInput.setText(currMessage);
                    msgInput.setTextSize(14);
                    msgInput.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
                    msgInput.setHintTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
                    msgInput.setBackgroundResource(R.drawable.bg_card);
                    msgInput.setPadding(dp(14), dp(12), dp(14), dp(12));
                    msgInput.setMinLines(4);
                    msgInput.setMaxLines(8);
                    msgInput.setGravity(Gravity.TOP | Gravity.START);
                    msgInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                    LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
                    mlp.bottomMargin = dp(14);
                    box.addView(msgInput, mlp);

                    CheckBox activeCheck = new CheckBox(this);
                    activeCheck.setText(currentLang.equals("mr") ? "सूचना सक्रिय ठेवा (Show to customers)" : "Active (Visible to customers)");
                    activeCheck.setChecked(currActive);
                    activeCheck.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1E293B"));
                    activeCheck.setPadding(dp(6), 0, 0, 0);
                    LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
                    clp.bottomMargin = dp(16);
                    box.addView(activeCheck, clp);

                    AlertDialog dlg = new AlertDialog.Builder(this)
                        .setTitle(currentLang.equals("mr") ? "📢 दुकान सूचना फलक संपादक" : "📢 Shop Notice Board Editor")
                        .setView(scroll)
                        .setPositiveButton(currentLang.equals("mr") ? "सेव्ह करा" : "Save Notice", null)
                        .setNeutralButton(currentLang.equals("mr") ? "सूचना हटवा" : "Clear Notice", null)
                        .setNegativeButton(currentLang.equals("mr") ? "रद्द करा" : "Cancel", null)
                        .create();

                    dlg.setOnShowListener(d -> {
                        Button btnSave = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
                        btnSave.setTextColor(Color.parseColor("#E85D04"));
                        btnSave.setOnClickListener(v -> {
                            String titleStr = titleInput.getText().toString().trim();
                            String msgStr = msgInput.getText().toString().trim();
                            boolean isActive = activeCheck.isChecked();

                            if (isActive && msgStr.isEmpty()) {
                                Toast.makeText(this, currentLang.equals("mr") ? "कृपया सूचना संदेश प्रविष्ट करा." : "Please enter notice message.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            btnSave.setEnabled(false);
                            new Thread(() -> {
                                try {
                                    JSONObject body = new JSONObject();
                                    body.put("title", titleStr);
                                    body.put("message", msgStr);
                                    body.put("active", isActive);

                                    Api.request(this, "PUT", "/api/notice", body);
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, currentLang.equals("mr") ? "सूचना यशस्वीरीत्या सेव्ह केली!" : "Notice saved successfully!", Toast.LENGTH_SHORT).show();
                                        dlg.dismiss();
                                        if ("dash".equals(activeTab)) {
                                            loadOwnerDashboard();
                                        } else if ("home".equals(activeTab)) {
                                            loadCustomerHome();
                                        }
                                    });
                                } catch (Exception e) {
                                    runOnUiThread(() -> {
                                        btnSave.setEnabled(true);
                                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                                }
                            }).start();
                        });

                        Button btnClear = dlg.getButton(AlertDialog.BUTTON_NEUTRAL);
                        btnClear.setTextColor(Color.parseColor("#DC2626"));
                        btnClear.setOnClickListener(v -> {
                            new AlertDialog.Builder(this)
                                .setTitle(currentLang.equals("mr") ? "सूचना हटवा?" : "Clear Notice?")
                                .setMessage(currentLang.equals("mr") ? "तुम्हाला खात्री आहे की ही सूचना काढायची आहे?" : "Are you sure you want to deactivate/clear this notice?")
                                .setPositiveButton(currentLang.equals("mr") ? "होय, हटवा" : "Yes, Clear", (cd, cw) -> {
                                    new Thread(() -> {
                                        try {
                                            Api.request(this, "DELETE", "/api/notice", null);
                                            runOnUiThread(() -> {
                                                Toast.makeText(this, currentLang.equals("mr") ? "सूचना हटवली." : "Notice cleared.", Toast.LENGTH_SHORT).show();
                                                dlg.dismiss();
                                                if ("dash".equals(activeTab)) {
                                                    loadOwnerDashboard();
                                                } else if ("home".equals(activeTab)) {
                                                    loadCustomerHome();
                                                }
                                            });
                                        } catch (Exception ex) {
                                            runOnUiThread(() -> Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
                                        }
                                    }).start();
                                })
                                .setNegativeButton(currentLang.equals("mr") ? "रद्द" : "Cancel", null)
                                .show();
                        });
                    });

                    dlg.show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void loadCustomerBillsDownloadCenter() {
        activeTab = "bills_download_center";
        buildBottomNav();
        boolean isDark = LocaleHelper.isDarkMode(this);
        statusTextView.setText(currentLang.equals("mr") ? "ग्राहक बिले डाउनलोड केंद्र" : "Customer Bills Download Center");
        mainContentContainer.removeAllViews();

        // 1. Back Row with Header
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(12));

        TextView backBtn = createTextView("← " + (currentLang.equals("mr") ? "मागे" : "Back"), 14, true, Color.parseColor("#0284C7"));
        backBtn.setPadding(dp(8), dp(6), dp(12), dp(6));
        backBtn.setBackgroundResource(R.drawable.bg_card);
        backBtn.setOnClickListener(v -> loadSettingsView());
        headerRow.addView(backBtn);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setPadding(dp(12), 0, 0, 0);
        titleCol.addView(createTextView(currentLang.equals("mr") ? "ग्राहक बिले डाउनलोड केंद्र" : "Customer Bills Download Center", 18, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));
        titleCol.addView(createTextView(currentLang.equals("mr") ? "प्रत्येक खरेदीचे बिल (PDF) स्वतंत्र किंवा सर्व बिले एकत्र डाउनलोड करा" : "Download individual purchase bills (PDF) separately or all together", 12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
        headerRow.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1.0f));

        mainContentContainer.addView(headerRow);

        // 2. Global Quick Action Card: Download All Shop Recent Invoices
        LinearLayout quickActionCard = new LinearLayout(this);
        quickActionCard.setOrientation(LinearLayout.VERTICAL);
        quickActionCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        quickActionCard.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams qalp = new LinearLayout.LayoutParams(-1, -2);
        qalp.bottomMargin = dp(12);

        quickActionCard.addView(createTextView("🏪 " + (currentLang.equals("mr") ? "सर्व दुकानाची चालू बिले" : "All Shop Invoices"), 15, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));
        quickActionCard.addView(createTextView(currentLang.equals("mr") ? "सर्व ग्राहकांची नुकतीच तयार झालेली बिले एकाच एकत्र PDF मध्ये डाउनलोड करा." : "Download all recently generated shop sales bills together in a single combined PDF.", 12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));

        Button dlAllShopBtn = createStyledButton("📥 " + (currentLang.equals("mr") ? "सर्व दुकानाची बिले एकत्र PDF डाउनलोड करा" : "Download All Recent Shop Bills (Combined PDF)"), Color.parseColor("#16A34A"), () -> {
            Toast.makeText(this, currentLang.equals("mr") ? "सर्व बिले तयार करत आहे..." : "Generating combined bills PDF...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                try {
                    JSONObject sRes = Api.request(this, "GET", "/api/sales", null);
                    JSONArray allSales = sRes != null ? sRes.optJSONArray("sales") : null;
                    if (allSales == null || allSales.length() == 0) {
                        runOnUiThread(() -> Toast.makeText(this, "No bills found.", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    File pdf = PdfGenerator.generateCombinedBillsPdf(this, allSales, "Shravan_Traders_All_Bills");
                    runOnUiThread(() -> promptSharePdf(pdf, "Shravan Traders - All Recent Invoices PDF"));
                } catch (Exception ex) {
                    runOnUiThread(() -> Toast.makeText(this, "PDF Error: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        });
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, dp(42));
        dlp.topMargin = dp(8);
        quickActionCard.addView(dlAllShopBtn, dlp);
        mainContentContainer.addView(quickActionCard, qalp);

        // 3. Search Bar
        EditText searchBar = createFormInput(currentLang.equals("mr") ? "ग्राहक शोधा (नाव किंवा मोबाईल नंबर)..." : "Search Customer (Name or Mobile)...", "", InputType.TYPE_CLASS_TEXT);
        searchBar.setBackgroundResource(R.drawable.bg_card);
        searchBar.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, dp(46));
        slp.bottomMargin = dp(12);
        mainContentContainer.addView(searchBar, slp);

        // 4. Customer List Container
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        mainContentContainer.addView(listContainer, new LinearLayout.LayoutParams(-1, -2));

        listContainer.addView(createTextView(currentLang.equals("mr") ? "ग्राहक माहिती लोड करत आहे..." : "Loading customers...", 14, false, Color.parseColor("#64748B")));

        new Thread(() -> {
            try {
                JSONObject r = Api.request(this, "GET", "/api/customers/ledger", null);
                JSONArray ledger = r != null ? r.optJSONArray("ledger") : new JSONArray();
                if (ledger == null) ledger = new JSONArray();

                final JSONArray finalLedger = ledger;
                runOnUiThread(() -> {
                    renderDownloadCenterCustomers(finalLedger, searchBar.getText().toString().trim(), listContainer);

                    searchBar.addTextChangedListener(new TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                            renderDownloadCenterCustomers(finalLedger, s.toString().trim(), listContainer);
                        }
                        @Override public void afterTextChanged(Editable s) {}
                    });
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    listContainer.removeAllViews();
                    listContainer.addView(createTextView("Error loading customers: " + ex.getMessage(), 14, false, Color.parseColor("#DC2626")));
                });
            }
        }).start();
    }

    private void renderDownloadCenterCustomers(JSONArray ledger, String query, LinearLayout listContainer) {
        listContainer.removeAllViews();
        boolean isDark = LocaleHelper.isDarkMode(this);
        int count = 0;

        for (int i = 0; i < ledger.length(); i++) {
            JSONObject c = ledger.optJSONObject(i);
            if (c == null) continue;
            String name = c.optString("name", "Unknown");
            String mobile = c.optString("mobile", "");
            String cId = c.optString("id");

            if (!query.isEmpty()) {
                String q = query.toLowerCase(Locale.US);
                if (!name.toLowerCase(Locale.US).contains(q) && !mobile.contains(q)) {
                    continue;
                }
            }

            count++;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackgroundResource(R.drawable.bg_card);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = dp(10);

            // Title & Mobile
            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.addView(createTextView("👤 " + name, 15, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")), new LinearLayout.LayoutParams(0, -2, 1.0f));
            if (!mobile.isEmpty() && !"N/A".equalsIgnoreCase(mobile)) {
                titleRow.addView(createTextView("📞 " + mobile, 12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
            }
            card.addView(titleRow);

            int pCount = c.optInt("purchasesCount", 0);
            double billed = c.optDouble("totalBilled", 0);
            double paid = c.optDouble("totalPaid", 0);
            double due = c.optDouble("creditBalance", 0);

            // Summary Info
            String stats = "📦 " + pCount + " " + (currentLang.equals("mr") ? "खरेदी बिले" : "Bills") +
                           "  |  ₹" + String.format(Locale.US, "%.0f", billed) + " " + (currentLang.equals("mr") ? "बिल" : "Billed") +
                           "  |  ₹" + String.format(Locale.US, "%.0f", paid) + " " + (currentLang.equals("mr") ? "जमा" : "Paid");
            card.addView(createTextView(stats, 12, false, isDark ? Color.parseColor("#CBD5E1") : Color.parseColor("#475569")));

            TextView dueTv = createTextView((currentLang.equals("mr") ? "शिल्लक बाकी: ₹" : "Remaining Due: ₹") + String.format(Locale.US, "%.0f", due), 13, true, due > 0 ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A"));
            card.addView(dueTv);

            // Action Buttons
            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams brp = new LinearLayout.LayoutParams(-1, dp(38));
            brp.topMargin = dp(8);

            Button viewBillsBtn = createStyledButton("📂 " + (currentLang.equals("mr") ? "खरेदी बिले पहा" : "View Bills (Separate)"), Color.parseColor("#0284C7"), () -> {
                showCustomerInvoicesSheet(cId, name, mobile);
            });

            Button dlAllCustBillsBtn = createStyledButton("📥 " + (currentLang.equals("mr") ? "सर्व एकत्र PDF" : "All Bills Together"), Color.parseColor("#16A34A"), () -> {
                downloadCustomerBillsTogether(cId, name);
            });

            btnRow.addView(viewBillsBtn, new LinearLayout.LayoutParams(0, -1, 1.3f));
            View sp = new View(this); btnRow.addView(sp, new LinearLayout.LayoutParams(dp(8), 1));
            btnRow.addView(dlAllCustBillsBtn, new LinearLayout.LayoutParams(0, -1, 1.0f));

            card.addView(btnRow, brp);
            listContainer.addView(card, cp);
        }

        if (count == 0) {
            listContainer.addView(createTextView(currentLang.equals("mr") ? "कोणतेही ग्राहक सापडले नाहीत." : "No customers found.", 14, false, Color.parseColor("#64748B")));
        }
    }

    private void downloadCustomerBillsTogether(String customerId, String customerName) {
        Toast.makeText(this, currentLang.equals("mr") ? "सर्व बिले तयार करत आहे..." : "Generating combined bills...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String url = "/api/sales";
                if (customerId != null && !customerId.isEmpty()) {
                    url += "?customerId=" + customerId;
                }
                JSONObject res = Api.request(this, "GET", url, null);
                JSONArray sales = res != null ? res.optJSONArray("sales") : null;
                if (sales == null || sales.length() == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "No bills found for this customer.", Toast.LENGTH_SHORT).show());
                    return;
                }
                File pdf = PdfGenerator.generateCombinedBillsPdf(this, sales, customerName);
                runOnUiThread(() -> promptSharePdf(pdf, customerName + " - All Bills Combined PDF"));
            } catch (Exception ex) {
                runOnUiThread(() -> Toast.makeText(this, "Error generating combined PDF: " + ex.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showCustomerInvoicesSheet(String customerId, String customerName, String customerMobile) {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(16), dp(18), dp(16));
        box.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        box.addView(createTextView("👤 " + customerName + (customerMobile.isEmpty() || "N/A".equals(customerMobile) ? "" : " (" + customerMobile + ")"), 16, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));
        box.addView(createTextView(currentLang.equals("mr") ? "खरेदी बिले लोड करत आहे..." : "Loading purchase bills...", 13, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));

        AlertDialog d = new AlertDialog.Builder(this)
            .setTitle(currentLang.equals("mr") ? "खरेदी बिले (Individual Bills)" : "Purchase Bills (Individual & All)")
            .setView(box)
            .setPositiveButton(currentLang.equals("mr") ? "बंद करा" : "Close", null)
            .show();

        new Thread(() -> {
            try {
                String url = "/api/sales";
                if (customerId != null && !customerId.isEmpty()) {
                    url += "?customerId=" + customerId;
                }
                JSONObject res = Api.request(this, "GET", url, null);
                JSONArray sales = res != null ? res.optJSONArray("sales") : null;
                if (sales == null) sales = new JSONArray();

                final JSONArray finalSales = sales;
                runOnUiThread(() -> {
                    box.removeAllViews();

                    // Header
                    box.addView(createTextView("👤 " + customerName, 17, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")));
                    if (!customerMobile.isEmpty() && !"N/A".equals(customerMobile)) {
                        box.addView(createTextView("📞 " + customerMobile, 12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
                    }
                    box.addView(createTextView("Total Purchases: " + finalSales.length() + " Bills", 13, true, Color.parseColor("#0284C7")));

                    // Top Quick Action: Download All Together
                    Button dlTogetherBtn = createStyledButton("📥 " + (currentLang.equals("mr") ? "सर्व बिले एकत्र डाउनलोड करा (PDF)" : "Download All Bills Together (Combined PDF)"), Color.parseColor("#16A34A"), () -> {
                        downloadCustomerBillsTogether(customerId, customerName);
                    });
                    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, dp(40));
                    tlp.topMargin = dp(8);
                    tlp.bottomMargin = dp(10);
                    box.addView(dlTogetherBtn, tlp);

                    View div = new View(this);
                    div.setBackgroundColor(isDark ? Color.parseColor("#334155") : Color.parseColor("#E2E8F0"));
                    box.addView(div, new LinearLayout.LayoutParams(-1, dp(1)));

                    ScrollView scroll = new ScrollView(this);
                    LinearLayout list = new LinearLayout(this);
                    list.setOrientation(LinearLayout.VERTICAL);
                    list.setPadding(0, dp(8), 0, 0);
                    scroll.addView(list);
                    box.addView(scroll, new LinearLayout.LayoutParams(-1, dp(380)));

                    if (finalSales.length() == 0) {
                        list.addView(createTextView(currentLang.equals("mr") ? "या ग्राहकासाठी कोणतीही बिले आढळली नाहीत." : "No purchase bills recorded for this customer.", 14, false, Color.parseColor("#64748B")));
                        return;
                    }

                    for (int i = 0; i < finalSales.length(); i++) {
                        JSONObject s = finalSales.optJSONObject(i);
                        if (s == null) continue;

                        String inv = s.optString("invoiceNo", "ST-0000");
                        String dt = s.optString("createdAt", "");
                        if (dt.length() >= 10) dt = dt.substring(0, 10);
                        double total = s.optDouble("total", 0);
                        double paid = s.optDouble("paidAmount", 0);
                        double due = Math.max(0, total - paid);

                        // Items summary
                        StringBuilder itemsStr = new StringBuilder();
                        JSONArray items = s.optJSONArray("items");
                        if (items != null) {
                            for (int k = 0; k < items.length(); k++) {
                                JSONObject it = items.optJSONObject(k);
                                if (it != null) {
                                    if (itemsStr.length() > 0) itemsStr.append(", ");
                                    itemsStr.append(it.optString("nameMr", it.optString("nameEn", "Item")))
                                            .append(" x ").append((int)it.optDouble("quantity", 1));
                                }
                            }
                        }

                        LinearLayout sCard = new LinearLayout(this);
                        sCard.setOrientation(LinearLayout.VERTICAL);
                        sCard.setPadding(dp(12), dp(10), dp(12), dp(10));
                        sCard.setBackgroundResource(R.drawable.bg_card);
                        LinearLayout.LayoutParams scp = new LinearLayout.LayoutParams(-1, -2);
                        scp.bottomMargin = dp(8);

                        // Row 1: Invoice & Date
                        LinearLayout r1 = new LinearLayout(this);
                        r1.setOrientation(LinearLayout.HORIZONTAL);
                        r1.addView(createTextView("🧾 #" + inv, 14, true, isDark ? Color.WHITE : Color.parseColor("#1E293B")), new LinearLayout.LayoutParams(0, -2, 1.0f));
                        r1.addView(createTextView("📅 " + dt, 12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
                        sCard.addView(r1);

                        // Row 2: Items
                        if (itemsStr.length() > 0) {
                            sCard.addView(createTextView(itemsStr.toString(), 12, false, isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B")));
                        }

                        // Row 3: Amounts & Status
                        String amtText = "Billed: ₹" + String.format(Locale.US, "%.0f", total) +
                                         "  |  Paid: ₹" + String.format(Locale.US, "%.0f", paid);
                        if (due > 0) amtText += "  |  Due: ₹" + String.format(Locale.US, "%.0f", due);
                        else amtText += "  |  (Fully Paid)";
                        sCard.addView(createTextView(amtText, 12, true, due > 0 ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A")));

                        // Row 4: Download Individual Bill Button
                        Button dlBillBtn = createStyledButton("📄 " + (currentLang.equals("mr") ? "या खरेदीचे बिल PDF डाउनलोड करा" : "Download This Purchase Bill (PDF)"), Color.parseColor("#0284C7"), () -> {
                            try {
                                File pdf = PdfGenerator.generateInvoicePdf(this, s, customerName);
                                promptSharePdf(pdf, "Invoice #" + inv + " for " + customerName);
                            } catch (Exception ex) {
                                Toast.makeText(this, "Error generating bill PDF: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(36));
                        bp.topMargin = dp(6);
                        sCard.addView(dlBillBtn, bp);

                        list.addView(sCard, scp);
                    }
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    box.removeAllViews();
                    box.addView(createTextView("Error loading bills: " + ex.getMessage(), 14, false, Color.parseColor("#DC2626")));
                });
            }
        }).start();
    }

    private void showChangePasswordDialog() {
        boolean isDark = LocaleHelper.isDarkMode(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackgroundColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFF9F2"));

        EditText currInput = createFormInput("Current Password", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText newInput = createFormInput("New Password (min 6 chars)", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText confirmInput = createFormInput("Confirm New Password", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        CheckBox showPassCheck = new CheckBox(this);
        showPassCheck.setText("Show Passwords");
        showPassCheck.setTextSize(13);
        showPassCheck.setTextColor(Color.parseColor("#475569"));
        showPassCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int inputType = isChecked ? 
                (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) :
                (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            currInput.setInputType(inputType);
            newInput.setInputType(inputType);
            confirmInput.setInputType(inputType);
            currInput.setSelection(currInput.getText().length());
            newInput.setSelection(newInput.getText().length());
            confirmInput.setSelection(confirmInput.getText().length());
        });

        box.addView(createTextView("Current Password:", 12, false, Color.parseColor("#64748B")));
        box.addView(currInput);
        box.addView(createTextView("New Password:", 12, false, Color.parseColor("#64748B")));
        box.addView(newInput);
        box.addView(createTextView("Confirm New Password:", 12, false, Color.parseColor("#64748B")));
        box.addView(confirmInput);
        box.addView(showPassCheck);

        new AlertDialog.Builder(this)
                .setTitle("Change Account Password / पासवर्ड बदला")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save Password", (d, w) -> {
                    String cP = currInput.getText().toString().trim();
                    String nP = newInput.getText().toString().trim();
                    String cnP = confirmInput.getText().toString().trim();

                    if (cP.isEmpty() || nP.isEmpty() || cnP.isEmpty()) {
                        Toast.makeText(this, "All password fields are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (nP.length() < 6) {
                        Toast.makeText(this, "New password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!nP.equals(cnP)) {
                        Toast.makeText(this, "New password and confirmation do not match.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new Thread(() -> {
                        try {
                            JSONObject body = new JSONObject();
                            body.put("currentPassword", cP);
                            body.put("newPassword", nP);
                            body.put("confirmPassword", cnP);

                            JSONObject res = Api.request(this, "POST", "/api/auth/change-password", body);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Password changed successfully! Please log in again.", Toast.LENGTH_LONG).show();
                                Api.clearSession(this);
                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, e.getMessage() != null ? e.getMessage() : "Failed to change password.", Toast.LENGTH_LONG).show());
                        }
                    }).start();
                }).show();
    }
}
