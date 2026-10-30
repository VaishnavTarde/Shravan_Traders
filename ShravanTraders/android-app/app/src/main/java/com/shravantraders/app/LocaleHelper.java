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
        return "light";
    }

    public static void setThemeMode(Context context, String themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, "light").apply();
        applyThemeMode("light");
    }

    public static boolean isDarkMode(Context context) {
        return false;
    }

    private static void applyThemeMode(String themeMode) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
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
