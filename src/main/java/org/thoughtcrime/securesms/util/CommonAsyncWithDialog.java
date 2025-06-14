package org.thoughtcrime.securesms.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class CommonAsyncWithDialog {

  private static final Executor executor = Executors.newSingleThreadExecutor();
  private static final Handler uiHandler = new Handler(Looper.getMainLooper());

  private final Context context;
  private final String initialTitle;
  private final String initialMessage;

  private ProgressDialog progressDialog;

  public CommonAsyncWithDialog(Context context, String title, String message) {
    this.context = context;
    this.initialTitle = title;
    this.initialMessage = message;
  }

  public void execute() {
    // Show progress dialog on UI thread
    uiHandler.post(new Runnable() {
      @Override
      public void run() {
        showProgressDialog();
      }
    });

    // Run background task
    executor.execute(new Runnable() {
      @Override
      public void run() {
        doInBackground();

        // When done, dismiss dialog and call onPostExecute
        uiHandler.post(new Runnable() {
          @Override
          public void run() {
            if (progressDialog != null && progressDialog.isShowing()) {
              progressDialog.dismiss();
            }
            onPostExecute();
          }
        });
      }
    });
  }

  // Runs in background thread
  protected abstract void doInBackground();

  // Runs on UI thread after background completes
  protected void onPostExecute() {
    // Optional override
  }

  // Update progress message safely on UI thread
  public void publishProgress(final String message) {
    uiHandler.post(new Runnable() {
      @Override
      public void run() {
        if (progressDialog != null && progressDialog.isShowing()) {
          progressDialog.setMessage(message);
        }
      }
    });
  }

  // Show a final result dialog
  protected void showFinalDialog(String title, String message) {
    new AlertDialog.Builder(context)
      .setTitle(title)
      .setMessage(message)
      .setPositiveButton("OK", null)
      .show();
  }

  // Internal: Show the loading dialog
  private void showProgressDialog() {
    progressDialog = new ProgressDialog(context);
    progressDialog.setTitle(initialTitle);
    progressDialog.setMessage(initialMessage);
    progressDialog.setCancelable(false);
    progressDialog.show();
  }
}
