package com.hmdm.launcher.databinding;
import com.hmdm.launcher.R;
import com.hmdm.launcher.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class DialogEnterPasswordBindingImpl extends DialogEnterPasswordBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = null;
    }
    // views
    @NonNull
    private final android.widget.LinearLayout mboundView0;
    @NonNull
    private final android.widget.TextView mboundView1;
    @NonNull
    private final android.widget.TextView mboundView2;
    @NonNull
    private final android.widget.Button mboundView4;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public DialogEnterPasswordBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 5, sIncludes, sViewsWithIds));
    }
    private DialogEnterPasswordBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.EditText) bindings[3]
            );
        this.mboundView0 = (android.widget.LinearLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.mboundView1 = (android.widget.TextView) bindings[1];
        this.mboundView1.setTag(null);
        this.mboundView2 = (android.widget.TextView) bindings[2];
        this.mboundView2.setTag(null);
        this.mboundView4 = (android.widget.Button) bindings[4];
        this.mboundView4.setTag(null);
        this.password.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x4L;
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
        if (BR.loading == variableId) {
            setLoading((java.lang.Boolean) variable);
        }
        else if (BR.error == variableId) {
            setError((java.lang.Boolean) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setLoading(@Nullable java.lang.Boolean Loading) {
        this.mLoading = Loading;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.loading);
        super.requestRebind();
    }
    public void setError(@Nullable java.lang.Boolean Error) {
        this.mError = Error;
        synchronized(this) {
            mDirtyFlags |= 0x2L;
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
        java.lang.Boolean loading = mLoading;
        boolean error = false;
        boolean androidxDatabindingViewDataBindingSafeUnboxError = false;
        boolean androidxDatabindingViewDataBindingSafeUnboxLoading = false;
        java.lang.Boolean Error1 = mError;
        boolean AndroidxDatabindingViewDataBindingSafeUnboxError1 = false;

        if ((dirtyFlags & 0x5L) != 0) {



                // read androidx.databinding.ViewDataBinding.safeUnbox(loading)
                androidxDatabindingViewDataBindingSafeUnboxLoading = androidx.databinding.ViewDataBinding.safeUnbox(loading);
        }
        if ((dirtyFlags & 0x6L) != 0) {



                // read androidx.databinding.ViewDataBinding.safeUnbox(error)
                androidxDatabindingViewDataBindingSafeUnboxError = androidx.databinding.ViewDataBinding.safeUnbox(Error1);


                // read !androidx.databinding.ViewDataBinding.safeUnbox(error)
                error = !androidxDatabindingViewDataBindingSafeUnboxError;


                // read androidx.databinding.ViewDataBinding.safeUnbox(!androidx.databinding.ViewDataBinding.safeUnbox(error))
                AndroidxDatabindingViewDataBindingSafeUnboxError1 = androidx.databinding.ViewDataBinding.safeUnbox(error);
        }
        // batch finished
        if ((dirtyFlags & 0x6L) != 0) {
            // api target 1

            com.hmdm.launcher.databinding.ViewBindingUtils.boolToVisible(this.mboundView1, androidxDatabindingViewDataBindingSafeUnboxError);
            com.hmdm.launcher.databinding.ViewBindingUtils.boolToVisible(this.mboundView2, AndroidxDatabindingViewDataBindingSafeUnboxError1);
        }
        if ((dirtyFlags & 0x5L) != 0) {
            // api target 1

            com.hmdm.launcher.databinding.ViewBindingUtils.boolToDisable(this.mboundView4, androidxDatabindingViewDataBindingSafeUnboxLoading);
            com.hmdm.launcher.databinding.ViewBindingUtils.boolToDisable(this.password, androidxDatabindingViewDataBindingSafeUnboxLoading);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): loading
        flag 1 (0x2L): error
        flag 2 (0x3L): null
    flag mapping end*/
    //end
}