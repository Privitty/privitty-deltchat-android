package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

public class NetworkUtil {

  public static boolean isInternetAvailable(Context context) {
    ConnectivityManager connectivityManager =
      (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    if (connectivityManager == null) return false;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      // For Android 6.0 and above
      Network network = connectivityManager.getActiveNetwork();
      if (network == null) return false;

      NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
      return capabilities != null &&
        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
          || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
          || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    } else {
      // For Android below 6.0
      NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
      return activeNetwork != null && activeNetwork.isConnected();
    }
  }
}
