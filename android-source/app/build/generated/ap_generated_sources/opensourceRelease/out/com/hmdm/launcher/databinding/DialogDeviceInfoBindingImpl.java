package com.hmdm.launcher.databinding;
import com.hmdm.launcher.R;
import com.hmdm.launcher.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class DialogDeviceInfoBindingImpl extends DialogDeviceInfoBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.imei_layout, 7);
        sViewsWithIds.put(R.id.serial_layout, 8);
    }
    // views
    @NonNull
    private final android.widget.LinearLayout mboundView0;
    @NonNull
    private final android.widget.TextView mboundView1;
    @NonNull
    private final android.widget.TextView mboundView2;
    @NonNull
    private final android.widget.TextView mboundView3;
    @NonNull
    private final android.widget.TextView mboundView4;
    @NonNull
    private final android.widget.TextView mboundView5;
    @NonNull
    private final android.widget.TextView mboundView6;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public DialogDeviceInfoBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 9, sIncludes, sViewsWithIds));
    }
    private DialogDeviceInfoBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.LinearLayout) bindings[7]
            , (android.widget.LinearLayout) bindings[8]
            );
        this.mboundView0 = (android.widget.LinearLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.mboundView1 = (android.widget.TextView) bindings[1];
        this.mboundView1.setTag(null);
        this.mboundView2 = (android.widget.TextView) bindings[2];
        this.mboundView2.setTag(null);
        this.mboundView3 = (android.widget.TextView) bindings[3];
        this.mboundView3.setTag(null);
        this.mboundView4 = (android.widget.TextView) bindings[4];
        this.mboundView4.setTag(null);
        this.mboundView5 = (android.widget.TextView) bindings[5];
        this.mboundView5.setTag(null);
        this.mboundView6 = (android.widget.TextView) bindings[6];
        this.mboundView6.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x40L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
        if (BR.serverUrl == variableId) {
            setServerUrl((java.lang.String) variable);
        }
        else if (BR.version == variableId) {
            setVersion((java.lang.String) variable);
        }
        else if (BR.serialNumber == variableId) {
            setSerialNumber((java.lang.String) variable);
        }
        else if (BR.imei == variableId) {
            setImei((java.lang.String) variable);
        }
        else if (BR.deviceId == variableId) {
            setDeviceId((java.lang.String) variable);
        }
        else if (BR.phone == variableId) {
            setPhone((java.lang.String) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setServerUrl(@Nullable java.lang.String ServerUrl) {
        this.mServerUrl = ServerUrl;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.serverUrl);
        super.requestRebind();
    }
    public void setVersion(@Nullable java.lang.String Version) {
        this.mVersion = Version;
        synchronized(this) {
            mDirtyFlags |= 0x2L;
        }
        notifyPropertyChanged(BR.version);
        super.requestRebind();
    }
    public void setSerialNumber(@Nullable java.lang.String SerialNumber) {
        this.mSerialNumber = SerialNumber;
        synchronized(this) {
            mDirtyFlags |= 0x4L;
        }
        notifyPropertyChanged(BR.serialNumber);
        super.requestRebind();
    }
    public void setImei(@Nullable java.lang.String Imei) {
        this.mImei = Imei;
        synchronized(this) {
            mDirtyFlags |= 0x8L;
        }
        notifyPropertyChanged(BR.imei);
        super.requestRebind();
    }
    public void setDeviceId(@Nullable java.lang.String DeviceId) {
        this.mDeviceId = DeviceId;
        synchronized(this) {
            mDirtyFlags |= 0x10L;
        }
        notifyPropertyChanged(BR.deviceId);
        super.requestRebind();
    }
    public void setPhone(@Nullable java.lang.String Phone) {
        this.mPhone = Phone;
        synchronized(this) {
            mDirtyFlags |= 0x20L;
        }
        notifyPropertyChanged(BR.phone);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        java.lang.String serverUrl = mServerUrl;
        java.lang.String version = mVersion;
        java.lang.String serialNumber = mSerialNumber;
        java.lang.String imei = mImei;
        java.lang.String deviceId = mDeviceId;
        java.lang.String phone = mPhone;

        if ((dirtyFlags & 0x41L) != 0) {
        }
        if ((dirtyFlags & 0x42L) != 0) {
        }
        if ((dirtyFlags & 0x44L) != 0) {
        }
        if ((dirtyFlags & 0x48L) != 0) {
        }
        if ((dirtyFlags & 0x50L) != 0) {
        }
        if ((dirtyFlags & 0x60L) != 0) {
        }
        // batch finished
        if ((dirtyFlags & 0x41L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.mboundView1, serverUrl);
        }
        if ((dirtyFlags & 0x50L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.mboundView2, deviceId);
        }
        if ((dirtyFlags & 0x48L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.mboundView3, imei);
        }
        if ((dirtyFlags & 0x44L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.mboundView4, serialNumber);
        }
        if ((dirtyFlags & 0x60L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.mboundView5, phone);
        }
        if ((dirtyFlags & 0x42L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.mboundView6, version);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): serverUrl
        flag 1 (0x2L): version
        flag 2 (0x3L): serialNumber
        flag 3 (0x4L): imei
        flag 4 (0x5L): deviceId
        flag 5 (0x6L): phone
        flag 6 (0x7L): null
    flag mapping end*/
    //end
}