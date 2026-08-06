/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /home/sahwmdm/android-sdk/build-tools/35.0.0/aidl -p/home/sahwmdm/android-sdk/platforms/android-34/framework.aidl -o/opt/projetos/hwmdm/android-source/app/build/generated/aidl_source_output_dir/opensourceRelease/out -I/opt/projetos/hwmdm/android-source/app/src/main/aidl -I/opt/projetos/hwmdm/android-source/app/src/opensource/aidl -I/opt/projetos/hwmdm/android-source/app/src/release/aidl -I/opt/projetos/hwmdm/android-source/app/src/opensourceRelease/aidl -I/home/sahwmdm/.gradle/caches/8.13/transforms/00eeb73d329f60914ee7cf1a9183d8d3/transformed/core-1.9.0/aidl -I/home/sahwmdm/.gradle/caches/8.13/transforms/97be29755e6b940ef3dec554e527f2c8/transformed/versionedparcelable-1.1.1/aidl -d/tmp/aidl13901366543969761924.d /opt/projetos/hwmdm/android-source/app/src/main/aidl/com/hmdm/IMdmApiCallback.aidl
 */
package com.hmdm;
// Callback interface used to deliver live configuration update progress events
// from the Headwind MDM launcher to a connected app.
// Added in library version 1.1.9.
//
// Declared "oneway" so that callbacks are dispatched asynchronously and a slow
// (or hung) client can never block the launcher's configuration update.
public interface IMdmApiCallback extends android.os.IInterface
{
  /** Default implementation for IMdmApiCallback. */
  public static class Default implements com.hmdm.IMdmApiCallback
  {
    /** The configuration update has started. */
    @Override public void onConfigUpdateStart() throws android.os.RemoteException
    {
    }
    /**
     * The configuration update failed before completion.
     * type: 1 = server error, 2 = network error
     */
    @Override public void onConfigUpdateError(int type, java.lang.String errorText) throws android.os.RemoteException
    {
    }
    /** The server configuration has been loaded and saved. */
    @Override public void onConfigLoaded() throws android.os.RemoteException
    {
    }
    /** Device policies/restrictions have been (re-)applied. */
    @Override public void onPoliciesUpdated() throws android.os.RemoteException
    {
    }
    /** A remote file is being downloaded. */
    @Override public void onFileDownloading(java.lang.String path) throws android.os.RemoteException
    {
    }
    /** Download progress for the current file/app. */
    @Override public void onDownloadProgress(int progress, long total, long current) throws android.os.RemoteException
    {
    }
    /**
     * A remote file failed.
     * type: 1 = download error, 2 = install error
     */
    @Override public void onFileError(int type, java.lang.String path) throws android.os.RemoteException
    {
    }
    /** The application install/update phase has started. */
    @Override public void onAppUpdateStart() throws android.os.RemoteException
    {
    }
    /** An application is being removed. */
    @Override public void onAppRemoving(java.lang.String pkg, java.lang.String name) throws android.os.RemoteException
    {
    }
    /** An application is being downloaded. */
    @Override public void onAppDownloading(java.lang.String pkg, java.lang.String name) throws android.os.RemoteException
    {
    }
    /** An application is being installed. */
    @Override public void onAppInstalling(java.lang.String pkg, java.lang.String name) throws android.os.RemoteException
    {
    }
    /**
     * An application failed.
     * type: 1 = download error, 2 = install error
     */
    @Override public void onAppError(int type, java.lang.String pkg) throws android.os.RemoteException
    {
    }
    /** A single application install completed. */
    @Override public void onAppInstallComplete(java.lang.String pkg) throws android.os.RemoteException
    {
    }
    /** The configuration update completed (config + policies + files). */
    @Override public void onConfigUpdateComplete() throws android.os.RemoteException
    {
    }
    /** All pending application installs completed. */
    @Override public void onAllAppInstallComplete() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.hmdm.IMdmApiCallback
  {
    /** Construct the stub at attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.hmdm.IMdmApiCallback interface,
     * generating a proxy if needed.
     */
    public static com.hmdm.IMdmApiCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.hmdm.IMdmApiCallback))) {
        return ((com.hmdm.IMdmApiCallback)iin);
      }
      return new com.hmdm.IMdmApiCallback.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_onConfigUpdateStart:
        {
          this.onConfigUpdateStart();
          break;
        }
        case TRANSACTION_onConfigUpdateError:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.onConfigUpdateError(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onConfigLoaded:
        {
          this.onConfigLoaded();
          break;
        }
        case TRANSACTION_onPoliciesUpdated:
        {
          this.onPoliciesUpdated();
          break;
        }
        case TRANSACTION_onFileDownloading:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.onFileDownloading(_arg0);
          break;
        }
        case TRANSACTION_onDownloadProgress:
        {
          int _arg0;
          _arg0 = data.readInt();
          long _arg1;
          _arg1 = data.readLong();
          long _arg2;
          _arg2 = data.readLong();
          this.onDownloadProgress(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_onFileError:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.onFileError(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onAppUpdateStart:
        {
          this.onAppUpdateStart();
          break;
        }
        case TRANSACTION_onAppRemoving:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.onAppRemoving(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onAppDownloading:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.onAppDownloading(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onAppInstalling:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.onAppInstalling(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onAppError:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.onAppError(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onAppInstallComplete:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.onAppInstallComplete(_arg0);
          break;
        }
        case TRANSACTION_onConfigUpdateComplete:
        {
          this.onConfigUpdateComplete();
          break;
        }
        case TRANSACTION_onAllAppInstallComplete:
        {
          this.onAllAppInstallComplete();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.hmdm.IMdmApiCallback
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      /** The configuration update has started. */
      @Override public void onConfigUpdateStart() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onConfigUpdateStart, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * The configuration update failed before completion.
       * type: 1 = server error, 2 = network error
       */
      @Override public void onConfigUpdateError(int type, java.lang.String errorText) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeString(errorText);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onConfigUpdateError, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** The server configuration has been loaded and saved. */
      @Override public void onConfigLoaded() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onConfigLoaded, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Device policies/restrictions have been (re-)applied. */
      @Override public void onPoliciesUpdated() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPoliciesUpdated, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** A remote file is being downloaded. */
      @Override public void onFileDownloading(java.lang.String path) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(path);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onFileDownloading, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Download progress for the current file/app. */
      @Override public void onDownloadProgress(int progress, long total, long current) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(progress);
          _data.writeLong(total);
          _data.writeLong(current);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onDownloadProgress, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * A remote file failed.
       * type: 1 = download error, 2 = install error
       */
      @Override public void onFileError(int type, java.lang.String path) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeString(path);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onFileError, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** The application install/update phase has started. */
      @Override public void onAppUpdateStart() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAppUpdateStart, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** An application is being removed. */
      @Override public void onAppRemoving(java.lang.String pkg, java.lang.String name) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(pkg);
          _data.writeString(name);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAppRemoving, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** An application is being downloaded. */
      @Override public void onAppDownloading(java.lang.String pkg, java.lang.String name) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(pkg);
          _data.writeString(name);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAppDownloading, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** An application is being installed. */
      @Override public void onAppInstalling(java.lang.String pkg, java.lang.String name) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(pkg);
          _data.writeString(name);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAppInstalling, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * An application failed.
       * type: 1 = download error, 2 = install error
       */
      @Override public void onAppError(int type, java.lang.String pkg) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(type);
          _data.writeString(pkg);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAppError, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** A single application install completed. */
      @Override public void onAppInstallComplete(java.lang.String pkg) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(pkg);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAppInstallComplete, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** The configuration update completed (config + policies + files). */
      @Override public void onConfigUpdateComplete() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onConfigUpdateComplete, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** All pending application installs completed. */
      @Override public void onAllAppInstallComplete() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onAllAppInstallComplete, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onConfigUpdateStart = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onConfigUpdateError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onConfigLoaded = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onPoliciesUpdated = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_onFileDownloading = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onDownloadProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onFileError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_onAppUpdateStart = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_onAppRemoving = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_onAppDownloading = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_onAppInstalling = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_onAppError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_onAppInstallComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_onConfigUpdateComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_onAllAppInstallComplete = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.hmdm.IMdmApiCallback";
  /** The configuration update has started. */
  public void onConfigUpdateStart() throws android.os.RemoteException;
  /**
   * The configuration update failed before completion.
   * type: 1 = server error, 2 = network error
   */
  public void onConfigUpdateError(int type, java.lang.String errorText) throws android.os.RemoteException;
  /** The server configuration has been loaded and saved. */
  public void onConfigLoaded() throws android.os.RemoteException;
  /** Device policies/restrictions have been (re-)applied. */
  public void onPoliciesUpdated() throws android.os.RemoteException;
  /** A remote file is being downloaded. */
  public void onFileDownloading(java.lang.String path) throws android.os.RemoteException;
  /** Download progress for the current file/app. */
  public void onDownloadProgress(int progress, long total, long current) throws android.os.RemoteException;
  /**
   * A remote file failed.
   * type: 1 = download error, 2 = install error
   */
  public void onFileError(int type, java.lang.String path) throws android.os.RemoteException;
  /** The application install/update phase has started. */
  public void onAppUpdateStart() throws android.os.RemoteException;
  /** An application is being removed. */
  public void onAppRemoving(java.lang.String pkg, java.lang.String name) throws android.os.RemoteException;
  /** An application is being downloaded. */
  public void onAppDownloading(java.lang.String pkg, java.lang.String name) throws android.os.RemoteException;
  /** An application is being installed. */
  public void onAppInstalling(java.lang.String pkg, java.lang.String name) throws android.os.RemoteException;
  /**
   * An application failed.
   * type: 1 = download error, 2 = install error
   */
  public void onAppError(int type, java.lang.String pkg) throws android.os.RemoteException;
  /** A single application install completed. */
  public void onAppInstallComplete(java.lang.String pkg) throws android.os.RemoteException;
  /** The configuration update completed (config + policies + files). */
  public void onConfigUpdateComplete() throws android.os.RemoteException;
  /** All pending application installs completed. */
  public void onAllAppInstallComplete() throws android.os.RemoteException;
}
