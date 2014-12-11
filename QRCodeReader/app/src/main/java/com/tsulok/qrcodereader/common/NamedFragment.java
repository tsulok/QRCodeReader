package com.tsulok.qrcodereader.common;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class NamedFragment extends Fragment implements INamedFragment {

    public NamedFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(getLayoutId(), container, false);

        inflateObjects(v);
        initObjects(v);
        initEventHandlers(v);

        return v;
    }
}
