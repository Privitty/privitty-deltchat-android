package org.thoughtcrime.securesms.components;

import static org.thoughtcrime.securesms.util.Prefs.getTheme;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcMsg;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.PrivJNI;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.ThemeUtil;

public class ConversationItemFooter extends LinearLayout {

  private TextView            dateView;
  private ImageView           secureIndicatorView,securePrvIndicatorView;
  private ImageView           locationIndicatorView;
  private ImageView           imageview_file_state_indicator;
  private DeliveryStatusView  deliveryStatusView;
  private Integer             textColor = null;
  private PrivJNI             privJNI = null;
  public ConversationItemFooter(Context context) {
    super(context);
    privJNI = new PrivJNI(context);
    init(null);
  }

  public ConversationItemFooter(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    privJNI = new PrivJNI(context);
    init(attrs);
  }

  public ConversationItemFooter(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    privJNI = new PrivJNI(context);
    init(attrs);
  }

  private void init(@Nullable AttributeSet attrs) {
    inflate(getContext(), R.layout.conversation_item_footer, this);

    dateView              = findViewById(R.id.footer_date);
    secureIndicatorView   = findViewById(R.id.footer_secure_indicator);
    securePrvIndicatorView   = findViewById(R.id.footer_prv_indicator);
    imageview_file_state_indicator   = findViewById(R.id.imageview_file_state_indicator);
    locationIndicatorView = findViewById(R.id.footer_location_indicator);
    deliveryStatusView    = new DeliveryStatusView(findViewById(R.id.delivery_indicator));

    if (attrs != null) {
      TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ConversationItemFooter, 0, 0);
      textColor = typedArray.getInt(R.styleable.ConversationItemFooter_footer_text_color, getResources().getColor(R.color.core_white));
      setTextColor(textColor);
      typedArray.recycle();
    }
  }

  public void setMessageRecord(@NonNull DcMsg messageRecord) {
    presentDate(messageRecord);
    if(messageRecord.isSecure()) {
      secureIndicatorView.setVisibility(VISIBLE);
      if (privJNI.isChatPrivittyProtected(messageRecord.getChatId()) && (messageRecord.getType() == DcMsg.DC_MSG_FILE))
      {
        securePrvIndicatorView.setVisibility(VISIBLE);
        imageview_file_state_indicator.setVisibility(VISIBLE);

        int fileState = 0;
        if (messageRecord.isForwarded()) {
          fileState = privJNI.getFileForwardAccessState(messageRecord.getChatId(), messageRecord.getFile(), (messageRecord.getFromId() == DcContact.DC_CONTACT_ID_SELF));
        } else {
          fileState = privJNI.getFileAccessState(messageRecord.getChatId(), messageRecord.getFilename(), (messageRecord.getFromId() != DcContact.DC_CONTACT_ID_SELF));
        }

        if (fileState == PrivJNI.PRV_SPLITKEYS_STATE_TYPE_SPLITKEYS_ACTIVE) {
          // access allowed
          int resId = ThemeUtil.getThemeAttributeResourceId(getContext(), R.attr.file_allowed_icon);
          imageview_file_state_indicator.setImageResource(resId);
        } else if (fileState == PrivJNI.PRV_SPLITKEYS_STATE_TYPE_SPLITKEYS_REQUEST) {
          // access requested
          int resId = ThemeUtil.getThemeAttributeResourceId(getContext(), R.attr.file_inprogress_icon);
          imageview_file_state_indicator.setImageResource(resId);
        } else if ((fileState == PrivJNI.PRV_SPLITKEYS_STATE_TYPE_SPLITKEYS_BLOCKED) || (fileState == PrivJNI.PRV_SPLITKEYS_STATE_TYPE_SPLITKEYS_REVOKED) || (fileState == PrivJNI.PRV_SPLITKEYS_STATE_TYPE_SPLITKEYS_DELETED)) {
          // access blocked or expired or Revoked
          int resId = ThemeUtil.getThemeAttributeResourceId(getContext(), R.attr.file_disallowed_icon);
          imageview_file_state_indicator.setImageResource(resId);
        } else if ((fileState == PrivJNI.PRV_SPLITKEYS_STATE_TYPE_RELAY_SPLITKEY_TO_OWNER) || (fileState == PrivJNI.PRV_SPLITKEYS_STATE_TYPE_RELAY_SPLITKEY_TO_RECIPIENT)) {
          // Relayed message
          int resId = ThemeUtil.getThemeAttributeResourceId(getContext(), R.attr.file_forward_icon);
          imageview_file_state_indicator.setImageResource(resId);
        }
      }
      else
      {
        securePrvIndicatorView.setVisibility(GONE);
        imageview_file_state_indicator.setVisibility(GONE);
      }
    } else {
      secureIndicatorView.setVisibility(GONE);
    }
    locationIndicatorView.setVisibility(messageRecord.hasLocation() ? View.VISIBLE : View.GONE);
    presentDeliveryStatus(messageRecord);
  }

  private void setTextColor(int color) {
    dateView.setTextColor(color);
    secureIndicatorView.setColorFilter(color);
    locationIndicatorView.setColorFilter(color);
    deliveryStatusView.setTint(color);
  }

  private void presentDate(@NonNull DcMsg messageRecord) {
    dateView.forceLayout();
    dateView.setText(DateUtils.getExtendedRelativeTimeSpanString(getContext(), messageRecord.getTimestamp()));
  }

  private void presentDeliveryStatus(@NonNull DcMsg messageRecord) {
    // isDownloading is temporary and should be checked first.
    boolean isDownloading = messageRecord.getDownloadState() == DcMsg.DC_DOWNLOAD_IN_PROGRESS;

         if (isDownloading)                deliveryStatusView.setDownloading();
    else if (messageRecord.isFailed())     deliveryStatusView.setFailed();
    else if (!messageRecord.isOutgoing())  deliveryStatusView.setNone();
    else if (messageRecord.isRemoteRead()) deliveryStatusView.setRead();
    else if (messageRecord.isDelivered())  deliveryStatusView.setSent();
    else if (messageRecord.isPreparing())  deliveryStatusView.setPreparing();
    else                                   deliveryStatusView.setPending();

    if (messageRecord.isFailed()) {
      deliveryStatusView.setTint(Color.RED);
    } else {
      deliveryStatusView.setTint(textColor); // Reset the color to the standard color (because the footer is re-used in a RecyclerView)
    }
  }

  public String getDescription() {
      String desc = dateView.getText().toString();
      String deliveryDesc = deliveryStatusView.getDescription();
      if (!"".equals(deliveryDesc)) {
          desc += "\n" + deliveryDesc;
      }
      return desc;
  }
}
