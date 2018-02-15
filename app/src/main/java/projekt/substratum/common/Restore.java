/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.common;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import projekt.substratum.R;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.tabs.BootAnimationsManager;
import projekt.substratum.tabs.FontsManager;
import projekt.substratum.tabs.WallpapersManager;
import projekt.substratum.util.tabs.SoundUtils;
import projekt.substratum.util.views.Lunchbar;
import projekt.substratum.util.views.SheetDialog;

import static projekt.substratum.common.Internal.ALL_WALLPAPER;
import static projekt.substratum.common.Internal.BOOT_ANIMATION_APPLIED;
import static projekt.substratum.common.Internal.HOME_WALLPAPER;
import static projekt.substratum.common.Internal.LOCK_WALLPAPER;
import static projekt.substratum.common.Internal.SOUNDS_APPLIED;
import static projekt.substratum.common.References.APP_THEME;
import static projekt.substratum.common.References.AUTO_THEME;
import static projekt.substratum.common.References.DARK_THEME;
import static projekt.substratum.common.References.DATA_RESOURCE_DIR;
import static projekt.substratum.common.References.DEFAULT_THEME;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.VENDOR_DIR;
import static projekt.substratum.common.Resources.isBootAnimationSupported;
import static projekt.substratum.common.Resources.isFontsSupported;
import static projekt.substratum.common.Resources.isSoundsSupported;

public enum Restore {
    ;

    public static void invoke(Context context, Activity activity) {
        SheetDialog sheetDialog = new SheetDialog(activity);
        View sheetView = View.inflate(context, R.layout.restore_sheet_dialog, null);

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        String selectedTheme = prefs.getString(APP_THEME, DEFAULT_THEME);

        LinearLayout disable_all = sheetView.findViewById(R.id.disable_all);
        LinearLayout uninstall_all = sheetView.findViewById(R.id.uninstall_all);
        LinearLayout restore_bootanimation = sheetView.findViewById(R.id.restore_bootanimation);
        LinearLayout restore_system_font = sheetView.findViewById(R.id.restore_system_font);
        LinearLayout restore_sounds = sheetView.findViewById(R.id.restore_sounds);
        LinearLayout home_wallpaper = sheetView.findViewById(R.id.home_wallpaper);
        LinearLayout lock_wallpaper = sheetView.findViewById(R.id.lock_wallpaper);
        LinearLayout both_wallpapers = sheetView.findViewById(R.id.both_wallpapers);

        // Doing this because for some reason the dark theme does not take charge
        if (context.getResources().getBoolean(R.bool.forceAppDarkTheme) ||
                selectedTheme.equals(DARK_THEME) ||
                selectedTheme.equals(AUTO_THEME)) {
            Boolean isNight;
            if (selectedTheme.equals(AUTO_THEME)) {
                Calendar cal = Calendar.getInstance();
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                isNight = hour < 6 || hour > 18;
            } else {
                isNight = true;
            }

            if (isNight) {
                sheetView.setBackgroundColor(context.getColor(R.color
                        .bottom_sheet_dialog_background_night));

                ColorStateList icon_tint = new ColorStateList(
                        new int[][]{
                                new int[]{android.R.attr.state_checked},
                                new int[]{}
                        },
                        new int[]{
                                context.getColor(R.color.bottom_sheet_dialog_icons_night),
                                context.getColor(R.color.bottom_sheet_dialog_icons_night)
                        });

                int text_tint = context.getColor(R.color.bottom_sheet_dialog_text_night);

                TextView disable = disable_all.findViewById(R.id.disable_text);
                disable.setTextColor(text_tint);
                disable.setCompoundDrawableTintList(icon_tint);

                TextView uninstall = uninstall_all.findViewById(R.id.uninstall_text);
                uninstall.setTextColor(text_tint);
                uninstall.setCompoundDrawableTintList(icon_tint);

                TextView bootanimation =
                        restore_bootanimation.findViewById(R.id.restore_bootanimation_text);
                bootanimation.setTextColor(text_tint);
                bootanimation.setCompoundDrawableTintList(icon_tint);

                TextView sounds = restore_sounds.findViewById(R.id.restore_sounds_text);
                sounds.setTextColor(text_tint);
                sounds.setCompoundDrawableTintList(icon_tint);

                TextView fonts = restore_system_font.findViewById(R.id.restore_font_text);
                fonts.setTextColor(text_tint);
                fonts.setCompoundDrawableTintList(icon_tint);

                TextView home_wall = home_wallpaper.findViewById(R.id.restore_home_wallpaper_text);
                home_wall.setTextColor(text_tint);
                home_wall.setCompoundDrawableTintList(icon_tint);

                TextView lock_wall = lock_wallpaper.findViewById(R.id.restore_lock_wallpaper_text);
                lock_wall.setTextColor(text_tint);
                lock_wall.setCompoundDrawableTintList(icon_tint);

                TextView both_walls =
                        both_wallpapers.findViewById(R.id.restore_both_wallpapers_text);
                both_walls.setTextColor(text_tint);
                both_walls.setCompoundDrawableTintList(icon_tint);
            }
        }

        View view = activity.findViewById(R.id.drawer_container);

        if (!Systems.checkOMS(context)) {
            disable_all.setVisibility(View.GONE);
            uninstall_all.setVisibility(View.GONE);
        }
        if (!isBootAnimationSupported(context)) restore_bootanimation.setVisibility(View.GONE);
        if (!isFontsSupported(context)) restore_system_font.setVisibility(View.GONE);
        if (!isSoundsSupported(context)) restore_sounds.setVisibility(View.GONE);

        // Overlays
        disable_all.setOnClickListener(vi -> {
            new RestoreFunction(activity).execute(false);
            sheetDialog.hide();
        });
        uninstall_all.setOnClickListener(vi -> {
            new RestoreFunction(activity).execute(true);
            sheetDialog.hide();
        });


        // Boot Animations
        restore_bootanimation.setOnClickListener(view2 -> {
            new BootAnimationClearer(activity).execute();
            sheetDialog.hide();
        });

        // Fonts
        restore_system_font.setOnClickListener(view2 -> {
            if (Systems.checkThemeInterfacer(context) ||
                    Systems.checkSubstratumService(context)) {
                new FontsClearer(activity).execute("");
            }
            sheetDialog.hide();
        });

        // Sounds
        restore_sounds.setOnClickListener(view2 -> {
            if (Systems.checkThemeInterfacer(context) ||
                    Systems.checkSubstratumService(context)) {
                new SoundsClearer(activity).execute();
            }
            sheetDialog.hide();
        });

        // Wallpapers
        home_wallpaper.setOnClickListener(view2 -> {
            try {
                WallpapersManager.clearWallpaper(context, HOME_WALLPAPER);
                if (view != null) {
                    Lunchbar.make(view,
                            activity.getString(R.string.manage_wallpaper_home_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            } catch (IOException e) {
                Log.e(SUBSTRATUM_LOG,
                        "Failed to restore home screen wallpaper! " + e.getMessage());
            } catch (NullPointerException e) {
                Log.e(SUBSTRATUM_LOG,
                        "Cannot retrieve lock screen wallpaper! " + e.getMessage());
            }
            sheetDialog.hide();
        });
        lock_wallpaper.setOnClickListener(view2 -> {
            try {
                WallpapersManager.clearWallpaper(context, LOCK_WALLPAPER);
                if (view != null) {
                    Lunchbar.make(view,
                            activity.getString(R.string.manage_wallpaper_lock_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            } catch (IOException e) {
                Log.e(SUBSTRATUM_LOG,
                        "Failed to restore lock screen wallpaper!" + e.getMessage());
            }
            sheetDialog.hide();
        });
        both_wallpapers.setOnClickListener(view2 -> {
            try {
                WallpapersManager.clearWallpaper(context, ALL_WALLPAPER);
                if (view != null) {
                    Lunchbar.make(view,
                            activity.getString(R.string.manage_wallpaper_all_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            } catch (IOException e) {
                Log.e(SUBSTRATUM_LOG,
                        "Failed to restore wallpapers! " + e.getMessage());
            }
            sheetDialog.hide();
        });

        // Show!
        sheetDialog.setContentView(sheetView);
        sheetDialog.show();
    }


    /**
     * Uninstall overlays
     */
    private static class RestoreFunction extends AsyncTask<Boolean, Void, Void> {
        ArrayList<String> final_commands_array;
        private WeakReference<Activity> ref;
        private boolean withUninstall;
        private ProgressDialog alertDialog;

        private RestoreFunction(Activity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            Activity activity = ref.get();
            if (activity != null) {
                alertDialog = new ProgressDialog(activity, R.style.RestoreDialog);
                alertDialog.setMessage(activity.getString(R.string.manage_dialog_performing));
                alertDialog.setIndeterminate(true);
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Activity activity = ref.get();
            if (activity != null) {
                Context context = activity.getApplicationContext();
                View view = activity.findViewById(R.id.drawer_container);
                if (alertDialog != null) alertDialog.dismiss();
                if (withUninstall) {
                    if (Systems.checkOMS(context)) {
                        try {
                            if (view != null) {
                                Lunchbar.make(view,
                                        context.getString(R.string
                                                .manage_system_overlay_uninstall_toast),
                                        Snackbar.LENGTH_LONG)
                                        .show();
                            }
                            //noinspection ConstantConditions
                            if (context != null) {
                                ThemeManager.uninstallOverlay(
                                        context, final_commands_array);
                            }
                        } catch (Exception e) {
                            // At this point the window is refreshed too many times detaching the
                            // activity
                            Log.e(SUBSTRATUM_LOG, "Profile window refreshed too " +
                                    "many times, restarting current activity to preserve app " +
                                    "integrity.");
                        }
                    } else {
                        if (view != null) {
                            Lunchbar.make(view,
                                    context.getString(R.string.abort_overlay_toast_success),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        AlertDialog.Builder alertDialogBuilder =
                                new AlertDialog.Builder(context);
                        alertDialogBuilder
                                .setTitle(context.getString(R.string
                                        .legacy_dialog_soft_reboot_title));

                        alertDialogBuilder
                                .setMessage(context.getString(R.string
                                        .legacy_dialog_soft_reboot_text));
                        alertDialogBuilder
                                .setPositiveButton(android.R.string.ok,
                                        (dialog, id) -> ElevatedCommands.reboot());
                        alertDialogBuilder.setCancelable(false);
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                } else {
                    if (view != null) {
                        Lunchbar.make(view,
                                context.getString(R.string.manage_system_overlay_toast),
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
            }
        }

        @Override
        protected Void doInBackground(Boolean... sUrl) {
            Activity activity = ref.get();
            if (activity != null) {
                Context context = activity.getApplicationContext();
                withUninstall = sUrl[0];
                if (context != null && withUninstall) {
                    if (Systems.checkOMS(context)) {
                        List<String> overlays = ThemeManager.listAllOverlays(context);

                        final_commands_array = new ArrayList<>();
                        final_commands_array.addAll(overlays.stream()
                                .filter(o -> Packages.getOverlayParent(context, o) != null)
                                .collect(Collectors.toList()));
                    } else {
                        FileOperations.mountRW();
                        FileOperations.mountRWData();
                        FileOperations.mountRWVendor();
                        FileOperations.bruteforceDelete(DATA_RESOURCE_DIR);
                        FileOperations.bruteforceDelete(LEGACY_NEXUS_DIR);
                        FileOperations.bruteforceDelete(PIXEL_NEXUS_DIR);
                        FileOperations.bruteforceDelete(VENDOR_DIR);
                        FileOperations.mountROVendor();
                        FileOperations.mountROData();
                        FileOperations.mountRO();
                    }
                } else {
                    ThemeManager.disableAllThemeOverlays(context);
                }
            }
            return null;
        }
    }

    /**
     * Clear applied boot animation
     */
    private static class BootAnimationClearer extends AsyncTask<Void, Void, Void> {
        private WeakReference<Activity> ref;
        private ProgressDialog alertDialog;

        private BootAnimationClearer(Activity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            Activity activity = ref.get();
            if (activity != null) {
                alertDialog = new ProgressDialog(activity, R.style.RestoreDialog);
                alertDialog.setMessage(activity.getString(R.string.manage_dialog_performing));
                alertDialog.setIndeterminate(true);
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            Activity activity = ref.get();
            if (activity != null) {
                View view = activity.findViewById(R.id.drawer_container);
                if (alertDialog != null) alertDialog.dismiss();

                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(
                                activity.getApplicationContext());

                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(BOOT_ANIMATION_APPLIED).apply();

                if (view != null) {
                    Lunchbar.make(view,
                            activity.getString(R.string.manage_bootanimation_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            Activity activity = ref.get();
            if (activity != null) {
                BootAnimationsManager.clearBootAnimation(activity, false);
            }
            return null;
        }
    }


    /**
     * Clear the applied fonts
     */
    private static class FontsClearer extends AsyncTask<String, Integer, String> {
        private WeakReference<Activity> ref;
        private ProgressDialog alertDialog;

        private FontsClearer(Activity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            Activity activity = ref.get();
            if (activity != null) {
                Context context = activity.getApplicationContext();
                if (References.ENABLE_EXTRAS_DIALOG) {
                    alertDialog = new ProgressDialog(context, R.style.RestoreDialog);
                    alertDialog.setMessage(context.getString(R.string.manage_dialog_performing));
                    alertDialog.setIndeterminate(true);
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Activity activity = ref.get();
            if (activity != null) {
                Context context = activity.getApplicationContext();
                if (References.ENABLE_EXTRAS_DIALOG && alertDialog != null) alertDialog.dismiss();

                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(
                                activity.getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(BOOT_ANIMATION_APPLIED).apply();
                editor.apply();

                if (Systems.checkSubstratumService(context) ||
                        Systems.checkThemeInterfacer(context)) {
                    Toast toast = Toast.makeText(
                            context,
                            R.string.manage_fonts_toast,
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Activity activity = ref.get();
            if (activity != null) {
                Context context = activity.getApplicationContext();
                FontsManager.clearFonts(context);
            }
            return null;
        }
    }

    /**
     * Clear all applied sounds
     */
    private static class SoundsClearer extends AsyncTask<Void, Void, Void> {
        private WeakReference<Activity> ref;
        private ProgressDialog alertDialog;

        private SoundsClearer(Activity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            Activity activity = ref.get();
            if (activity != null) {
                alertDialog = new ProgressDialog(activity, R.style.RestoreDialog);
                alertDialog.setMessage(activity.getString(R.string.manage_dialog_performing));
                alertDialog.setIndeterminate(true);
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            Activity activity = ref.get();
            if (activity != null) {
                View view = activity.findViewById(R.id.drawer_container);
                if (alertDialog != null) alertDialog.dismiss();

                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(
                                activity.getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(SOUNDS_APPLIED).apply();

                if (view != null) {
                    Lunchbar.make(view,
                            activity.getString(R.string.manage_sounds_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            Activity activity = ref.get();
            if (activity != null) {
                SoundUtils.SoundsClearer(activity);
            }
            return null;
        }
    }
}