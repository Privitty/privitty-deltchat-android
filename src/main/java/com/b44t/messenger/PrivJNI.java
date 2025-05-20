package com.b44t.messenger;
import android.util.Log;
import com.b44t.messenger.PrivEvent;

import android.content.Context;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.DcHelper;

public class PrivJNI {

    public final static int PRV_EVENT_CREATE_NONE                               = 0;
    public final static int PRV_EVENT_CREATE_VAULT                              = 1;
    public final static int PRV_EVENT_DEINIT                                    = 2;
    public final static int PRV_EVENT_ABORT                                     = 3;
    public final static int PRV_EVENT_SHUTDOWN                                  = 4;
    public final static int PRV_EVENT_ADD_NEW_PEER                              = 5;
    public final static int PRV_EVENT_RECEIVED_PEER_PDU                         = 6;
    public final static int PRV_EVENT_STOP_RENDERING                            = 7;
    public final static int PRV_EVENT_PEER_OFFLINE                              = 8;
    public final static int PRV_EVENT_PEER_TIMEOUT_REACHED                      = 9;
    public final static int PRV_EVENT_FILE_SANITY_FAILED                        = 10;
    /*
     * NOTE: Add any event above PRV_EVENT_LAST and update PRV_EVENT_LAST
     */ 
    public final static int PRV_EVENT_LAST                                      = 11;


    public final static int PRV_APP_STATUS_ERROR                                = 0;
    public final static int PRV_APP_STATUS_FAILED                               = 1;
    public final static int PRV_APP_STATUS_INVALID_REQUEST                      = 2;
    public final static int PRV_APP_STATUS_VAULT_IS_READY                       = 3;
    public final static int PRV_APP_STATUS_VAULT_FAILED                         = 4;
    public final static int PRV_APP_STATUS_USER_ALREADY_EXISTS                  = 5;
    public final static int PRV_APP_STATUS_USER_DOESNOT_EXISTS                  = 6;
    public final static int PRV_APP_STATUS_PEER_ALREADY_ADDED                   = 7;
    public final static int PRV_APP_STATUS_SEND_PEER_PDU                        = 8;
    public final static int PRV_APP_STATUS_PEER_ADD_ACCEPTED                    = 9;
    public final static int PRV_APP_STATUS_PEER_ADD_COMPLETE                    = 10;
    public final static int PRV_APP_STATUS_PEER_ADD_CONCLUDED                   = 11;
    public final static int PRV_APP_STATUS_PEER_ADD_PENDING                     = 12;
    public final static int PRV_APP_STATUS_PEER_BLOCKED                         = 13;
    public final static int PRV_APP_STATUS_FILE_ENCRYPTED                       = 14;
    public final static int PRV_APP_STATUS_FILE_ENCRYPTION_FAILED               = 15;
    public final static int PRV_APP_STATUS_FILE_DECRYPTION_FAILED               = 16;
    public final static int PRV_APP_STATUS_INVALID_FILE                         = 17;
    public final static int PRV_APP_STATUS_FILE_INACCESSIBLE                    = 18;
    public final static int PRV_APP_STATUS_AWAITING_PEER_AUTH                   = 19;
    public final static int PRV_APP_STATUS_PEER_SPLITKEYS_REQUEST               = 20;
    public final static int PRV_APP_STATUS_PEER_SPLITKEYS_RESPONSE              = 21;
    public final static int PRV_APP_STATUS_PEER_SPLITKEYS_REVOKED               = 22;
    public final static int PRV_APP_STATUS_PEER_OTSP_SPLITKEYS                  = 23;
    public final static int PRV_APP_STATUS_DELETE_CHAT                          = 24;
    public final static int PRV_APP_STATUS_GROUP_ALREADY_EXISTS                 = 25;
    public final static int PRV_APP_STATUS_GROUP_ADD_ACCEPTED                   = 26;
    public final static int PRV_APP_STATUS_FORWARD_PDU                          = 27;
    public final static int PRV_APP_STATUS_FORWARD_SPLITKEYS_REQUEST            = 28;
    public final static int PRV_APP_STATUS_RELAY_FORWARD_SPLITKEYS_REQUEST      = 29;
    public final static int PRV_APP_STATUS_REVERT_FORWARD_SPLITKEYS_REQUEST     = 30;
    public final static int PRV_APP_STATUS_RELAY_BACKWARD_SPLITKEYS_RESPONSE    = 31;
    public final static int PRV_APP_STATUS_PEER_SPLITKEYS_DELETED               = 32;
    public final static int PRV_APP_STATUS_PEER_SPLITKEYS_UNDO_REVOKED          = 33;

    /*
     * NOTE: Add any event above PRV_APP_STATUS_LIB_LAST and update PRV_APP_STATUS_LIB_LAST
     */ 
    public final static int PRV_APP_STATUS_LIB_LAST                             = 34;


    /*
     * NOTE: Splitkeys status types
     */
    public final static int PRV_SPLITKEYS_STATE_TYPE_NONE                       = 0;
    public final static int PRV_SPLITKEYS_STATE_TYPE_SPLITKEYS_ACTIVE           = 1;
    public final static int PRV_SPLITKEYS_STATE_TYPE_SPLITKEYS_REQUEST          = 2;
    public final static int PRV_SPLITKEYS_STATE_TYPE_SPLITKEYS_REVOKED          = 3;
    public final static int PRV_SPLITKEYS_STATE_TYPE_SPLITKEYS_BLOCKED          = 4;
    public final static int PRV_SPLITKEYS_STATE_TYPE_SPLITKEYS_DELETED          = 5;

    /*
     * Static Privitty strings
     */
    public final static String PRV_DB_CONFIG_GENERAL_ACCESS_TIME                = "general_access_time";

    private Context context = null;
    public PrivJNI(Context context) {
        this.context = context;
    }

    public native String version();
    public native boolean setConfiguration(String key, String value);
    public native void startEventLoop(String path);
    public native void stopConsumer();
    public native void produceEvent(PrivEvent event);
    public native String encryptFile(int chatId, String path, String filename);
    public native String freshOtsp(int chatId, String path);
    public native String decryptFile(int chatId, String path, String filename, boolean direction);
    public native boolean isPeerAdded(int chatId);
    public native void addMessage(int msg_id, int chat_id, int p_id, String msg_text, String msg_type,
                                  String media_path, String file_name, int file_session_timeout,
                                  int can_download, int can_forward, int num_peer_sss_request,
                                  String forwarded_to, int sent_privitty_protected);
    public native boolean isChatPrivittyProtected(int chatId);
    public native boolean cleanChat(int chatId);
    public native boolean cleanMsgs(int chatId, String[] filenames);
    public native boolean revokeMsgs(int chatId, String filenames);
    public native boolean isChatVersion(String mime_header);
    public native int getFileAccessState(int chat_id, String file_name, boolean direction);
    public native int getFileForwardAccessState(int chat_id, String file_path, boolean direction);
    public native boolean canDownloadFile(int chat_id, String file_name, boolean direction);
    public native boolean canForwardFile(int chat_id, String file_name, boolean direction);     // Direction: Incoming or Outgoing message
    /*
     * Set the file attributes for a file that owner is sharing in the UI.
     */
    public native void setFileAttributes(int chat_id, String prvFilepath, boolean direction,
                                         boolean download_file, boolean forward_file, int access_time);
    /*
     * Add peer request to whom this file is being forwarded.
     */
    public native Result forwardPeerAdd(int source_chat_id, int recipient_chat_id, String recipient_name,
                                        String file_path, String file_name, boolean direction);
    public native String decryptForwardedFile(int chat_id, String filePath, String fileName, boolean outgoing);
    /*
     * Set the file attributes for a `forwarded` file by the file owner in the UI
     */
    public native void setFileForwardAttributes(int chat_id, int recipient_id, String file_name,
                                                boolean direction, boolean download_file, boolean forward_file,
                                                int access_time);
    public native byte[] createChatGroup(int chatId, String ChatGroupName);

    public static class Result {
      public boolean success;
      public String filepath;

      public Result(boolean success, String filepath) {
        this.success = success;
        this.filepath = filepath;
      }
  }
}


