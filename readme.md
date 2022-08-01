# NFC HCE NDEF Reader

This app read an emulated NDEF message.

For sending the emulated tag you need a second application: NfcHceNdefSender

This app uses the AID D2760000850101 - this is the official NFC tag 4 class NDEF AID

AndroidManifest.xml
```plaintext
    <uses-permission android:name="android.permission.NFC" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    ...
    <queries>
        <intent>
            <action android:name="android.intent.action.SENDTO" />
            <data android:scheme="*" />
        </intent>
    </queries>
```


```plaintext

```


```plaintext

```


```plaintext

```

