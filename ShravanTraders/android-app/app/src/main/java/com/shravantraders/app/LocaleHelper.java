package com.shravantraders.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREF_NAME = "session";
    private static final String KEY_LANG = "language";
    private static final String KEY_THEME = "theme_mode"; // "light", "dark", "system"

    public static Context applyLocaleAndTheme(Context context) {
        String lang = getLanguage(context);
        String theme = getThemeMode(context);

        applyThemeMode(theme);
        return setLocale(context, lang);
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, "mr"); // Default Marathi
    }

    public static void setLanguage(Context context, String lang) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();
    }

    public static String getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, "light"); // Default Light mode for clear readability
    }

    public static void setThemeMode(Context context, String themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, themeMode).apply();
        applyThemeMode(themeMode);
    }

    public static boolean isDarkMode(Context context) {
        String mode = getThemeMode(context);
        if ("dark".equalsIgnoreCase(mode)) return true;
        if ("light".equalsIgnoreCase(mode)) return false;
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static void applyThemeMode(String themeMode) {
        if ("dark".equalsIgnoreCase(themeMode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("light".equalsIgnoreCase(themeMode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public static Context setLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        configuration.setLocale(locale);
        LocaleList localeList = new LocaleList(locale);
        LocaleList.setDefault(localeList);
        configuration.setLocales(localeList);

        return context.createConfigurationContext(configuration);
    }
}
