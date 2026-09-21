package com.hmdm.launcher.databinding;
import com.hmdm.launcher.R;
import com.hmdm.launcher.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class DialogEnterDeviceIdBindingImpl extends DialogEnterDeviceIdBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.device_id, 5);
        sViewsWithIds.put(R.id.showDeviceIdQrCode, 6);
        sViewsWithIds.put(R.id.showDeviceIdVariants, 7);
        sViewsWithIds.put(R.id.saveDeviceId, 8);
    }
    // views
    @NonNull
    private final android.widget.LinearLayout mboundView0;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public DialogEnterDeviceIdBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 9, sIncludes, sViewsWithIds));
    }
    private DialogEnterDeviceIdBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.AutoCompleteTextView) bindings[5]
            , (android.widget.TextView) bindings[1]
            , (android.widget.TextView) bindings[2]
            , (android.widget.TextView) bindings[3]
            , (android.widget.Button) bindings[4]
            , (android.widget.Button) bindings[8]
            , (android.widget.Button) bindings[6]
            , (android.widget.Button) bindings[7]
            );
        this.deviceIdError.setTag(null);
        this.deviceIdErrorDetails.setTag(null);
        this.deviceIdPrompt.setTag(null);
        this.exitDeviceId.setTag(null);
        this.mboundView0 = (android.widget.LinearLayout) bindings[0];
        this.mboundView0.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x2L;
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
        if (BR.error == variableId) {
            setError((java.lang.Boolean) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setError(@Nullable java.lang.Boolean Error) {
        this.mError = Error;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.error);
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
        boolean error = false;
        boolean androidxDatabindingViewDataBindingSafeUnboxError = false;
        boolean AndroidxDatabindingViewDataBindingSafeUnboxError1 = false;
        java.lang.Boolean Error1 = mError;

        if ((dirtyFlags & 0x3L) != 0) {



                // read androidx.databinding.ViewDataBinding.safeUnbox(error)
                androidxDatabindingViewDataBindingSafeUnboxError = androidx.databinding.ViewDataBinding.safeUnbox(Error1);


                // read !androidx.databinding.ViewDataBinding.safeUnbox(error)
                error = !androidxDatabindingViewDataBindingSafeUnboxError;


                // read androidx.databinding.ViewDataBinding.safeUnbox(!androidx.databinding.ViewDataBinding.safeUnbox(error))
                AndroidxDatabindingViewDataBindingSafeUnboxError1 = androidx.databinding.ViewDataBinding.safeUnbox(error);
        }
        // batch finished
        if ((dirtyFlags & 0x3L) != 0) {
            // api target 1

            com.hmdm.launcher.databinding.ViewBindingUtils.boolToVisible(this.deviceIdError, androidxDatabindingViewDataBindingSafeUnboxError);
            com.hmdm.launcher.databinding.ViewBindingUtils.boolToVisible(this.deviceIdErrorDetails, androidxDatabindingViewDataBindingSafeUnboxError);
            com.hmdm.launcher.databinding.ViewBindingUtils.boolToVisible(this.deviceIdPrompt, AndroidxDatabindingViewDataBindingSafeUnboxError1);
            com.hmdm.launcher.databinding.ViewBindingUtils.boolToVisible(this.exitDeviceId, androidxDatabindingViewDataBindingSafeUnboxError);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): error
        flag 1 (0x2L): null
    flag mapping end*/
    //end
}