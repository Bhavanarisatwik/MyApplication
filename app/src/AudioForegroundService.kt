<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.INTERNET"/>

<application ...>

<service
android:name=".AudioForegroundService"
android:foregroundServiceType="microphone"
android:exported="false" />

</application>
