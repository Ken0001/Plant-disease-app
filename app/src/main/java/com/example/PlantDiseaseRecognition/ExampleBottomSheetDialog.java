package com.example.PlantDiseaseRecognition;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;


public class ExampleBottomSheetDialog extends BottomSheetDialogFragment{

    private BottomSheetListener mListener;
    MainActivity main = new MainActivity();


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.button_sheet_layout, container, false);

        Button button1 = v.findViewById(R.id.button1);
        Button button2 = v.findViewById(R.id.button2);
        //Take picture
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onButtonClicked(1);

                dismiss();
            }
        });
        //Select from gallery
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onButtonClicked(2);
                dismiss();
            }
        });

        return v;
    }





    public interface BottomSheetListener {
        void onButtonClicked(int img_src);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (BottomSheetListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement BottomSheetListener");
        }
    }
}
