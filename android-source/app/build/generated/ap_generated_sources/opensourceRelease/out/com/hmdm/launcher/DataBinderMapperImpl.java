package com.hmdm.launcher;

import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import androidx.databinding.DataBinderMapper;
import androidx.databinding.DataBindingComponent;
import androidx.databinding.ViewDataBinding;
import com.hmdm.launcher.databinding.ActivityAdminBindingImpl;
import com.hmdm.launcher.databinding.ActivityErrorDetailsBindingImpl;
import com.hmdm.launcher.databinding.ActivityInitialSetupBindingImpl;
import com.hmdm.launcher.databinding.ActivityMainBindingImpl;
import com.hmdm.launcher.databinding.ActivityMdmChoiceBindingImpl;
import com.hmdm.launcher.databinding.DialogAccessibilityServiceBindingImpl;
import com.hmdm.launcher.databinding.DialogAdministratorModeBindingImpl;
import com.hmdm.launcher.databinding.DialogDeviceInfoBindingImpl;
import com.hmdm.launcher.databinding.DialogEnterDeviceIdBindingImpl;
import com.hmdm.launcher.databinding.DialogEnterPasswordBindingImpl;
import com.hmdm.launcher.databinding.DialogEnterServerBindingImpl;
import com.hmdm.launcher.databinding.DialogFileDownloadingFailedBindingImpl;
import com.hmdm.launcher.databinding.DialogHistorySettingsBindingImpl;
import com.hmdm.launcher.databinding.DialogManageStorageBindingImpl;
import com.hmdm.launcher.databinding.DialogMiuiPermissionsBindingImpl;
import com.hmdm.launcher.databinding.DialogNetworkErrorBindingImpl;
import com.hmdm.launcher.databinding.DialogOverlaySettingsBindingImpl;
import com.hmdm.launcher.databinding.DialogPermissionsBindingImpl;
import com.hmdm.launcher.databinding.DialogSystemSettingsBindingImpl;
import com.hmdm.launcher.databinding.DialogUnknownSourcesBindingImpl;
import com.hmdm.launcher.databinding.ItemAppBindingImpl;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataBinderMapperImpl extends DataBinderMapper {
  private static final int LAYOUT_ACTIVITYADMIN = 1;

  private static final int LAYOUT_ACTIVITYERRORDETAILS = 2;

  private static final int LAYOUT_ACTIVITYINITIALSETUP = 3;

  private static final int LAYOUT_ACTIVITYMAIN = 4;

  private static final int LAYOUT_ACTIVITYMDMCHOICE = 5;

  private static final int LAYOUT_DIALOGACCESSIBILITYSERVICE = 6;

  private static final int LAYOUT_DIALOGADMINISTRATORMODE = 7;

  private static final int LAYOUT_DIALOGDEVICEINFO = 8;

  private static final int LAYOUT_DIALOGENTERDEVICEID = 9;

  private static final int LAYOUT_DIALOGENTERPASSWORD = 10;

  private static final int LAYOUT_DIALOGENTERSERVER = 11;

  private static final int LAYOUT_DIALOGFILEDOWNLOADINGFAILED = 12;

  private static final int LAYOUT_DIALOGHISTORYSETTINGS = 13;

  private static final int LAYOUT_DIALOGMANAGESTORAGE = 14;

  private static final int LAYOUT_DIALOGMIUIPERMISSIONS = 15;

  private static final int LAYOUT_DIALOGNETWORKERROR = 16;

  private static final int LAYOUT_DIALOGOVERLAYSETTINGS = 17;

  private static final int LAYOUT_DIALOGPERMISSIONS = 18;

  private static final int LAYOUT_DIALOGSYSTEMSETTINGS = 19;

  private static final int LAYOUT_DIALOGUNKNOWNSOURCES = 20;

  private static final int LAYOUT_ITEMAPP = 21;

  private static final SparseIntArray INTERNAL_LAYOUT_ID_LOOKUP = new SparseIntArray(21);

  static {
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.activity_admin, LAYOUT_ACTIVITYADMIN);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.activity_error_details, LAYOUT_ACTIVITYERRORDETAILS);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.activity_initial_setup, LAYOUT_ACTIVITYINITIALSETUP);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.activity_main, LAYOUT_ACTIVITYMAIN);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.activity_mdm_choice, LAYOUT_ACTIVITYMDMCHOICE);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_accessibility_service, LAYOUT_DIALOGACCESSIBILITYSERVICE);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_administrator_mode, LAYOUT_DIALOGADMINISTRATORMODE);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_device_info, LAYOUT_DIALOGDEVICEINFO);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_enter_device_id, LAYOUT_DIALOGENTERDEVICEID);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_enter_password, LAYOUT_DIALOGENTERPASSWORD);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_enter_server, LAYOUT_DIALOGENTERSERVER);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_file_downloading_failed, LAYOUT_DIALOGFILEDOWNLOADINGFAILED);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_history_settings, LAYOUT_DIALOGHISTORYSETTINGS);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_manage_storage, LAYOUT_DIALOGMANAGESTORAGE);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_miui_permissions, LAYOUT_DIALOGMIUIPERMISSIONS);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_network_error, LAYOUT_DIALOGNETWORKERROR);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_overlay_settings, LAYOUT_DIALOGOVERLAYSETTINGS);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_permissions, LAYOUT_DIALOGPERMISSIONS);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_system_settings, LAYOUT_DIALOGSYSTEMSETTINGS);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.dialog_unknown_sources, LAYOUT_DIALOGUNKNOWNSOURCES);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.hmdm.launcher.R.layout.item_app, LAYOUT_ITEMAPP);
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View view, int layoutId) {
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = view.getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
        case  LAYOUT_ACTIVITYADMIN: {
          if ("layout/activity_admin_0".equals(tag)) {
            return new ActivityAdminBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_admin is invalid. Received: " + tag);
        }
        case  LAYOUT_ACTIVITYERRORDETAILS: {
          if ("layout/activity_error_details_0".equals(tag)) {
            return new ActivityErrorDetailsBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_error_details is invalid. Received: " + tag);
        }
        case  LAYOUT_ACTIVITYINITIALSETUP: {
          if ("layout/activity_initial_setup_0".equals(tag)) {
            return new ActivityInitialSetupBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_initial_setup is invalid. Received: " + tag);
        }
        case  LAYOUT_ACTIVITYMAIN: {
          if ("layout/activity_main_0".equals(tag)) {
            return new ActivityMainBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_main is invalid. Received: " + tag);
        }
        case  LAYOUT_ACTIVITYMDMCHOICE: {
          if ("layout/activity_mdm_choice_0".equals(tag)) {
            return new ActivityMdmChoiceBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_mdm_choice is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGACCESSIBILITYSERVICE: {
          if ("layout/dialog_accessibility_service_0".equals(tag)) {
            return new DialogAccessibilityServiceBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_accessibility_service is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGADMINISTRATORMODE: {
          if ("layout/dialog_administrator_mode_0".equals(tag)) {
            return new DialogAdministratorModeBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_administrator_mode is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGDEVICEINFO: {
          if ("layout/dialog_device_info_0".equals(tag)) {
            return new DialogDeviceInfoBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_device_info is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGENTERDEVICEID: {
          if ("layout/dialog_enter_device_id_0".equals(tag)) {
            return new DialogEnterDeviceIdBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_enter_device_id is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGENTERPASSWORD: {
          if ("layout/dialog_enter_password_0".equals(tag)) {
            return new DialogEnterPasswordBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_enter_password is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGENTERSERVER: {
          if ("layout/dialog_enter_server_0".equals(tag)) {
            return new DialogEnterServerBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_enter_server is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGFILEDOWNLOADINGFAILED: {
          if ("layout/dialog_file_downloading_failed_0".equals(tag)) {
            return new DialogFileDownloadingFailedBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_file_downloading_failed is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGHISTORYSETTINGS: {
          if ("layout/dialog_history_settings_0".equals(tag)) {
            return new DialogHistorySettingsBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_history_settings is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGMANAGESTORAGE: {
          if ("layout/dialog_manage_storage_0".equals(tag)) {
            return new DialogManageStorageBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_manage_storage is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGMIUIPERMISSIONS: {
          if ("layout/dialog_miui_permissions_0".equals(tag)) {
            return new DialogMiuiPermissionsBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_miui_permissions is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGNETWORKERROR: {
          if ("layout/dialog_network_error_0".equals(tag)) {
            return new DialogNetworkErrorBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_network_error is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGOVERLAYSETTINGS: {
          if ("layout/dialog_overlay_settings_0".equals(tag)) {
            return new DialogOverlaySettingsBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_overlay_settings is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGPERMISSIONS: {
          if ("layout/dialog_permissions_0".equals(tag)) {
            return new DialogPermissionsBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_permissions is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGSYSTEMSETTINGS: {
          if ("layout/dialog_system_settings_0".equals(tag)) {
            return new DialogSystemSettingsBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_system_settings is invalid. Received: " + tag);
        }
        case  LAYOUT_DIALOGUNKNOWNSOURCES: {
          if ("layout/dialog_unknown_sources_0".equals(tag)) {
            return new DialogUnknownSourcesBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for dialog_unknown_sources is invalid. Received: " + tag);
        }
        case  LAYOUT_ITEMAPP: {
          if ("layout/item_app_0".equals(tag)) {
            return new ItemAppBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for item_app is invalid. Received: " + tag);
        }
      }
    }
    return null;
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View[] views, int layoutId) {
    if(views == null || views.length == 0) {
      return null;
    }
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = views[0].getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
      }
    }
    return null;
  }

  @Override
  public int getLayoutId(String tag) {
    if (tag == null) {
      return 0;
    }
    Integer tmpVal = InnerLayoutIdLookup.sKeys.get(tag);
    return tmpVal == null ? 0 : tmpVal;
  }

  @Override
  public String convertBrIdToString(int localId) {
    String tmpVal = InnerBrLookup.sKeys.get(localId);
    return tmpVal;
  }

  @Override
  public List<DataBinderMapper> collectDependencies() {
    ArrayList<DataBinderMapper> result = new ArrayList<DataBinderMapper>(1);
    result.add(new androidx.databinding.library.baseAdapters.DataBinderMapperImpl());
    return result;
  }

  private static class InnerBrLookup {
    static final SparseArray<String> sKeys = new SparseArray<String>(15);

    static {
      sKeys.put(0, "_all");
      sKeys.put(1, "deviceId");
      sKeys.put(2, "downloadedLength");
      sKeys.put(3, "downloading");
      sKeys.put(4, "error");
      sKeys.put(5, "fileLength");
      sKeys.put(6, "imei");
      sKeys.put(7, "loading");
      sKeys.put(8, "message");
      sKeys.put(9, "phone");
      sKeys.put(10, "serialNumber");
      sKeys.put(11, "server");
      sKeys.put(12, "serverUrl");
      sKeys.put(13, "showContent");
      sKeys.put(14, "version");
    }
  }

  private static class InnerLayoutIdLookup {
    static final HashMap<String, Integer> sKeys = new HashMap<String, Integer>(21);

    static {
      sKeys.put("layout/activity_admin_0", com.hmdm.launcher.R.layout.activity_admin);
      sKeys.put("layout/activity_error_details_0", com.hmdm.launcher.R.layout.activity_error_details);
      sKeys.put("layout/activity_initial_setup_0", com.hmdm.launcher.R.layout.activity_initial_setup);
      sKeys.put("layout/activity_main_0", com.hmdm.launcher.R.layout.activity_main);
      sKeys.put("layout/activity_mdm_choice_0", com.hmdm.launcher.R.layout.activity_mdm_choice);
      sKeys.put("layout/dialog_accessibility_service_0", com.hmdm.launcher.R.layout.dialog_accessibility_service);
      sKeys.put("layout/dialog_administrator_mode_0", com.hmdm.launcher.R.layout.dialog_administrator_mode);
      sKeys.put("layout/dialog_device_info_0", com.hmdm.launcher.R.layout.dialog_device_info);
      sKeys.put("layout/dialog_enter_device_id_0", com.hmdm.launcher.R.layout.dialog_enter_device_id);
      sKeys.put("layout/dialog_enter_password_0", com.hmdm.launcher.R.layout.dialog_enter_password);
      sKeys.put("layout/dialog_enter_server_0", com.hmdm.launcher.R.layout.dialog_enter_server);
      sKeys.put("layout/dialog_file_downloading_failed_0", com.hmdm.launcher.R.layout.dialog_file_downloading_failed);
      sKeys.put("layout/dialog_history_settings_0", com.hmdm.launcher.R.layout.dialog_history_settings);
      sKeys.put("layout/dialog_manage_storage_0", com.hmdm.launcher.R.layout.dialog_manage_storage);
      sKeys.put("layout/dialog_miui_permissions_0", com.hmdm.launcher.R.layout.dialog_miui_permissions);
      sKeys.put("layout/dialog_network_error_0", com.hmdm.launcher.R.layout.dialog_network_error);
      sKeys.put("layout/dialog_overlay_settings_0", com.hmdm.launcher.R.layout.dialog_overlay_settings);
      sKeys.put("layout/dialog_permissions_0", com.hmdm.launcher.R.layout.dialog_permissions);
      sKeys.put("layout/dialog_system_settings_0", com.hmdm.launcher.R.layout.dialog_system_settings);
      sKeys.put("layout/dialog_unknown_sources_0", com.hmdm.launcher.R.layout.dialog_unknown_sources);
      sKeys.put("layout/item_app_0", com.hmdm.launcher.R.layout.item_app);
    }
  }
}
