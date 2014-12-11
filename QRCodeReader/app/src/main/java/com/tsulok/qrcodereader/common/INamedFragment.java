package com.tsulok.qrcodereader.common;

import android.view.View;

public interface INamedFragment {
    /**
     * The layout of the fragment
     * @return The associated layout id
     */
    public int getLayoutId();

    /**
     * The title of the fragment
     * @return The associated string id
     */
    public int getNameId();

    /**
     * View typed fields should be initialized here.
     *
     * @param v  Root view (which will be returned in onCreateView)
     */
    public void inflateObjects(final View v);

    /**
     * Other (non view typed) fields (such as adapters) should be initialized here.
     *
     * @param v  Root view (which will be returned in onCreateView)
     */
    public void initObjects(final View v);

    /**
     * Event handlers of view typed fields (such as onClickListeners) should be initialized here.
     *
     * @param v  Root view (which will be returned in onCreateView)
     */
    public void initEventHandlers(final View v);
}
