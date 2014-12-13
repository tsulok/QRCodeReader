package com.tsulok.qrcodereader;

public interface IQRFound {

    /**
     * Called when a new QR code is found
     * @param data which found
     */
    public void onFound(String data);
}
