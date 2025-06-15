package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_VERIFIED_ONE_ON_ONE_CHATS;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_SHOW_EMAILS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcEventEmitter;
import com.b44t.messenger.rpc.Rpc;
import com.b44t.messenger.PrivJNI;
import com.b44t.messenger.PrivEvent;
import com.b44t.messenger.DcMsg;
import com.b44t.messenger.DcChat;
import com.facebook.drawee.backends.pipeline.Fresco;

import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.FetchWorker;
import org.thoughtcrime.securesms.connect.ForegroundDetector;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.connect.NetworkStateReceiver;
import org.thoughtcrime.securesms.crypto.DatabaseSecret;
import org.thoughtcrime.securesms.crypto.DatabaseSecretProvider;
import org.thoughtcrime.securesms.geolocation.DcLocationManager;
import org.thoughtcrime.securesms.jobmanager.JobManager;
import org.thoughtcrime.securesms.notifications.FcmReceiveService;
import org.thoughtcrime.securesms.notifications.InChatSounds;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.AndroidSignalProtocolLogger;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.SignalProtocolLoggerProvider;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ApplicationContext extends MultiDexApplication {
  private static final String TAG = ApplicationContext.class.getSimpleName();

  public static DcAccounts      dcAccounts;
  public Rpc                    rpc;
  public DcContext              dcContext;
  public DcLocationManager      dcLocationManager;
  public DcEventCenter          eventCenter;
  public NotificationCenter     notificationCenter;
  private JobManager            jobManager;
  public PrivJNI                privJni;

  private int                   debugOnAvailableCount;
  private int                   debugOnBlockedStatusChangedCount;
  private int                   debugOnCapabilitiesChangedCount;
  private int                   debugOnLinkPropertiesChangedCount;

  public static ApplicationContext getInstance(@NonNull Context context) {
    return (ApplicationContext)context.getApplicationContext();
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Fresco.initialize(getApplicationContext());

    // if (LeakCanary.isInAnalyzerProcess(this)) {
    //   // This process is dedicated to LeakCanary for heap analysis.
    //   // You should not init your app in this process.
    //   return;
    // }
    // LeakCanary.install(this);

    Log.i("DeltaChat", "++++++++++++++++++ ApplicationContext.onCreate() ++++++++++++++++++");

    System.loadLibrary("native-utils");
    System.loadLibrary("priv");

    privJni = new PrivJNI(getApplicationContext());
    registerMsgCallback();

    String curr_path = getFilesDir().getAbsolutePath(); // till files
    privJni.startEventLoop(curr_path);

    dcAccounts = new DcAccounts(new File(getFilesDir(), "accounts").getAbsolutePath());
    rpc = new Rpc(dcAccounts.getJsonrpcInstance());
    AccountManager.getInstance().migrateToDcAccounts(this);
    int[] allAccounts = dcAccounts.getAll();
    for (int accountId : allAccounts) {
      DcContext ac = dcAccounts.getAccount(accountId);
      if (!ac.isOpen()) {
        try {
          DatabaseSecret secret = DatabaseSecretProvider.getOrCreateDatabaseSecret(this, accountId);
          boolean res = ac.open(secret.asString());
          if (res) Log.i(TAG, "Successfully opened account " + accountId + ", path: " + ac.getBlobdir());
          else Log.e(TAG, "Error opening account " + accountId + ", path: " + ac.getBlobdir());
        } catch (Exception e) {
          Log.e(TAG, "Failed to open account " + accountId + ", path: " + ac.getBlobdir() + ": " + e);
          e.printStackTrace();
        }
      }
    }
    if (allAccounts.length == 0) {
      dcAccounts.addAccount();
    }
    dcContext = dcAccounts.getSelectedAccount();

    // Privitty Specific Configuration
    dcContext.setConfig(CONFIG_SHOW_EMAILS, "0");
    dcContext.setConfig("save_mime_headers", "1");

    notificationCenter = new NotificationCenter(this);
    eventCenter = new DcEventCenter(this);
    new Thread(() -> {
      DcEventEmitter emitter = dcAccounts.getEventEmitter();
      while (true) {
        DcEvent event = emitter.getNextEvent();
        if (event==null) {
          break;
        }

        if (event.getId() == DcContext.DC_EVENT_INCOMING_MSG) {
          DcMsg dcMsg = dcContext.getMsg(event.getData2Int());
          int chatId = event.getData1Int();

          Log.d("JAVA-Privitty", "isContactRequest: " + dcContext.getChat(chatId).isContactRequest());
          if ( ((dcMsg.showPadlock() == 1) && !dcContext.getChat(chatId).isContactRequest()) && Util.isValidJson(dcMsg.getSubject())) {
            try {
              // Encrypted or guaranteed E2E (using QR)
              Log.d("JAVA-Privitty", "isSecure(): " + dcMsg.showPadlock() + " isPeerAdded: " + privJni.isPeerAdded(chatId));
              JSONObject jSubject = new JSONObject(dcMsg.getSubject());
              if ("true".equalsIgnoreCase(jSubject.getString("privitty"))) {
                if ("new_peer_concluded".equalsIgnoreCase(jSubject.getString("type"))) {
                  Log.d("JAVA-Privitty", "Privitty message: new_peer_concluded, ignore it");
                  dcContext.deleteMsgs(new int[]{event.getData2Int()});
                  continue;
                } else if ("new_group_concluded".equalsIgnoreCase(jSubject.getString("type"))) {
                  Log.d("JAVA-Privitty", "Privitty message: new_group_concluded, ignore it");
                  dcContext.deleteMsgs(new int[]{event.getData2Int()});
                  continue;
                }
                Log.d("JAVA-Privitty", "Privitty message, punt it to libpriv. Sub: " + dcMsg.getSubject());
                PrivJNI privJni = DcHelper.getPriv(getApplicationContext());
                byte[] byteArrayMsg = Base64.getDecoder().decode(dcMsg.getText());
                PrivEvent jevent = new PrivEvent(PrivJNI.PRV_EVENT_RECEIVED_PEER_PDU, "", "",
                  dcMsg.getId(), dcMsg.getFromId(), event.getData1Int(),
                  "", "", "", 0, byteArrayMsg);
                privJni.produceEvent(jevent);

                dcContext.deleteMsgs(new int[]{event.getData2Int()});
                continue;
              }
            } catch (Exception e) {
              Log.e("JAVA-Privitty", "Exception in Privitty message: " + e.getMessage());
            }
          } else {
            if (!privJni.isChatVersion(dcContext.getMimeHeaders(event.getData2Int()))) {
              Log.d("JAVA-Privitty", "This message is from E-MAIL client");
              DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
              String user_msg = "Please install Privitty Chat App to secure your data";
              msg.setText(user_msg);
              int msgId = dcContext.sendMsg(chatId, msg);
            }
          }
        }

        eventCenter.handleEvent(event);
      }
      Log.i("DeltaChat", "shutting down event handler");
    }, "eventThread").start();

    rpc.start();

    // migrating global notifications pref. to per-account config, added  10/July/24
    final String NOTIFICATION_PREF = "pref_key_enable_notifications";
    boolean isMuted = !Prefs.getBooleanPreference(this, NOTIFICATION_PREF, true);
    if (isMuted) {
      for (int accId : dcAccounts.getAll()) {
        dcAccounts.getAccount(accId).setMuted(true);
      }
      Prefs.removePreference(this, NOTIFICATION_PREF);
    }
    // /migrating global notifications

    for (int accountId : allAccounts) {
      dcAccounts.getAccount(accountId).setConfig(CONFIG_VERIFIED_ONE_ON_ONE_CHATS, "1");
    }

    // set translations before starting I/O to avoid sending untranslated MDNs (issue #2288)
    DcHelper.setStockTranslations(this);

    dcAccounts.startIo();

    new ForegroundDetector(ApplicationContext.getInstance(this));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      ConnectivityManager connectivityManager =
        (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
      connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull android.net.Network network) {
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onAvailable() #" + debugOnAvailableCount++);
          dcAccounts.maybeNetwork();
        }

        @Override
        public void onBlockedStatusChanged(@NonNull android.net.Network network, boolean blocked) {
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onBlockedStatusChanged() #" + debugOnBlockedStatusChangedCount++);
        }

        @Override
        public void onCapabilitiesChanged(@NonNull android.net.Network network, NetworkCapabilities networkCapabilities) {
          // usually called after onAvailable(), so a maybeNetwork seems contraproductive
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onCapabilitiesChanged() #" + debugOnCapabilitiesChangedCount++);
        }

        @Override
        public void onLinkPropertiesChanged(@NonNull android.net.Network network, LinkProperties linkProperties) {
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onLinkPropertiesChanged() #" + debugOnLinkPropertiesChangedCount++);
        }
      });
    } // no else: use old method for debugging
    BroadcastReceiver networkStateReceiver = new NetworkStateReceiver();
    registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

    KeepAliveService.maybeStartSelf(this);

    initializeLogging();
    initializeJobManager();
    InChatSounds.getInstance(this);

    dcLocationManager = new DcLocationManager(this);
    DynamicTheme.setDefaultDayNightMode(this);

    IntentFilter filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
    registerReceiver(new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Util.localeChanged();
            DcHelper.setStockTranslations(context);
        }
    }, filter);

    // MAYBE TODO: i think the ApplicationContext is also created
    // when the app is stated by FetchWorker timeouts.
    // in this case, the normal threads shall not be started.
    Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
    PeriodicWorkRequest fetchWorkRequest = new PeriodicWorkRequest.Builder(
            FetchWorker.class,
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, // usually 15 minutes
            TimeUnit.MILLISECONDS,
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, // the start may be preferred by up to 5 minutes, so we run every 10-15 minutes
            TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build();
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FetchWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            fetchWorkRequest);
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

    if (Prefs.isPushEnabled(this)) {
      FcmReceiveService.register(this);
    } else {
      Log.i(TAG, "FCM disabled at build time");
    }
  }

  public JobManager getJobManager() {
    return jobManager;
  }

  private void initializeLogging() {
    SignalProtocolLoggerProvider.setProvider(new AndroidSignalProtocolLogger());
  }

  private void initializeJobManager() {
    this.jobManager = new JobManager(this, 5);
  }

  /*
   * C++ --> JAVA callback, to handle various status types.
   */
  public void onNativeMsgCallback(int chatId, int statusCode, int forwardToChatId, byte[] pdu) {

    if (statusCode == PrivJNI.PRV_APP_STATUS_VAULT_IS_READY) {
      Log.d("JAVA-Privitty", "Congratulations! Vault is created\n");

    } else if (statusCode == PrivJNI.PRV_APP_STATUS_SEND_PEER_PDU) {
      Log.d("JAVA-Privitty", "Send add new peer request to chatId:" + chatId);
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'new_peer_add'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(chatId, msg);
      });

    } else if (statusCode == PrivJNI.PRV_APP_STATUS_FORWARD_PDU) {
      Log.d("JAVA-Privitty", "Forward pdu to forwardToChatId:" + forwardToChatId);
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'forward_add_request'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(forwardToChatId, msg);
      });

    } else if (statusCode == PrivJNI.PRV_APP_STATUS_PEER_ADD_COMPLETE) {
      Log.d("JAVA-Privitty", "Congratulations! Add new peer handshake is complete with chatID:" + chatId);
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'new_peer_complete'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(chatId, msg);
      });
    } else if (statusCode == PrivJNI.PRV_APP_STATUS_PEER_ADD_CONCLUDED) {
      Log.d("JAVA-Privitty", "Congratulations! New peer concluded.");
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'new_peer_concluded'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(chatId, msg);
      });

    } else if (statusCode == PrivJNI.PRV_APP_STATUS_PEER_OTSP_SPLITKEYS) {
      Log.d("JAVA-Privitty", "Peer OTSP sent");
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'OTSP_SENT'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(chatId, msg);

        int fromId = msg.getFromId();
        String msgText = "OTSP_SENT";
        String msgType = "system";
        String mediaPath = "";
        String filename = "";
        int fileSessionTimeout = 0;
        int canDownload = 0;
        int canForward = 0;
        int numPeerSssRequest = 0;
        String forwardedTo = "";
        int sentPrivittyProtected = 0;

        privJni.addMessage(msgId, chatId, fromId, msgText, msgType, mediaPath, filename,
          fileSessionTimeout, canDownload, canForward,
          numPeerSssRequest, forwardedTo, sentPrivittyProtected);
      });
    } else if (statusCode == PrivJNI.PRV_APP_STATUS_PEER_SPLITKEYS_REQUEST) {
      Log.d("JAVA-Privitty", "Peer SPLITKEYS request");
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'SPLITKEYS_REQUEST'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(chatId, msg);
      });
    } else if (statusCode == PrivJNI.PRV_APP_STATUS_PEER_SPLITKEYS_RESPONSE) {
      Log.d("JAVA-Privitty", "Peer SPLITKEYS response");
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'SPLITKEYS_RESPONSE'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(chatId, msg);
      });
    } else if (statusCode == PrivJNI.PRV_APP_STATUS_PEER_SPLITKEYS_REVOKED) {
      Log.d("JAVA-Privitty", "Peer SPLITKEYS revoked");
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'SPLITKEYS_REVOKED'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(chatId, msg);
      });
      new Handler(Looper.getMainLooper()).post(() -> {
        Toast.makeText(getApplicationContext(), "You undo revoke", Toast.LENGTH_SHORT).show();
      });
    } else if (statusCode == PrivJNI.PRV_APP_STATUS_PEER_SPLITKEYS_UNDO_REVOKED) {
      Log.d("JAVA-Privitty", "Peer SPLITKEYS undo revoked");
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'SPLITKEYS_UNDO_REVOKED'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(chatId, msg);
      });
      new Handler(Looper.getMainLooper()).post(() -> {
        Toast.makeText(getApplicationContext(), "You revoked access", Toast.LENGTH_SHORT).show();
      });
    } else if (statusCode == PrivJNI.PRV_APP_STATUS_PEER_SPLITKEYS_DELETED) {
      Log.d("JAVA-Privitty", "Peer SPLITKEYS deleted");
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'SPLITKEYS_DELETED'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(chatId, msg);
      });
    } else if (statusCode == PrivJNI.PRV_APP_STATUS_GROUP_ADD_ACCEPTED) {
      Log.d("JAVA-Privitty", "Congratulations! New chat group is ready.");
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'new_group_concluded'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(chatId, msg);
      });
    } else if (statusCode == PrivJNI.PRV_APP_STATUS_FORWARD_SPLITKEYS_REQUEST ||
               statusCode == PrivJNI.PRV_APP_STATUS_REVERT_FORWARD_SPLITKEYS_REQUEST) {
      Log.d("JAVA-Privitty", "Forward/Revert Request: " + statusCode + " ChatId: " + chatId + " ForwardToChatId: " + forwardToChatId );
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'relay_message'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(chatId, msg);
      });
    } else if (statusCode == PrivJNI.PRV_APP_STATUS_RELAY_FORWARD_SPLITKEYS_REQUEST) {
      Log.d("JAVA-Privitty", "Relay request: " + statusCode + " ChatId: " + chatId + " ForwardToChatId: " + forwardToChatId );
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'relay_request'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(forwardToChatId, msg);
      });
    } else if (statusCode == PrivJNI.PRV_APP_STATUS_RELAY_BACKWARD_SPLITKEYS_RESPONSE) {
      Log.d("JAVA-Privitty", "Relay response: " + statusCode + " ChatId: " + chatId + " ForwardToChatId: " + forwardToChatId );
      Util.runOnAnyBackgroundThread(() -> {
        DcMsg msg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
        msg.setSubject("{'privitty':'true', 'type':'relay_response'}");
        String base64Msg = Base64.getEncoder().encodeToString(pdu);
        msg.setText(base64Msg);
        int msgId = dcContext.sendMsg(forwardToChatId, msg);
      });
    } else {
      Log.e("JAVA-Privitty", "StatusCode: " + statusCode);
    }
  }

  public native void registerMsgCallback();
}
