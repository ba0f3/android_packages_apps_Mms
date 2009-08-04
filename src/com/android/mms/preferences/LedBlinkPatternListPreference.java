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

public class LedBlinkPatternListPreference extends ListPreference implements
        OnSeekBarChangeListener {
    private Context mContext;
    private String mLedColor;
    private String mLedBlinkPattern;
    private SeekBar mSeekBarOn;
    private TextView mSeekBarOnLabel;
    private TextView mSeekBarOffLabel;
    private SeekBar mSeekBarOff;
    private Notification mNotification;
    private NotificationManager mNotificationManager;
    private boolean mDialogShowing;

    public LedBlinkPatternListPreference(Context context) {
        super(context);
        mContext = context;
    }

    public LedBlinkPatternListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(mContext);
            mLedBlinkPattern = prefs.getString(MessagingPreferenceActivity.NOTIFICATION_LED_BLINK,
                    mContext.getString(R.string.pref_mms_notification_led_blink_default));
            if (mLedBlinkPattern.equals("custom")) {
                mLedColor = prefs.getString(
                        MessagingPreferenceActivity.NOTIFICATION_LED_COLOR,
                        mContext.getString(R.string.pref_mms_notification_led_color_default));
                if(mLedColor.equals("custom")) {
                    mLedColor = prefs.getString(MessagingPreferenceActivity.NOTIFICATION_LED_CUSTOM,
                            mContext.getString(R.string.pref_mms_notification_led_color_default));
                }
                mLedBlinkPattern = prefs.getString(MessagingPreferenceActivity.NOTIFICATION_LED_BLINK_CUSTOM,
                        mContext.getString(R.string.pref_mms_notification_led_blink_default));
                showDialog();
            }
        }
    }

    private void showDialog() {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.led_pattern_dialog, null);

        mSeekBarOn = (SeekBar) v.findViewById(R.id.SeekBarOn);
        mSeekBarOff = (SeekBar) v.findViewById(R.id.SeekBarOff);
        mSeekBarOnLabel = (TextView) v.findViewById(R.id.SeekBarOnMsLabel);
        mSeekBarOffLabel = (TextView) v.findViewById(R.id.SeekBarOffMsLabel);
        mSeekBarOn.setOnSeekBarChangeListener(this);
        mSeekBarOff.setOnSeekBarChangeListener(this);

        int[] pattern = MessagingPreferenceActivity.parseLEDPattern(mLedBlinkPattern);
        mSeekBarOn.setProgress(pattern[0]);
        mSeekBarOff.setProgress(pattern[1]);

        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = new Notification();
        mNotification.ledARGB = Color.parseColor(mLedColor);
        mNotification.ledOnMS = pattern[0];
        mNotification.ledOffMS = pattern[1];
        mNotification.flags = Notification.FLAG_SHOW_LIGHTS;
        mNotificationManager.notify(0, mNotification);

        new AlertDialog.Builder(mContext).setIcon(
                android.R.drawable.ic_dialog_info).setTitle(
                R.string.pref_mms_notification_led_blink_custom_dialog).setView(v)
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
                                String pattern = mSeekBarOnLabel.getText() + "," + mSeekBarOffLabel.getText();
                                mDialogShowing = false;
                                mNotificationManager.cancel(0);
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                                SharedPreferences.Editor settings = prefs.edit();
                                if (MessagingPreferenceActivity.parseLEDPattern(pattern) != null) {
                                    settings.putString(MessagingPreferenceActivity.NOTIFICATION_LED_BLINK_CUSTOM, pattern);
                                    settings.commit();
                                    Toast.makeText(mContext, mContext.getString(R.string.pref_mms_notification_led_blink_custom_set),
                                        Toast.LENGTH_LONG).show();
                                  } else {
                                    settings.putString(MessagingPreferenceActivity.NOTIFICATION_LED_BLINK_CUSTOM, mContext
                                        .getString(R.string.pref_mms_notification_led_blink_default));
                                    settings.commit();
                                    Toast.makeText(mContext, mContext.getString(R.string.pref_mms_notification_led_blink_custom_not_set),
                                        Toast.LENGTH_LONG).show();
                                  }
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
            mLedBlinkPattern = prefs.getString(MessagingPreferenceActivity.NOTIFICATION_LED_BLINK_CUSTOM,
                    mContext.getString(R.string.pref_mms_notification_led_blink_default));
            showDialog();
        }
    }

    @Override
    protected View onCreateDialogView() {
        mDialogShowing = false;
        return super.onCreateDialogView();
    }

    public void onProgressChanged(SeekBar seekbar, int progress, boolean fromTouch) {
        if (seekbar.equals(mSeekBarOn)) {
            mSeekBarOnLabel.setText(Integer.toString(progress));
        } else if (seekbar.equals(mSeekBarOff)) {
            mSeekBarOffLabel.setText(Integer.toString(progress));
        }
        try {
            mNotification.ledOnMS = mSeekBarOn.getProgress();
            mNotification.ledOffMS = mSeekBarOff.getProgress();
            mNotificationManager.notify(0, mNotification);
        } catch(NullPointerException e) {}
    }


    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
