package com.flyscale.alarms;

import com.flyscale.alarms.provider.Alarm;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class LabelDialogFragment extends DialogFragment{
	private static final String KEY_LABEL="label";
	private static final String KEY_ALARM="alarm";
	private static final String KEY_TAG="tag";
	
	private EditText mLabelBox;
	
	
	public static LabelDialogFragment newInstance(Alarm alarm,String label,String tag){
		final LabelDialogFragment frag=new LabelDialogFragment();
		Bundle args=new Bundle();
		args.putString(KEY_LABEL,label);
		args.putParcelable(KEY_ALARM, alarm);
		args.putSerializable(KEY_TAG, tag);
		frag.setArguments(args);
		return frag;
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, 0);
	}
	
	public View onCreateView(LayoutInflater inflater,ViewGroup container,
			Bundle savedInstance){
		Bundle bundle=getArguments();
		final String label=bundle.getString(KEY_LABEL);
		final Alarm alarm=bundle.getParcelable(KEY_ALARM);
		final String tag=bundle.getString(KEY_TAG);
		
		final View view=inflater.inflate(R.layout.label_dialog, container,false);
		mLabelBox=(EditText)view.findViewById(R.id.labelBox);
		mLabelBox.setText(label);
		mLabelBox.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId==EditorInfo.IME_ACTION_DONE){
					set(alarm,tag);
					return true;
				}
				return false;
			}
		});
		
		final Button cancelButton=(Button) view.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		
		final Button setButton=(Button) view.findViewById(R.id.setButton);
		setButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				set(alarm, tag);
			}
		});
		
		return view;
	}
	
	private void set(Alarm alarm,String tag){
		String label=mLabelBox.getText().toString();
		if(label.trim().length()==0){
			label="";
		}
		
		final Activity activity=getActivity();
		if(activity instanceof AlarmLabelDialogHandler){
			((MainActivity)getActivity()).onDialogLabelSet(alarm, label, tag);
		}
		dismiss();
	}
	
	interface AlarmLabelDialogHandler{
		void onDialogLabelSet(Alarm alarm,String label,String tag);
	}
}
