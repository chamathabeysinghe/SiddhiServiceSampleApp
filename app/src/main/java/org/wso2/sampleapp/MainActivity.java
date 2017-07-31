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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.wso2.siddhiservice.IRequestController;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String APP_IDENTIFIER="org.wso2.sampleapp";
    private final String SIDDHI_SERVICE_IDENTIFIER="org.wso2.siddhiappservice.AIDL";

    private DataUpdateReceiver dataUpdateReceiver;

    private String inStreamDefinition = "" +
            "@app:name('foo2')" +
            "@source(type='temperature', @map(type='passThrough'))" +
            "define stream streamTemperature ( sensorName string, timestamp long, accuracy int,temp float);" +
            "@sink(type='broadcast' , identifier='TEMPERATURE_DETAILS' , @map(type='passThrough'))" +
            "define stream broadcastOutputStream (temp float); " +
            "from streamTemperature [temp > 6] select temp insert into broadcastOutputStream";


    private ListView listView;
    private ArrayList<String> messageList=new ArrayList<>();
    private ArrayAdapter<String> listAdapter;

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
        listView= (ListView) findViewById(R.id.messageList);

        listAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,messageList);
        listView.setAdapter(listAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter("TEMPERATURE_DETAILS");
        registerReceiver(dataUpdateReceiver, intentFilter);
    }

    /**
     *
     * Bind the Android app to Sidddhi Service
     * @param view
     */
    public void bindToSiddhiService(View view){
        Toast.makeText(this, "Binding to the service", Toast.LENGTH_SHORT).show();
        Intent intent=new Intent(SIDDHI_SERVICE_IDENTIFIER);
        bindService(convertIntent(intent),serviceCon,BIND_AUTO_CREATE);

    }

    /**
     * Send the app stream to SiddhiService
     * @param view
     */
    public void sendAQuery(View view){
        try {
            comman.startSiddhiApp(inStreamDefinition,APP_IDENTIFIER);
            Toast.makeText(this,"Send the query : "+inStreamDefinition,Toast.LENGTH_LONG).show();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    public void stopAQuery(View view){
        try {
            comman.stopSiddhiApp(APP_IDENTIFIER);
            Toast.makeText(this,"Stop the query",Toast.LENGTH_SHORT).show();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private Intent convertIntent(Intent implicitIntent){
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

    /**
     * Broadcast receiver to get intents from the Siddhi Service
     * Has a hardcoded intent filter to match the query
     */
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("TEMPERATURE_DETAILS")){
                messageList.add("TEMPERATURE_DETAILS: "+intent.getStringExtra("events"));
                listAdapter.notifyDataSetChanged();
                Log.i("TEMPERATURE_DETAILS",intent.getStringExtra("events"));
            }
        }
    }

}
