package org.wso2.sampleapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import org.wso2.siddhiservice.IRequestController;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private DataUpdateReceiver dataUpdateReceiver;

    String inStreamDefinition = "" +
            "@app:name('foo1')" +
            "@source(type='proximity',classname='org.wso2.ceptest.MainActivity', @map(type='passThrough'))" +
            "@sink(type='broadcast' , identifier='EVENT_DETAILS' , @map(type='passThrough'))" +
            "define stream streamProximity ( sensorName string, timestamp long, accuracy int,distance float);";

    String inStreamDefinition2 = "" +
            "@app:name('foo2')" +
            "@source(type='temperature',classname='org.wso2.ceptest.MainActivity', @map(type='passThrough'))" +
            "define stream streamProximity ( sensorName string, timestamp long, accuracy int,distance float);" +
            "@sink(type='broadcast' , identifier='TEMPERATURE_DETAILS' , @map(type='passThrough'))" +
            "define stream broadcastOutputStream (distance float); " +
            "from streamProximity [distance > 4] select distance insert into broadcastOutputStream";

    String inStreamDefinition3 = "" +
            "@app:name('foo3')" +
            "@source(type='proximity',classname='org.wso2.ceptest.MainActivity', @map(type='passThrough'))" +
            "define stream streamProximity ( sensorName string, timestamp long, accuracy int,distance float);" +
            "@sink(type='broadcast' , identifier='EVENT_DETAILS' , @map(type='passThrough'))" +
            "define stream broadcastOutputStream (distance float); " +
            "from streamProximity#window.timeBatch(5 sec) select sum(distance) as distance insert into broadcastOutputStream";





    private class DataUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("TEMPERATURE_DETAILS")){
                Log.e("TEMPERATURE_DETAILS",intent.getStringExtra("events"));
            }
        }
    }


    private IRequestController comman ;
    private ServiceConnection serviceCon=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            comman=IRequestController.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter("TEMPERATURE_DETAILS");
        registerReceiver(dataUpdateReceiver, intentFilter);
    }

    public void bindToSiddhiService(View view){
        Log.e("Clicked","Clicked the button ");
        Intent intent=new Intent("org.wso2.siddhiappservice.AIDL");
        bindService(convertIntent(intent),serviceCon,BIND_AUTO_CREATE);

    }

    public void sendAQuery(View view){
        try {
            comman.startSiddhiApp(inStreamDefinition2,"org.wso2.sampleapp");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public Intent convertIntent(Intent implicitIntent){
        PackageManager pm=getPackageManager();
        List<ResolveInfo> resolveInfoList=pm.queryIntentServices(implicitIntent,0);
        if(resolveInfoList.size()!=1){
            Log.e("Not null ","Size error  problem  "+resolveInfoList.size());
        }
        if(resolveInfoList==null){
            Log.e("NUll","null error");
            return null;
        }
        ResolveInfo serviceInfo=resolveInfoList.get(0);
        ComponentName component=new ComponentName(serviceInfo.serviceInfo.packageName,serviceInfo.serviceInfo.name);
        Intent explicitIntent=new Intent(implicitIntent);
        explicitIntent.setComponent(component);
        return explicitIntent;
    }

}
