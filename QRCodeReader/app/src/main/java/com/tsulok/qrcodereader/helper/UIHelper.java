package com.tsulok.qrcodereader.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import com.tsulok.qrcodereader.App;

public class UIHelper {

    private static Toast toast;

    public static void makeToast(String message){
        if(toast != null){
            toast.cancel();
        }
        toast = Toast.makeText(App.getAppContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void makeToast(int messageId){
        makeToast(App.getAppContext().getResources().getString(messageId));
    }

    public static AlertDialog.Builder createDialog(final Activity activity){
        return new AlertDialog.Builder(activity)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                });
    }

    public static AlertDialog.Builder alert(Activity activity, String title){
        return createDialog(activity).setTitle(title);
    }

    public static AlertDialog.Builder alert(Activity activity, String title, String message){
        return createDialog(activity).setTitle(title).setMessage(message);
    }
}
