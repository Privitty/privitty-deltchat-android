package org.thoughtcrime.securesms.connect;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.b44t.messenger.ActivityImageViewer;
import com.b44t.messenger.ActivityVideoViewer;
import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;
import com.b44t.messenger.DcMsg;
import com.b44t.messenger.rpc.Rpc;
import com.b44t.messenger.PrivJNI;
import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.LocalHelpActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.ShareActivity;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.qr.QrActivity;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.FileUtils;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.NetworkUtil;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import com.b44t.messenger.ActivityPDFViewer;

public class DcHelper {

  private static final String TAG = DcHelper.class.getSimpleName();

  public static final String CONFIG_ADDRESS = "addr";
  public static final String CONFIG_CONFIGURED_ADDRESS = "configured_addr";
  public static final String CONFIG_MAIL_SERVER = "mail_server";
  public static final String CONFIG_MAIL_USER = "mail_user";
  public static final String CONFIG_MAIL_PASSWORD = "mail_pw";
  public static final String CONFIG_MAIL_PORT = "mail_port";
  public static final String CONFIG_MAIL_SECURITY = "mail_security";
  public static final String CONFIG_SEND_SERVER = "send_server";
  public static final String CONFIG_SEND_USER = "send_user";
  public static final String CONFIG_SEND_PASSWORD = "send_pw";
  public static final String CONFIG_SEND_PORT = "send_port";
  public static final String CONFIG_SEND_SECURITY = "send_security";
  public static final String CONFIG_SERVER_FLAGS = "server_flags";
  public static final String CONFIG_DISPLAY_NAME = "displayname";
  public static final String CONFIG_SELF_STATUS = "selfstatus";
  public static final String CONFIG_SELF_AVATAR = "selfavatar";
  public static final String CONFIG_E2EE_ENABLED = "e2ee_enabled";
  public static final String CONFIG_QR_OVERLAY_LOGO = "qr_overlay_logo";
  public static final String CONFIG_INBOX_WATCH = "inbox_watch";
  public static final String CONFIG_SENTBOX_WATCH = "sentbox_watch";
  public static final String CONFIG_MVBOX_WATCH = "mvbox_watch";
  public static final String CONFIG_MVBOX_MOVE = "mvbox_move";
  public static final String CONFIG_ONLY_FETCH_MVBOX = "only_fetch_mvbox";
  public static final String CONFIG_BCC_SELF = "bcc_self";
  public static final String CONFIG_SHOW_EMAILS = "show_emails";
  public static final String CONFIG_MEDIA_QUALITY = "media_quality";
  public static final String CONFIG_WEBRTC_INSTANCE = "webrtc_instance";
  public static final String CONFIG_PROXY_ENABLED = "proxy_enabled";
  public static final String CONFIG_PROXY_URL = "proxy_url";
  public static final String CONFIG_VERIFIED_ONE_ON_ONE_CHATS = "verified_one_on_one_chats";
  public static final String CONFIG_WEBXDC_REALTIME_ENABLED = "webxdc_realtime_enabled";
  public static final String CONFIG_PRIVATE_TAG = "private_tag";

  public static DcContext getContext(@NonNull Context context) {
    return ApplicationContext.getInstance(context).dcContext;
  }

  public static PrivJNI getPriv(@NonNull Context context) {
    return ApplicationContext.getInstance(context).privJni;
  }

  public static Rpc getRpc(@NonNull Context context) {
    return ApplicationContext.getInstance(context).rpc;
  }

  public static DcAccounts getAccounts(@NonNull Context context) {
    return ApplicationContext.getInstance(context).dcAccounts;
  }

  public static DcEventCenter getEventCenter(@NonNull Context context) {
    return ApplicationContext.getInstance(context).eventCenter;
  }

  public static NotificationCenter getNotificationCenter(@NonNull Context context) {
    return ApplicationContext.getInstance(context).notificationCenter;
  }

  public static boolean hasAnyConfiguredContext(Context context) {
    DcAccounts accounts = getAccounts(context);
    int[] accountIds = accounts.getAll();
    for (int accountId : accountIds) {
      if (accounts.getAccount(accountId).isConfigured() == 1) {
        return true;
      }
    }
    return false;
  }

  public static boolean isConfigured(Context context) {
    DcContext dcContext = getContext(context);
    return dcContext.isConfigured() == 1;
  }

  public static String getSelfAddr(Context context) {
    DcContext dcContext = getContext(context);
    return dcContext.getConfig(CONFIG_CONFIGURED_ADDRESS);
  }

  public static int getInt(Context context, String key) {
    DcContext dcContext = getContext(context);
    return dcContext.getConfigInt(key);
  }

  public static String get(Context context, String key) {
    DcContext dcContext = getContext(context);
    return dcContext.getConfig(key);
  }

  @Deprecated public static int getInt(Context context, String key, int defaultValue) {
    return getInt(context, key);
  }

  @Deprecated public static String get(Context context, String key, String defaultValue) {
    return get(context, key);
  }

  public static void set(Context context, String key, String value) {
    DcContext dcContext = getContext(context);
    dcContext.setConfig(key, value);
  }

  public static void setStockTranslations(Context context) {
    DcContext dcContext = getContext(context);
    // the integers are defined in the core and used only here, an enum or sth. like that won't have a big benefit
    dcContext.setStockTranslation(1, context.getString(R.string.chat_no_messages));
    dcContext.setStockTranslation(2, context.getString(R.string.self));
    dcContext.setStockTranslation(3, context.getString(R.string.draft));
    dcContext.setStockTranslation(7, context.getString(R.string.voice_message));
    dcContext.setStockTranslation(9, context.getString(R.string.image));
    dcContext.setStockTranslation(10, context.getString(R.string.video));
    dcContext.setStockTranslation(11, context.getString(R.string.audio));
    dcContext.setStockTranslation(12, context.getString(R.string.file));
    dcContext.setStockTranslation(23, context.getString(R.string.gif));
    dcContext.setStockTranslation(24, context.getString(R.string.encrypted_message));
    dcContext.setStockTranslation(29, context.getString(R.string.systemmsg_cannot_decrypt));
    dcContext.setStockTranslation(35, context.getString(R.string.contact_verified));
    dcContext.setStockTranslation(36, context.getString(R.string.contact_not_verified));
    dcContext.setStockTranslation(37, context.getString(R.string.contact_setup_changed));
    dcContext.setStockTranslation(40, context.getString(R.string.chat_archived_label));
    dcContext.setStockTranslation(42, context.getString(R.string.autocrypt_asm_subject));
    dcContext.setStockTranslation(43, context.getString(R.string.autocrypt_asm_general_body));
    dcContext.setStockTranslation(60, context.getString(R.string.login_error_cannot_login));
    dcContext.setStockTranslation(66, context.getString(R.string.location));
    dcContext.setStockTranslation(67, context.getString(R.string.sticker));
    dcContext.setStockTranslation(68, context.getString(R.string.device_talk));
    dcContext.setStockTranslation(69, context.getString(R.string.saved_messages));
    dcContext.setStockTranslation(70, context.getString(R.string.device_talk_explain));
    dcContext.setStockTranslation(71, context.getString(R.string.device_talk_welcome_message2));
    dcContext.setStockTranslation(72, context.getString(R.string.systemmsg_unknown_sender_for_chat));
    dcContext.setStockTranslation(73, context.getString(R.string.systemmsg_subject_for_new_contact));
    dcContext.setStockTranslation(74, context.getString(R.string.systemmsg_failed_sending_to));
    dcContext.setStockTranslation(82, context.getString(R.string.videochat_invitation));
    dcContext.setStockTranslation(83, context.getString(R.string.videochat_invitation_body));
    dcContext.setStockTranslation(84, context.getString(R.string.configuration_failed_with_error));
    dcContext.setStockTranslation(85, context.getString(R.string.devicemsg_bad_time));
    dcContext.setStockTranslation(86, context.getString(R.string.devicemsg_update_reminder));
    dcContext.setStockTranslation(90, context.getString(R.string.reply_noun));
    dcContext.setStockTranslation(91, context.getString(R.string.devicemsg_self_deleted));
    dcContext.setStockTranslation(97, context.getString(R.string.forwarded));
    dcContext.setStockTranslation(98, context.getString(R.string.devicemsg_storage_exceeding));
    dcContext.setStockTranslation(99, context.getString(R.string.n_bytes_message));
    dcContext.setStockTranslation(100, context.getString(R.string.download_max_available_until));
    dcContext.setStockTranslation(103, context.getString(R.string.incoming_messages));
    dcContext.setStockTranslation(104, context.getString(R.string.outgoing_messages));
    dcContext.setStockTranslation(105, context.getString(R.string.storage_on_domain));
    dcContext.setStockTranslation(107, context.getString(R.string.connectivity_connected));
    dcContext.setStockTranslation(108, context.getString(R.string.connectivity_connecting));
    dcContext.setStockTranslation(109, context.getString(R.string.connectivity_updating));
    dcContext.setStockTranslation(110, context.getString(R.string.sending));
    dcContext.setStockTranslation(111, context.getString(R.string.last_msg_sent_successfully));
    dcContext.setStockTranslation(112, context.getString(R.string.error_x));
    dcContext.setStockTranslation(113, context.getString(R.string.not_supported_by_provider));
    dcContext.setStockTranslation(114, context.getString(R.string.messages));
    dcContext.setStockTranslation(115, context.getString(R.string.broadcast_list));
    dcContext.setStockTranslation(116, context.getString(R.string.part_of_total_used));
    dcContext.setStockTranslation(117, context.getString(R.string.secure_join_started));
    dcContext.setStockTranslation(118, context.getString(R.string.secure_join_replies));
    dcContext.setStockTranslation(119, context.getString(R.string.qrshow_join_contact_hint));

    dcContext.setStockTranslation(124, context.getString(R.string.group_name_changed_by_you));
    dcContext.setStockTranslation(125, context.getString(R.string.group_name_changed_by_other));
    dcContext.setStockTranslation(126, context.getString(R.string.group_image_changed_by_you));
    dcContext.setStockTranslation(127, context.getString(R.string.group_image_changed_by_other));
    dcContext.setStockTranslation(128, context.getString(R.string.add_member_by_you));
    dcContext.setStockTranslation(129, context.getString(R.string.add_member_by_other));
    dcContext.setStockTranslation(130, context.getString(R.string.remove_member_by_you));
    dcContext.setStockTranslation(131, context.getString(R.string.remove_member_by_other));
    dcContext.setStockTranslation(132, context.getString(R.string.group_left_by_you));
    dcContext.setStockTranslation(133, context.getString(R.string.group_left_by_other));
    dcContext.setStockTranslation(134, context.getString(R.string.group_image_deleted_by_you));
    dcContext.setStockTranslation(135, context.getString(R.string.group_image_deleted_by_other));
    dcContext.setStockTranslation(136, context.getString(R.string.location_enabled_by_you));
    dcContext.setStockTranslation(137, context.getString(R.string.location_enabled_by_other));
    dcContext.setStockTranslation(138, context.getString(R.string.ephemeral_timer_disabled_by_you));
    dcContext.setStockTranslation(139, context.getString(R.string.ephemeral_timer_disabled_by_other));
    dcContext.setStockTranslation(140, context.getString(R.string.ephemeral_timer_seconds_by_you));
    dcContext.setStockTranslation(141, context.getString(R.string.ephemeral_timer_seconds_by_other));
    dcContext.setStockTranslation(142, context.getString(R.string.ephemeral_timer_1_minute_by_you));
    dcContext.setStockTranslation(143, context.getString(R.string.ephemeral_timer_1_minute_by_other));
    dcContext.setStockTranslation(144, context.getString(R.string.ephemeral_timer_1_hour_by_you));
    dcContext.setStockTranslation(145, context.getString(R.string.ephemeral_timer_1_hour_by_other));
    dcContext.setStockTranslation(146, context.getString(R.string.ephemeral_timer_1_day_by_you));
    dcContext.setStockTranslation(147, context.getString(R.string.ephemeral_timer_1_day_by_other));
    dcContext.setStockTranslation(148, context.getString(R.string.ephemeral_timer_1_week_by_you));
    dcContext.setStockTranslation(149, context.getString(R.string.ephemeral_timer_1_week_by_other));
    dcContext.setStockTranslation(150, context.getString(R.string.ephemeral_timer_minutes_by_you));
    dcContext.setStockTranslation(151, context.getString(R.string.ephemeral_timer_minutes_by_other));
    dcContext.setStockTranslation(152, context.getString(R.string.ephemeral_timer_hours_by_you));
    dcContext.setStockTranslation(153, context.getString(R.string.ephemeral_timer_hours_by_other));
    dcContext.setStockTranslation(154, context.getString(R.string.ephemeral_timer_days_by_you));
    dcContext.setStockTranslation(155, context.getString(R.string.ephemeral_timer_days_by_other));
    dcContext.setStockTranslation(156, context.getString(R.string.ephemeral_timer_weeks_by_you));
    dcContext.setStockTranslation(157, context.getString(R.string.ephemeral_timer_weeks_by_other));

    // HACK: svg does not handle entities correctly and shows `&quot;` as the text `quot;`.
    // until that is fixed, we fix the most obvious errors (core uses encode_minimal, so this does not affect so many characters)
    // cmp. https://github.com/deltachat/deltachat-android/issues/2187
    dcContext.setStockTranslation(120, context.getString(R.string.qrshow_join_group_hint).replace("\"", ""));
    dcContext.setStockTranslation(121, context.getString(R.string.connectivity_not_connected));
    dcContext.setStockTranslation(122, context.getString(R.string.aeap_addr_changed));
    dcContext.setStockTranslation(123, context.getString(R.string.aeap_explanation));
    dcContext.setStockTranslation(162, context.getString(R.string.multidevice_qr_subtitle));
    dcContext.setStockTranslation(163, context.getString(R.string.multidevice_transfer_done_devicemsg));

    // The next two strings should only be set if the UI actually shows more info when the user clicks on the
    // DC_INFO_PROTECTION_{EN|DIS}ABLED info message
    dcContext.setStockTranslation(170, context.getString(R.string.chat_protection_enabled_tap_to_learn_more));
    dcContext.setStockTranslation(171, context.getString(R.string.chat_protection_broken_tap_to_learn_more));

    dcContext.setStockTranslation(172, context.getString(R.string.chat_new_group_hint));
    dcContext.setStockTranslation(173, context.getString(R.string.member_x_added));
    dcContext.setStockTranslation(174, context.getString(R.string.invalid_unencrypted_tap_to_learn_more));
    dcContext.setStockTranslation(176, context.getString(R.string.reaction_by_you));
    dcContext.setStockTranslation(177, context.getString(R.string.reaction_by_other));
    dcContext.setStockTranslation(190, context.getString(R.string.secure_join_wait));
    dcContext.setStockTranslation(191, context.getString(R.string.secure_join_wait_timeout));
  }

  public static File getImexDir() {
    // DIRECTORY_DOCUMENTS could be used but DIRECTORY_DOWNLOADS seems to be easier accessible by the user,
    // eg. "Download Managers" are nearly always installed.
    // CAVE: do not use DownloadManager to add the file as it is deleted on uninstall then ...
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
  }

  public static final HashMap<String, Integer> sharedFiles = new HashMap<>();

  public static void openForViewOrShare(Context activity, int msg_id, String cmd) {
    DcContext dcContext = getContext(activity);

    if (!(activity instanceof Activity)) {
      // would be nicer to accepting only Activity objects,
      // however, typically in Android just Context objects are passed around (as this normally does not make a difference).
      // Accepting only Activity here would force callers to cast, which would easily result in even more ugliness.
      Toast.makeText(activity, "openForViewOrShare() expects an Activity object", Toast.LENGTH_LONG).show();
      return;
    }

    DcMsg msg = dcContext.getMsg(msg_id);
    String path = msg.getFile();

    File srcP = new File(path);
    Log.d("JAVA-Privitty", "Message Subject: " + msg.getSubject());

    PrivJNI privJni = getPriv(activity);
    String prvFile;
    if (msg.getFromId() == DcContact.DC_CONTACT_ID_SELF) {
      if (msg.isForwarded()) {
        prvFile = privJni.decryptForwardedFile(msg.getChatId(), srcP.getParent(), srcP.getName(), true);
      } else {
        prvFile = privJni.decryptFile(msg.getChatId(), srcP.getParent(), srcP.getName(), true);
      }
    } else {
      if(!NetworkUtil.isInternetAvailable(activity))
      {
        Toast.makeText(activity, "Please connect to the internet to view the file content.", Toast.LENGTH_LONG).show();
        return;
      }
      if (msg.isForwarded()) {
        prvFile = privJni.decryptForwardedFile(msg.getChatId(), srcP.getParent(), srcP.getName(), false);
      } else {
        prvFile = privJni.decryptFile(msg.getChatId(), srcP.getParent(), srcP.getName(), false);
      }
    }
    Log.d("JAVA-Privitty", "File Name: " + prvFile);

    Intent intent = null;

    if (prvFile.equals("SPLITKEYS_EXPIRED") || prvFile.equals("SPLITKEYS_REQUESTED")) {
      Toast.makeText(activity, "Access expired, requesting again", Toast.LENGTH_LONG).show();
      return;
    } else if (prvFile.equals("SPLITKEYS_REQUESTING")) {
      Toast.makeText(activity, "Requesting access from Owner", Toast.LENGTH_LONG).show();
      return;
    } else if (prvFile.equals("SPLITKEYS_DELETED")) {
      Toast.makeText(activity, "Peer deleted the file !", Toast.LENGTH_LONG).show();
      return;
    } else if (prvFile.equals("SPLITKEYS_REVOKED")) {
      Toast.makeText(activity, "Access revoked !", Toast.LENGTH_LONG).show();
      return;
    } else if (prvFile.equals("SPLITKEYS_DENIED")) {
       Toast.makeText(activity, "Owner denied access!", Toast.LENGTH_LONG).show();
       return;
    } else if (prvFile.equals("FORWARD_DISAPPEARING_MESSAGES")) {
        Toast.makeText(activity, "Forwarded messages disappear once expired", Toast.LENGTH_LONG).show();
        return;
     } else if (prvFile.endsWith(".pdf")) {
      // Open pdf
      ActivityPDFViewer.prfFilePath = prvFile;
      if (msg.getFromId() != DcContact.DC_CONTACT_ID_SELF) {
        ActivityPDFViewer.blFlagAllowDownload = privJni.canDownloadFile(msg.getChatId(), srcP.getName(), false);
      } else {
        ActivityPDFViewer.blFlagAllowDownload = false;
      }
      intent = new Intent((Activity) activity, ActivityPDFViewer.class);
      activity.startActivity(intent);
    } else if (prvFile.endsWith(".png") || prvFile.endsWith(".jpg") || prvFile.endsWith(".jpeg")) {
      // Open Image
      ActivityImageViewer.prfFilePath = prvFile;
      if (msg.getFromId() != DcContact.DC_CONTACT_ID_SELF) {
        ActivityImageViewer.blFlagAllowDownload = privJni.canDownloadFile(msg.getChatId(), srcP.getName(), false);
      } else {
        ActivityImageViewer.blFlagAllowDownload = false;
      }
      intent = new Intent((Activity) activity, ActivityImageViewer.class);
      activity.startActivity(intent);
    } else if (prvFile.endsWith(".mp4") || prvFile.endsWith(".mov") || prvFile.endsWith(".mkv") ||
      prvFile.endsWith(".3gp")) {
      // Open Video
      ActivityVideoViewer.prfFilePath = prvFile;
      if (msg.getFromId() != DcContact.DC_CONTACT_ID_SELF) {
        ActivityVideoViewer.blFlagAllowDownload = privJni.canDownloadFile(msg.getChatId(), srcP.getName(), false);
      } else {
        ActivityVideoViewer.blFlagAllowDownload = false;
      }
      intent = new Intent((Activity) activity, ActivityVideoViewer.class);
      activity.startActivity(intent);
    } else if (prvFile.endsWith(".doc") || prvFile.endsWith(".docx") || prvFile.endsWith(".xlsx") ||
      prvFile.endsWith(".xlsx") || prvFile.endsWith(".pptx") || prvFile.endsWith(".ppt")) {
      // MS office
      openOfficeFile(prvFile, activity);
    } else {
      Toast.makeText(activity, "Unsupported file format", Toast.LENGTH_LONG).show();
    }


//    Log.i("JAVA-Privitty", "Native retunred file: " + path);
//
//    try {
//      File file = new File(path);
//      if (!file.exists()) {
//        Toast.makeText(activity, activity.getString(R.string.file_not_found, path), Toast.LENGTH_LONG).show();
//        return;
//      }
//
//      Uri uri;
//      if (path.startsWith(dcContext.getBlobdir())) {
//        uri = Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".attachments/" + Uri.encode(file.getName()));
//        sharedFiles.put("/" + file.getName(), 1); // as different Android version handle uris in putExtra differently, we also check them on our own
//      } else {
//        if (Build.VERSION.SDK_INT >= 24) {
//          uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileprovider", file);
//        } else {
//          uri = Uri.fromFile(file);
//        }
//      }
//      Log.i("JAVA-Privitty", "URI : " + uri);
//
//      if (cmd.equals(Intent.ACTION_VIEW)) {
//        mimeType = checkMime(path, mimeType);
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(uri, mimeType);
//        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        startActivity((Activity) activity, intent);
//      } else {
//        Intent intent = new Intent(Intent.ACTION_SEND);
//        intent.setType(mimeType);
//        intent.putExtra(Intent.EXTRA_STREAM, uri);
//        intent.putExtra(Intent.EXTRA_TEXT, msg.getText());
//        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.chat_share_with_title)));
//      }
//    } catch (RuntimeException e) {
//      Toast.makeText(activity, String.format("%s (%s)", activity.getString(R.string.no_app_to_handle_data), mimeType), Toast.LENGTH_LONG).show();
//      Log.i(TAG, "opening of external activity failed.", e);
//    }
  }

  private static void openOfficeFile(String fileName,Context activity)
  {
    File file = new File(fileName);
    if (!file.exists())
    {
      Toast.makeText(activity, "File not found: " + fileName, Toast.LENGTH_LONG).show();
      return;
    }

    Uri uri = FileProvider.getUriForFile(activity, "com.b44t.messenger.beta.fileprovider", file);

    // Get the MIME type based on file extension
    String mimeType = getMimeType(fileName);

    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(uri, mimeType);
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    try
    {
      activity.startActivity(Intent.createChooser(intent, "Open with"));
    }
    catch (ActivityNotFoundException e)
    {
      Toast.makeText(activity, "No app found to open this file", Toast.LENGTH_LONG).show();
    }
  }

  // Helper method to get the correct MIME type
  private static String getMimeType(String fileName) {
    if (fileName.endsWith(".docx"))
    {
      return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    } else if (fileName.endsWith(".doc")) {
      return "application/msword";
    } else if (fileName.endsWith(".xlsx")) {
      return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    } else if (fileName.endsWith(".xls")) {
      return "application/vnd.ms-excel";
    } else if (fileName.endsWith(".pptx")) {
      return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    } else if (fileName.endsWith(".ppt")) {
      return "application/vnd.ms-powerpoint";
    } else {
      return "/"; // Default type
    }
  }

  public static void sendToChat(Context activity, byte[] data, String mimeType, String fileName, String text) {
    Intent intent = new Intent(activity, ShareActivity.class);
    intent.setAction(Intent.ACTION_SEND);

    if (data != null) {
      Uri uri = PersistentBlobProvider.getInstance().create(activity, data, mimeType, fileName);
      intent.setType(mimeType);
      intent.putExtra(Intent.EXTRA_STREAM, uri);
      intent.putExtra(ShareActivity.EXTRA_TITLE, activity.getString(R.string.send_file_to, fileName));
    }

    if (text != null) {
      intent.putExtra(Intent.EXTRA_TEXT, text);
      if (data == null) {
        intent.putExtra(ShareActivity.EXTRA_TITLE, activity.getString(R.string.send_message_to));
      }
    }

    activity.startActivity(intent);
  }

  private static void startActivity(Activity activity, Intent intent) {
    // request for permission to install apks on API 26+ if intent mimetype is an apk
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
      "application/vnd.android.package-archive".equals(intent.getType()) &&
      !activity.getPackageManager().canRequestPackageInstalls()) {
      activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", activity.getPackageName()))));
      return;
    }
    activity.startActivity(intent);
  }

  private static String checkMime(String path, String mimeType) {
    if(mimeType == null || mimeType.equals("application/octet-stream")) {
      path = path.replaceAll(" ", "");
      String extension = MediaUtil.getFileExtensionFromUrl(path);
      String newType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
      if(newType != null) return newType;
    }
    return mimeType;
  }

  public static String getBlobdirFile(DcContext dcContext, String filename, String ext) {
    filename = FileUtils.sanitizeFilename(filename);
    ext = FileUtils.sanitizeFilename(ext);
    String outPath = null;
    for (int i = 0; i < 1000; i++) {
      String test = dcContext.getBlobdir() + "/" + filename + (i == 0 ? "" : i < 100 ? "-" + i : "-" + (new Date().getTime() + i)) + ext;
      if (!new File(test).exists()) {
        outPath = test;
        break;
      }
    }
    if(outPath==null) {
      // should not happen
      outPath = dcContext.getBlobdir() + "/" + Math.random();
    }
    return outPath;
  }

  public static String getBlobdirFile(DcContext dcContext, String path) {
    String filename = path.substring(path.lastIndexOf('/')+1); // is the whole path if '/' is not found (lastIndexOf() returns -1 then)
    String ext = "";
    int point = filename.indexOf('.');
    if(point!=-1) {
      ext = filename.substring(point);
      filename = filename.substring(0, point);
    }
    return getBlobdirFile(dcContext, filename, ext);

  }

  public static boolean isWebrtcConfigOk(DcContext dcContext) {
    String instance = dcContext.getConfig(DcHelper.CONFIG_WEBRTC_INSTANCE);
    return (instance != null && !instance.isEmpty());
  }

  @NonNull
  public static ThreadRecord getThreadRecord(Context context, DcLot summary, DcChat chat) { // adapted from ThreadDatabase.getCurrent()
    int chatId = chat.getId();

    String body = summary.getText1();
    if (!body.isEmpty()) {
      body += ": ";
    }
    body += summary.getText2();

    Recipient recipient = new Recipient(context, chat);
    long date = summary.getTimestamp();
    int unreadCount = getContext(context).getFreshMsgCount(chatId);

    return new ThreadRecord(body, recipient, date,
      unreadCount, chatId,
      chat.getVisibility(), chat.isProtected(), chat.isSendingLocations(), chat.isMuted(), chat.isContactRequest(), summary);
  }

  public static boolean isNetworkConnected(Context context) {
    try {
      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo netInfo = cm.getActiveNetworkInfo();
      if (netInfo != null && netInfo.isConnected()) {
        return true;
      }

    } catch (Exception e) {
    }
    return false;
  }

  /**
   * Gets a string you can show to the user with basic information about connectivity.
   * @param context
   * @param connectedString Usually "Connected", but when using this as the title in
   *                        ConversationListActivity, we want to write "Delta Chat"
   *                        or the user's display name there instead.
   * @return
   */
  public static String getConnectivitySummary(Context context, String connectedString) {
    int connectivity = getContext(context).getConnectivity();
    if (connectivity >= DcContext.DC_CONNECTIVITY_CONNECTED) {
      return connectedString;
    } else if (connectivity >= DcContext.DC_CONNECTIVITY_WORKING) {
      return context.getString(R.string.connectivity_updating);
    } else if (connectivity >= DcContext.DC_CONNECTIVITY_CONNECTING) {
      return context.getString(R.string.connectivity_connecting);
    } else {
      return context.getString(R.string.connectivity_not_connected);
    }
  }

  public static void showVerificationBrokenDialog(Context context, String name) {
    new AlertDialog.Builder(context)
      .setMessage(context.getString(R.string.chat_protection_broken_explanation, name))
      .setNeutralButton(R.string.learn_more, (d, w) -> openHelp(context, "#nocryptanymore"))
      .setNegativeButton(R.string.qrscan_title, (d, w) -> context.startActivity(new Intent(context, QrActivity.class)))
      .setPositiveButton(R.string.ok, null)
      .setCancelable(true)
      .show();
  }

  public static void showProtectionEnabledDialog(Context context) {
/*    new AlertDialog.Builder(context)
      .setMessage(context.getString(R.string.chat_protection_enabled_explanation))
      .setNeutralButton(R.string.learn_more, (d, w) -> openHelp(context, "#e2eeguarantee"))
      .setPositiveButton(R.string.ok, null)
      .setCancelable(true)
      .show();*/

    // Opening security details in external browser
    String url = "https://www.privittytech.com";
    Intent i = new Intent(Intent.ACTION_VIEW);
    i.setData(Uri.parse(url));
    context.startActivity(i);
  }

  public static void showInvalidUnencryptedDialog(Context context) {
    new AlertDialog.Builder(context)
      .setMessage(context.getString(R.string.invalid_unencrypted_explanation))
      .setNeutralButton(R.string.learn_more, (d, w) -> openHelp(context, "#howtoe2ee"))
      .setNegativeButton(R.string.qrscan_title, (d, w) -> context.startActivity(new Intent(context, QrActivity.class)))
      .setPositiveButton(R.string.ok, null)
      .setCancelable(true)
      .show();
  }

  public static void showEncryptionRequiredDialog(Context context, String addr) {
    new AlertDialog.Builder(context)
      .setMessage(context.getString(R.string.encryption_required_for_new_contact, addr))
      .setNeutralButton(R.string.learn_more, (d, w) -> openHelp(context, "#howtoe2ee"))
      .setNegativeButton(R.string.qrscan_title, (d, w) -> context.startActivity(new Intent(context, QrActivity.class)))
      .setPositiveButton(R.string.ok, null)
      .setCancelable(true)
      .show();
  }

  public static void openHelp(Context context, String section) {
    Intent intent = new Intent(context, LocalHelpActivity.class);
    if (section != null) { intent.putExtra(LocalHelpActivity.SECTION_EXTRA, section); }
    context.startActivity(intent);
  }
}
