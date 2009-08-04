package com.android.mms.preferences;

import com.android.mms.R;
import com.android.mms.ui.MessagingPreferenceActivity;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class LedColorListPreference extends ListPreference implements
        OnSeekBarChangeListener {
    private Context mContext;
    private String mLedColor;
    private String mLedColorCustom;
    private SeekBar mRedSeekBar;
    private SeekBar mBlueSeekBar;
    private SeekBar mGreenSeekBar;
    private TextView mRedTextView;
    private TextView mBlueTextView;
    private TextView mGreenTextView;
    private Notification mNotification;
    private NotificationManager mNotificationManager;
    private boolean mDialogShowing;

    public LedColorListPreference(Context context) {
        super(context);
        mContext = context;
    }

    public LedColorListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(mContext);
            mLedColor = prefs
                    .getString(
                            MessagingPreferenceActivity.NOTIFICATION_LED_COLOR,
                            mContext
                                    .getString(R.string.pref_mms_notification_led_color_default));
            mLedColorCustom = prefs
                    .getString(
                            MessagingPreferenceActivity.NOTIFICATION_LED_CUSTOM,
                            mContext
                                    .getString(R.string.pref_mms_notification_led_color_default));
            if (mLedColor.equals("custom")) {
                showDialog();
            }
        }
    }

    private void showDialog() {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int color = Color.parseColor(mContext
                .getString(R.string.pref_mms_notification_led_color_default));
        try {
            color = Color.parseColor(mLedColorCustom);
        } catch (IllegalArgumentException e) {
            // No need to do anything here
        }
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        View v = inflater.inflate(R.layout.led_color_dialog, null);

        mRedSeekBar = (SeekBar) v.findViewById(R.id.RedSeekBar);
        mGreenSeekBar = (SeekBar) v.findViewById(R.id.GreenSeekBar);
        mBlueSeekBar = (SeekBar) v.findViewById(R.id.BlueSeekBar);

        mRedTextView = (TextView) v.findViewById(R.id.RedTextView);
        mGreenTextView = (TextView) v.findViewById(R.id.GreenTextView);
        mBlueTextView = (TextView) v.findViewById(R.id.BlueTextView);

        mRedSeekBar.setProgress(red);
        mGreenSeekBar.setProgress(green);
        mBlueSeekBar.setProgress(blue);

        mRedSeekBar.setOnSeekBarChangeListener(this);
        mGreenSeekBar.setOnSeekBarChangeListener(this);
        mBlueSeekBar.setOnSeekBarChangeListener(this);

        updateSeekBarTextView(mRedSeekBar);
        updateSeekBarTextView(mGreenSeekBar);
        updateSeekBarTextView(mBlueSeekBar);

        mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = new Notification();
        mNotification.ledOffMS = 0;
        mNotification.ledOnMS = 5000;
        mNotification.flags = Notification.FLAG_SHOW_LIGHTS;

        updateColorPreview();

        new AlertDialog.Builder(mContext).setIcon(
                android.R.drawable.ic_dialog_info).setTitle(
                R.string.pref_title_mms_notification_led_color).setView(v)
                .setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mDialogShowing = false;
                        mNotificationManager.cancel(0);
                    }
                }).setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                mDialogShowing = false;
                                mNotificationManager.cancel(0);
                            }
                        }).setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                int red = mRedSeekBar.getProgress();
                                int green = mGreenSeekBar.getProgress();
                                int blue = mBlueSeekBar.getProgress();
                                int color = Color.rgb(red, green, blue);

                                mDialogShowing = false;
                                mNotificationManager.cancel(0);
                                SharedPreferences prefs = PreferenceManager
                                        .getDefaultSharedPreferences(mContext);
                                SharedPreferences.Editor settings = prefs
                                        .edit();
                                settings
                                        .putString(
                                                MessagingPreferenceActivity.NOTIFICATION_LED_CUSTOM,
                                                "#"
                                                        + Integer
                                                                .toHexString(color));
                                settings.commit();

                                Toast
                                        .makeText(
                                                mContext,
                                                R.string.pref_mms_notification_led_color_custom_set,
                                                Toast.LENGTH_LONG).show();
                            }
                        }).show();
        mDialogShowing = true;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (mDialogShowing) {
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(mContext);
            mLedColor = prefs
                    .getString(
                            MessagingPreferenceActivity.NOTIFICATION_LED_COLOR,
                            mContext
                                    .getString(R.string.pref_mms_notification_led_color_default));
            mLedColorCustom = prefs
                    .getString(
                            MessagingPreferenceActivity.NOTIFICATION_LED_CUSTOM,
                            mContext
                                    .getString(R.string.pref_mms_notification_led_color_default));
            showDialog();
        }
    }

    @Override
    protected View onCreateDialogView() {
        mDialogShowing = false;
        return super.onCreateDialogView();
    }

    public void onProgressChanged(SeekBar seekbar, int progress,
            boolean fromTouch) {
        updateSeekBarTextView(seekbar, progress);
        updateColorPreview();
    }

    private void updateSeekBarTextView(SeekBar seekbar) {
        updateSeekBarTextView(seekbar, seekbar.getProgress());
    }

    private void updateSeekBarTextView(SeekBar seekbar, int progress) {
        if (seekbar.equals(mRedSeekBar)) {
            mRedTextView
                    .setText(mContext
                            .getString(R.string.pref_mms_notification_led_color_custom_dialog_red)
                            + " " + progress);
        } else if (seekbar.equals(mGreenSeekBar)) {
            mGreenTextView
                    .setText(mContext
                            .getString(R.string.pref_mms_notification_led_color_custom_dialog_green)
                            + " " + progress);
        } else if (seekbar.equals(mBlueSeekBar)) {
            mBlueTextView
                    .setText(mContext
                            .getString(R.string.pref_mms_notification_led_color_custom_dialog_blue)
                            + " " + progress);
        }
    }

    private void updateColorPreview() {
        int color = Color.rgb(mRedSeekBar.getProgress(), mGreenSeekBar
                .getProgress(), mBlueSeekBar.getProgress());
        mNotification.ledARGB = color;
        mNotificationManager.notify(0, mNotification);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
