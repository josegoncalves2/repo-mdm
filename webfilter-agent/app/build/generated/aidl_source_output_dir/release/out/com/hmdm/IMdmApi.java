/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /opt/projetos/hwmdm/repo-mdm/.android-sdk/build-tools/35.0.0/aidl -p/opt/projetos/hwmdm/repo-mdm/.android-sdk/platforms/android-34/framework.aidl -o/opt/projetos/hwmdm/repo-mdm/webfilter-agent/app/build/generated/aidl_source_output_dir/release/out -I/opt/projetos/hwmdm/repo-mdm/webfilter-agent/app/src/main/aidl -I/opt/projetos/hwmdm/repo-mdm/webfilter-agent/app/src/release/aidl -I/home/sahw/.gradle/caches/8.13/transforms/2a73b9be6d4f291b9bd93ba4a78e16cf/transformed/core-1.13.1/aidl -I/home/sahw/.gradle/caches/8.13/transforms/97be29755e6b940ef3dec554e527f2c8/transformed/versionedparcelable-1.1.1/aidl -d/tmp/aidl15745311041754153301.d /opt/projetos/hwmdm/repo-mdm/webfilter-agent/app/src/main/aidl/com/hmdm/IMdmApi.aidl
 */
package com.hmdm;
public interface IMdmApi extends android.os.IInterface
{
  /** Default implementation for IMdmApi. */
  public static class Default implements com.hmdm.IMdmApi
  {
    @Override public android.os.Bundle queryConfig() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void log(long timestamp, int level, java.lang.String packageId, java.lang.String message) throws android.os.RemoteException
    {
    }
    @Override public java.lang.String queryAppPreference(java.lang.String packageId, java.lang.String attr) throws android.os.RemoteException
    {
      return null;
    }
    @Override public boolean setAppPreference(java.lang.String packageId, java.lang.String attr, java.lang.String value) throws android.os.RemoteException
    {
      return false;
    }
    @Override public void commitAppPreferences(java.lang.String packageId) throws android.os.RemoteException
    {
    }
    @Override public int getVersion() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public android.os.Bundle queryPrivilegedConfig(java.lang.String apiKey) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void setCustom(int number, java.lang.String value) throws android.os.RemoteException
    {
    }
    @Override public void forceConfigUpdate() throws android.os.RemoteException
    {
    }
    @Override public boolean sendPush(java.lang.String apiKey, java.lang.String type, java.lang.String payload) throws android.os.RemoteException
    {
      return false;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.hmdm.IMdmApi
  {
    /** Construct the stub at attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.hmdm.IMdmApi interface,
     * generating a proxy if needed.
     */
    public static com.hmdm.IMdmApi asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.hmdm.IMdmApi))) {
        return ((com.hmdm.IMdmApi)iin);
      }
      return new com.hmdm.IMdmApi.Stub.Proxy(obj);
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
        case TRANSACTION_queryConfig:
        {
          android.os.Bundle _result = this.queryConfig();
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_log:
        {
          long _arg0;
          _arg0 = data.readLong();
          int _arg1;
          _arg1 = data.readInt();
          java.lang.String _arg2;
          _arg2 = data.readString();
          java.lang.String _arg3;
          _arg3 = data.readString();
          this.log(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_queryAppPreference:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _result = this.queryAppPreference(_arg0, _arg1);
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_setAppPreference:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          boolean _result = this.setAppPreference(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_commitAppPreferences:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.commitAppPreferences(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getVersion:
        {
          int _result = this.getVersion();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_queryPrivilegedConfig:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          android.os.Bundle _result = this.queryPrivilegedConfig(_arg0);
          reply.writeNoException();
          _Parcel.writeTypedObject(reply, _result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
          break;
        }
        case TRANSACTION_setCustom:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.setCustom(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_forceConfigUpdate:
        {
          this.forceConfigUpdate();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_sendPush:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          boolean _result = this.sendPush(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.hmdm.IMdmApi
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
      @Override public android.os.Bundle queryConfig() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.os.Bundle _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_queryConfig, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, android.os.Bundle.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void log(long timestamp, int level, java.lang.String packageId, java.lang.String message) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(timestamp);
          _data.writeInt(level);
          _data.writeString(packageId);
          _data.writeString(message);
          boolean _status = mRemote.transact(Stub.TRANSACTION_log, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public java.lang.String queryAppPreference(java.lang.String packageId, java.lang.String attr) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageId);
          _data.writeString(attr);
          boolean _status = mRemote.transact(Stub.TRANSACTION_queryAppPreference, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean setAppPreference(java.lang.String packageId, java.lang.String attr, java.lang.String value) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageId);
          _data.writeString(attr);
          _data.writeString(value);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAppPreference, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void commitAppPreferences(java.lang.String packageId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_commitAppPreferences, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getVersion() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getVersion, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.os.Bundle queryPrivilegedConfig(java.lang.String apiKey) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.os.Bundle _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(apiKey);
          boolean _status = mRemote.transact(Stub.TRANSACTION_queryPrivilegedConfig, _data, _reply, 0);
          _reply.readException();
          _result = _Parcel.readTypedObject(_reply, android.os.Bundle.CREATOR);
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setCustom(int number, java.lang.String value) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(number);
          _data.writeString(value);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCustom, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void forceConfigUpdate() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_forceConfigUpdate, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean sendPush(java.lang.String apiKey, java.lang.String type, java.lang.String payload) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(apiKey);
          _data.writeString(type);
          _data.writeString(payload);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendPush, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_queryConfig = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_log = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_queryAppPreference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_setAppPreference = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_commitAppPreferences = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_queryPrivilegedConfig = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_setCustom = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_forceConfigUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_sendPush = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.hmdm.IMdmApi";
  public android.os.Bundle queryConfig() throws android.os.RemoteException;
  public void log(long timestamp, int level, java.lang.String packageId, java.lang.String message) throws android.os.RemoteException;
  public java.lang.String queryAppPreference(java.lang.String packageId, java.lang.String attr) throws android.os.RemoteException;
  public boolean setAppPreference(java.lang.String packageId, java.lang.String attr, java.lang.String value) throws android.os.RemoteException;
  public void commitAppPreferences(java.lang.String packageId) throws android.os.RemoteException;
  public int getVersion() throws android.os.RemoteException;
  public android.os.Bundle queryPrivilegedConfig(java.lang.String apiKey) throws android.os.RemoteException;
  public void setCustom(int number, java.lang.String value) throws android.os.RemoteException;
  public void forceConfigUpdate() throws android.os.RemoteException;
  public boolean sendPush(java.lang.String apiKey, java.lang.String type, java.lang.String payload) throws android.os.RemoteException;
  /** @hide */
  static class _Parcel {
    static private <T> T readTypedObject(
        android.os.Parcel parcel,
        android.os.Parcelable.Creator<T> c) {
      if (parcel.readInt() != 0) {
          return c.createFromParcel(parcel);
      } else {
          return null;
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedObject(
        android.os.Parcel parcel, T value, int parcelableFlags) {
      if (value != null) {
        parcel.writeInt(1);
        value.writeToParcel(parcel, parcelableFlags);
      } else {
        parcel.writeInt(0);
      }
    }
  }
}
