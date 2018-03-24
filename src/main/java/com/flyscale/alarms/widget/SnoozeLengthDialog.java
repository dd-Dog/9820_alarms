package com.flyscale.alarms.widget;

import com.flyscale.alarms.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

public class SnoozeLengthDialog extends DialogPreference {
	private final Context mContext;
	private NumberPicker mNumberPickerView;
	private int mSnoozeMinutes;
	private static final String DEFAULT_SNOOZE_TIME = "10";

	public SnoozeLengthDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		setDialogLayoutResource(R.layout.snooze_length_picker);
		setTitle(R.string.snooze_duration_title);
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		mNumberPickerView = (NumberPicker) view
				.findViewById(R.id.minutes_picker);
		mNumberPickerView.setMaxValue(30);
		mNumberPickerView.setMinValue(1);
		mNumberPickerView.setValue(mSnoozeMinutes);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

	protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue) {
		String val;
		if (restorePersistedValue) {
			val = getPersistedString(DEFAULT_SNOOZE_TIME);
			if (val != null) {
				mSnoozeMinutes = Integer.parseInt(val);
			}
		} else {
			val = (String) defaultValue;
			if (val != null) {
				mSnoozeMinutes = Integer.parseInt(val);
			}
			persistString(val);
		}
	}

	public void setSummary() {
		setSummary(String.format(
				mContext.getResources()
						.getQuantityString(R.plurals.snooze_duration,
								mSnoozeMinutes).toString(), mSnoozeMinutes));
	}
	
	public int getCurrentValues(){
		return mSnoozeMinutes;
	}

	protected void onDialogClosed(boolean positiveResult){
		if(positiveResult){
			mNumberPickerView.clearFocus();
			mSnoozeMinutes=mNumberPickerView.getValue();
			persistString(Integer.toString(mSnoozeMinutes));
			setSummary();
		}
	}
	
}
