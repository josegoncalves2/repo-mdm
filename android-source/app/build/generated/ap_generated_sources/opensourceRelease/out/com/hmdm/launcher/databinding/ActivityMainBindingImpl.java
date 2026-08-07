package com.hmdm.launcher.databinding;
import com.hmdm.launcher.R;
import com.hmdm.launcher.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ActivityMainBindingImpl extends ActivityMainBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.loading, 7);
        sViewsWithIds.put(R.id.activity_main_background, 8);
        sViewsWithIds.put(R.id.status_header, 9);
        sViewsWithIds.put(R.id.clock, 10);
        sViewsWithIds.put(R.id.activity_main_title, 11);
        sViewsWithIds.put(R.id.battery_state, 12);
        sViewsWithIds.put(R.id.activity_main_content, 13);
        sViewsWithIds.put(R.id.activity_bottom_layout, 14);
        sViewsWithIds.put(R.id.activity_bottom_line, 15);
    }
    // views
    @NonNull
    private final android.widget.TextView mboundView1;
    @NonNull
    private final android.widget.LinearLayout mboundView3;
    @NonNull
    private final android.widget.TextView mboundView4;
    @NonNull
    private final android.widget.TextView mboundView5;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ActivityMainBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 16, sIncludes, sViewsWithIds));
    }
    private ActivityMainBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.RelativeLayout) bindings[14]
            , (androidx.recyclerview.widget.RecyclerView) bindings[15]
            , (android.widget.RelativeLayout) bindings[0]
            , (android.widget.ImageView) bindings[8]
            , (androidx.recyclerview.widget.RecyclerView) bindings[13]
            , (android.widget.RelativeLayout) bindings[6]
            , (android.widget.TextView) bindings[11]
            , (com.hmdm.launcher.ui.custom.BatteryStateView) bindings[12]
            , (android.widget.TextView) bindings[10]
            , (android.widget.ProgressBar) bindings[7]
            , (android.widget.ProgressBar) bindings[2]
            , (android.widget.LinearLayout) bindings[9]
            );
        this.activityMain.setTag(null);
        this.activityMainContentWrapper.setTag(null);
        this.mboundView1 = (android.widget.TextView) bindings[1];
        this.mboundView1.setTag(null);
        this.mboundView3 = (android.widget.LinearLayout) bindings[3];
        this.mboundView3.setTag(null);
        this.mboundView4 = (android.widget.TextView) bindings[4];
        this.mboundView4.setTag(null);
        this.mboundView5 = (android.widget.TextView) bindings[5];
        this.mboundView5.setTag(null);
        this.progress.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x20L;
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
        if (BR.showContent == variableId) {
            setShowContent((java.lang.Boolean) variable);
        }
        else if (BR.message == variableId) {
            setMessage((java.lang.String) variable);
        }
        else if (BR.fileLength == variableId) {
            setFileLength((java.lang.Long) variable);
        }
        else if (BR.downloading == variableId) {
            setDownloading((java.lang.Boolean) variable);
        }
        else if (BR.downloadedLength == variableId) {
            setDownloadedLength((java.lang.Long) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setShowContent(@Nullable java.lang.Boolean ShowContent) {
        this.mShowContent = ShowContent;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.showContent);
        super.requestRebind();
    }
    public void setMessage(@Nullable java.lang.String Message) {
        this.mMessage = Message;
        synchronized(this) {
            mDirtyFlags |= 0x2L;
        }
        notifyPropertyChanged(BR.message);
        super.requestRebind();
    }
    public void setFileLength(@Nullable java.lang.Long FileLength) {
        this.mFileLength = FileLength;
        synchronized(this) {
            mDirtyFlags |= 0x4L;
        }
        notifyPropertyChanged(BR.fileLength);
        super.requestRebind();
    }
    public void setDownloading(@Nullable java.lang.Boolean Downloading) {
        this.mDownloading = Downloading;
        synchronized(this) {
            mDirtyFlags |= 0x8L;
        }
        notifyPropertyChanged(BR.downloading);
        super.requestRebind();
    }
    public void setDownloadedLength(@Nullable java.lang.Long DownloadedLength) {
        this.mDownloadedLength = DownloadedLength;
        synchronized(this) {
            mDirtyFlags |= 0x10L;
        }
        notifyPropertyChanged(BR.downloadedLength);
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
        java.lang.String stringValueOfFileLength = null;
        java.lang.String stringValueOfDownloadedLength = null;
        java.lang.Boolean showContent = mShowContent;
        long androidxDatabindingViewDataBindingSafeUnboxDownloadedLength = 0;
        boolean androidxDatabindingViewDataBindingSafeUnboxShowContent = false;
        long androidxDatabindingViewDataBindingSafeUnboxFileLength = 0;
        java.lang.String message = mMessage;
        java.lang.Long fileLength = mFileLength;
        java.lang.Boolean downloading = mDownloading;
        boolean androidxDatabindingViewDataBindingSafeUnboxDownloading = false;
        java.lang.Long downloadedLength = mDownloadedLength;

        if ((dirtyFlags & 0x21L) != 0) {



                // read androidx.databinding.ViewDataBinding.safeUnbox(showContent)
                androidxDatabindingViewDataBindingSafeUnboxShowContent = androidx.databinding.ViewDataBinding.safeUnbox(showContent);
        }
        if ((dirtyFlags & 0x22L) != 0) {
        }
        if ((dirtyFlags & 0x24L) != 0) {



                // read androidx.databinding.ViewDataBinding.safeUnbox(fileLength)
                androidxDatabindingViewDataBindingSafeUnboxFileLength = androidx.databinding.ViewDataBinding.safeUnbox(fileLength);


                // read String.valueOf(androidx.databinding.ViewDataBinding.safeUnbox(fileLength))
                stringValueOfFileLength = java.lang.String.valueOf(androidxDatabindingViewDataBindingSafeUnboxFileLength);
        }
        if ((dirtyFlags & 0x28L) != 0) {



                // read androidx.databinding.ViewDataBinding.safeUnbox(downloading)
                androidxDatabindingViewDataBindingSafeUnboxDownloading = androidx.databinding.ViewDataBinding.safeUnbox(downloading);
        }
        if ((dirtyFlags & 0x30L) != 0) {



                // read androidx.databinding.ViewDataBinding.safeUnbox(downloadedLength)
                androidxDatabindingViewDataBindingSafeUnboxDownloadedLength = androidx.databinding.ViewDataBinding.safeUnbox(downloadedLength);


                // read String.valueOf(androidx.databinding.ViewDataBinding.safeUnbox(downloadedLength))
                stringValueOfDownloadedLength = java.lang.String.valueOf(androidxDatabindingViewDataBindingSafeUnboxDownloadedLength);
        }
        // batch finished
        if ((dirtyFlags & 0x21L) != 0) {
            // api target 1

            com.hmdm.launcher.databinding.ViewBindingUtils.boolToVisible(this.activityMainContentWrapper, androidxDatabindingViewDataBindingSafeUnboxShowContent);
        }
        if ((dirtyFlags & 0x22L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.mboundView1, message);
        }
        if ((dirtyFlags & 0x28L) != 0) {
            // api target 1

            com.hmdm.launcher.databinding.ViewBindingUtils.boolToVisible(this.mboundView3, androidxDatabindingViewDataBindingSafeUnboxDownloading);
            com.hmdm.launcher.databinding.ViewBindingUtils.boolToVisible(this.progress, androidxDatabindingViewDataBindingSafeUnboxDownloading);
        }
        if ((dirtyFlags & 0x30L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.mboundView4, stringValueOfDownloadedLength);
        }
        if ((dirtyFlags & 0x24L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.mboundView5, stringValueOfFileLength);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): showContent
        flag 1 (0x2L): message
        flag 2 (0x3L): fileLength
        flag 3 (0x4L): downloading
        flag 4 (0x5L): downloadedLength
        flag 5 (0x6L): null
    flag mapping end*/
    //end
}