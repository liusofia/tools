package com.xiaomi.wifiscantest;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.Thread;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.os.Bundle;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import xiaomi.android.miwifiutiilstoolaarlibs.MiWifiUtils;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MiWifiScanTestActivity extends Activity{
    public static final String TAG = "MiWifiFeatureTools";

    //fragment start
    private TextView mTitleShowText;
    private TextView mSwitchApNavTxt;
    private TextView mCaptureLogNavTxt;
    private TextView mEnableDisableWifiNavTxt;
    private TextView mIperfNavTxt;
    private FrameLayout mContextFramelayout;
    private TextView mShowAPIText;

    String API_LEVEL_NUMBER = "0x2018";
    public static int api_level = 0x2023;

    private SwitchAPFeatureFragment mSwitchApFragment;
    private CaptureFeatureFragment mCaptureLogFragment;
    private EnableDisableWIFIStressFeatureFragment mEnableDisableWifiFragment;
    private IperfFeatureFragment mIperfNavFragment;

    private FragmentManager mFragmentManager;
    //fragment end
    private WifiManager mWifiManager;
    private Timer mScanTimer;
    private Thread mCaptureWifiLogThread;
    private Thread mStressThread;
    private Thread mEAPOLFreameThread;
    private Thread mStartScanListCaptureKernelLogThread;
    private Thread mStartStressWifiStateThread;
    private MiWifiUtils mWifiUtils;
    private Context mContext;
    private List<ScanResult> mScanList;
    protected Object mWifiScanResultLock = new Object();
    List<WifiConfiguration> saveConfigured;
    private boolean isAvailableCheckScanResult = false;
    private boolean isAvailableStressWifiStateOption = false;

    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 0x2018;
    private static final int RESULT_CODE_FOR_OPEN_GPS = 0x2019;
    private static final int START = 0x2020;
    private static final int STOP = 0x2021;
    private static final int CONNECT_WIFI = 0x2022;
    private static final int STRESS_WIFI_STATE = 0x2023;
    private static final int UPDATE_FILE_PATH = 0x2024;
    private static int AVAILABLE_SWITCH_SSID_COUNT = 2;
    private WifiReceiver mWifiReceiver = null;
    private WifiHandler mHandler;

    private static MiWifiScanTestActivity miWifiScanTestActivityParent;

    private boolean isHasAlreadyStartedStressAPOption = false;
    private boolean isHasAlreadyStopedStressAPOption = false;
    private boolean isHasCheckedListDetails = false;
    private boolean isHasAlreadyStartStressWifiStateOption = false;
    private boolean isHasAlreadyStopStressWifiStateOption = false;

    private boolean isStopStressThread = false;
    private boolean isStopCaptureLog = false;
    private boolean isStopEAPOLFreameThread = false;
    private boolean isStopCaptureWifiKernelLogThread = false;
    private boolean isStopStressWIFIState = false;

    private final int WIFI_STATE_DISABLING = 0;
    private final int WIFI_STATE_DISABLED = 1;
    private final int WIFI_STATE_ENABLING = 2;
    private final int WIFI_STATE_ENABLED = 3;
    private final int WIFI_STATE_UNKNOWN = 4;

    public List<ScanResult> getScanList(){
        return mScanList;
    }

    //获取cache路径下所有文件
    public static ArrayList<File> getListFiles(Object obj) {
        File directory = null;
        if (obj instanceof File) {
            directory = (File) obj;
        } else {
            directory = new File(obj.toString());
        }
        ArrayList<File> files = new ArrayList<File>();
        if (directory.isFile()) {
            files.add(directory);
            return files;
        } else if (directory.isDirectory()) {
            File[] fileArr = directory.listFiles();
            for (int i = 0; i < fileArr.length; i++) {
                File fileOne = fileArr[i];
                files.addAll(getListFiles(fileOne));
            }
        }
        return files;
    }

    public void getCacheDirFile(){
        String path =  mContext.getCacheDir().getPath();
        ArrayList<File> list = getListFiles(path);
        for (File file : list) {
            Log.d(TAG,"Cache路径下文件:" + file.getName());
            if(null != mEnableDisableWifiFragment){
                Log.d(TAG,"准备开始展示log 文件" + file.getName());
                mEnableDisableWifiFragment.showCachePath(file.getName());
                mSwitchApFragment.showCachePath(file.getName());
            }
        }
    }

    public void showScanList(){
//        for(int i = 0;i< getScanList().size();i++){
//            Log.d(TAG,"SSid index: " + new Integer(i + 1).toString() + "; SSID:" + mScanList.get(i).SSID + "; Level: " + String.valueOf(mScanList.get(i).level));
//        }
        //获得当前连接ap 信息
        String currentAPInfo = "连接AP信息如下：" + "\n" + "当前SSID: " + mWifiUtils.getWifiInfo().getSSID() + "\n" +"当前BSSID: " + mWifiUtils.getWifiInfo().getBSSID() + "\n" + "当前信号强度: " + mWifiUtils.getWifiInfo().getRssi() + "\n";

        //尝试获得已保存的ap
        if(null != saveConfigured){
            for(int i = 0;i < saveConfigured.size(); i++) {
                Log.d(TAG,"获取已保存列表：" + saveConfigured.get(i).SSID);
            }
        }

        //确认单击查看当前ap或者点击开始启动压力切换才显示当前ap连接信息
        if((null != mSwitchApFragment) && !isHasAlreadyStopedStressAPOption && (isHasCheckedListDetails || isHasAlreadyStartedStressAPOption)){
            //通知fragment 现在可以显示出来
            mSwitchApFragment.showAPDeatilInfo(currentAPInfo,mScanList);
//            Log.d(TAG,currentAPInfo);
        }else{
            Log.d(TAG,"你都没点显示信息我才不显示呢");
        }

        //WIFI开关功能fragment中展示当前AP信息
        if(null != mEnableDisableWifiFragment || isHasAlreadyStartStressWifiStateOption){
            Log.d(TAG,"WIFI开关功能fragment中展示当前AP信息");
            mEnableDisableWifiFragment.showCurrentAPDeatil(currentAPInfo);
        }
    }

    private void startPeriodScan() {
        mScanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "start Scan!");
                mWifiUtils.startScan();
            }
        }, 1000, 8000/*first started after 1s, then started every 10s*/);
    }

    private boolean checkSwitchWifiResult(){
        Log.d(TAG,"检查是否切换成功 ------------");
        Log.d(TAG,"切换前 " + beforeSSID);
        Log.d(TAG,"切换后 " + getChangedSSID());
        if(beforeSSID.equals("0x") || getChangedSSID().equals("0x")){
            Log.d(TAG,"切换无效");
            mScanTimer.cancel();
            return false;
        }

        if(!getChangedSSID().equals(beforeSSID)){
            Log.d(TAG,"切换成功");
            return true;
        }else{
            Log.d(TAG,"checkSwitchWifiResult() -> 切换失败");
            //切换失败触发抓取kernel log 线程
            captureKernelLog();
            captureWifiLog();
            mHandler.sendEmptyMessage(STOP);
            isHasAlreadyStopedStressAPOption = true;
            mScanTimer.cancel();
            isStopStressThread = true;
            isStopCaptureLog = true;
            isStopEAPOLFreameThread = true;
            isStopCaptureWifiKernelLogThread = true;
            mScanTimer.cancel();
            return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //fragment start
        mFragmentManager = getFragmentManager();
        //fragment end

        mContext = getApplicationContext();
        mWifiUtils = new MiWifiUtils(mContext);

        Log.d(TAG, "create");
        mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mScanTimer = new Timer();
        mCaptureWifiLogThread = new Thread(new CaptureWifiLogThread());
        mStressThread = new Thread(new StressThread(MiWifiScanTestActivity.this));
        mEAPOLFreameThread = new Thread(new EAPOLFreameThread());
        mStartScanListCaptureKernelLogThread = new Thread(new ScanCaptureKernelLog());
        mStartStressWifiStateThread = new Thread(new StartStressWifiState(MiWifiScanTestActivity.this));
        mWifiReceiver = new WifiReceiver();
        Log.d(TAG, "scan and check create");

        //One step:request security permission
        //Two step:open GPS if needed
        //Three step:getScanList details
        initLocation();
        initViews();
        //Fore step:copy shell file from assets folder to device folder
        if(mWifiUtils.copyApkFromAssets(MiWifiScanTestActivity.this,"run_eapol_frame.sh","/data/log/run_eapol_frame.sh")){
            Log.d(TAG,"请稍等...准备拷贝抓包shell文件");
            Toast.makeText(mContext,"请稍等...准备拷贝抓包shell文件",Toast.LENGTH_SHORT).show();
        }else{
            Log.d(TAG,"请稍等...准备拷贝抓包shell文件");
            Toast.makeText(mContext,"请稍等...准备拷贝抓包shell文件",Toast.LENGTH_SHORT).show();
        }
        //register a BroadcastReceiver to the SCAN_RESULTS_AVAILABLE_ACTION in order to get the list of the available access points as soon as the scan completes
        registerScanResultAvailableBroadcast();
        registerWifiNetworkChangedBroadcase();
        mHandler = new WifiHandler();

        Toast.makeText(mContext,"欢迎来到WifiScan工具",Toast.LENGTH_SHORT).show();
        Log.d(TAG, '\n' + "欢迎来到WifiScan工具");

        if((null != mWifiUtils) && !mWifiUtils.isWiFiAvailable() && mWifiUtils.isGPSAvailable()){
            Log.d(TAG,"世界上最遥远的距离就是没网，请等待网络连接");
            Toast.makeText(mContext,"世界上最遥远的距离就是没网，请联网...",Toast.LENGTH_SHORT).show();
        }
        startPeriodScan();
    }

    //重置所有文本的选中状态
    private void setSelected(){
        Log.d(TAG,"setSelected");
//        mSwitchApNavTxt.setSelected(false);
//        mCaptureLogNavTxt.setSelected(false);
//        mEnableDisableWifiNavTxt.setSelected(false);
        mIperfNavTxt.setSelected(false);
    }

    //隐藏所有Fragment
    private void hideAllFragment(FragmentTransaction fragmentTransaction){
        if(mSwitchApFragment != null)fragmentTransaction.hide(mSwitchApFragment);
        if(mCaptureLogFragment != null)fragmentTransaction.hide(mCaptureLogFragment);
        if(mEnableDisableWifiFragment != null)fragmentTransaction.hide(mEnableDisableWifiFragment);
        if(mIperfNavFragment != null)fragmentTransaction.hide(mIperfNavFragment);
    }

    private void initViews(){
        Log.d(TAG,"initViews()");
        //通过button控件开启CheckThread线程 控制开始扫描
        setContentView(R.layout.main_layout);

        //Fragment start
        mTitleShowText = (TextView)findViewById(R.id.showToolsFeature);
        mShowAPIText = (TextView)findViewById(R.id.showApiLevel);
        mShowAPIText.setVisibility(TextView.INVISIBLE);

        API_LEVEL_NUMBER = android.os.Build.VERSION.SDK;
        api_level = Integer.valueOf(API_LEVEL_NUMBER).intValue();
        mShowAPIText = (TextView)findViewById(R.id.showApiLevel);
        mShowAPIText.setVisibility(TextView.INVISIBLE);
        if(api_level >= 27){
            Log.d(TAG,"需要展示提示");
            mShowAPIText.setText("提示：当前Android SDK level: " + API_LEVEL_NUMBER + " 无法提供抓取EAPOL帧功能");
            mShowAPIText.setVisibility(TextView.VISIBLE);
        }else{
            Log.d(TAG,"不需要展示提示");
            mShowAPIText.setVisibility(TextView.INVISIBLE);
        }
//        mSwitchApNavTxt = (TextView)findViewById(R.id.switch_ap_nav_txt);
//        mSwitchApNavTxt.setVisibility(View.INVISIBLE);
//        mCaptureLogNavTxt = (TextView)findViewById(R.id.capture_log_nav_txt);
//        mCaptureLogNavTxt.setVisibility(View.INVISIBLE);
//        mEnableDisableWifiNavTxt = (TextView)findViewById(R.id.enable_disable_wifi_nav_txt);
//        mEnableDisableWifiNavTxt.setVisibility(View.INVISIBLE);
        mIperfNavTxt = (TextView)findViewById(R.id.iperf_nav_txt);
        mContextFramelayout = (FrameLayout)findViewById(R.id.ly_content);

//        mSwitchApNavTxt.setOnClickListener(SwitchApNavListener);
//        mCaptureLogNavTxt.setOnClickListener(CaptureLogNavListener);
//        mEnableDisableWifiNavTxt.setOnClickListener(EnableDisableWifiNavListener);
        mIperfNavTxt.setOnClickListener(IperNavListener);
        mIperfNavTxt.performClick();
        //Fragment end
    }

    TextView.OnClickListener SwitchApNavListener = new TextView.OnClickListener(){
        @Override
        public void onClick(View v) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            hideAllFragment(fragmentTransaction);
            setSelected();
            Log.d(TAG,"SwitchApNavListener -> onClick");
            mSwitchApNavTxt.setSelected(true);
            if(null == mSwitchApFragment){
                mSwitchApFragment = new SwitchAPFeatureFragment();
                fragmentTransaction.add(R.id.ly_content,mSwitchApFragment);
            }else{
                fragmentTransaction.show(mSwitchApFragment);
            }
            fragmentTransaction.commit();
        }
    };

    TextView.OnClickListener CaptureLogNavListener = new TextView.OnClickListener(){
        @Override
        public void onClick(View v) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            hideAllFragment(fragmentTransaction);
            setSelected();
            mCaptureLogNavTxt.setSelected(true);
            if(null == mCaptureLogFragment){
                mCaptureLogFragment = new CaptureFeatureFragment();
                fragmentTransaction.add(R.id.ly_content,mCaptureLogFragment);
            }else{
                fragmentTransaction.show(mCaptureLogFragment);
            }
            fragmentTransaction.commit();
        }
    };

    TextView.OnClickListener EnableDisableWifiNavListener = new TextView.OnClickListener(){
        @Override
        public void onClick(View v) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            hideAllFragment(fragmentTransaction);
            setSelected();
            mEnableDisableWifiNavTxt.setSelected(true);
            if(null == mEnableDisableWifiFragment){
                mEnableDisableWifiFragment = new EnableDisableWIFIStressFeatureFragment();
                fragmentTransaction.add(R.id.ly_content,mEnableDisableWifiFragment);
            }else{
                fragmentTransaction.show(mEnableDisableWifiFragment);
            }
            fragmentTransaction.commit();
        }
    };

    TextView.OnClickListener IperNavListener = new TextView.OnClickListener(){
        @Override
        public void onClick(View v) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            hideAllFragment(fragmentTransaction);
            setSelected();
            mIperfNavTxt.setSelected(true);
            if(null == mIperfNavFragment){
                mIperfNavFragment = new IperfFeatureFragment();
                fragmentTransaction.add(R.id.ly_content,mIperfNavFragment);
            }else{
                fragmentTransaction.show(mIperfNavFragment);
            }
            fragmentTransaction.commit();
        }
    };

    public void getEAPOLIsAvailable(){
        if(api_level >= 27){
            Log.d(TAG,"level >= 27 Android 8.1 can not support eapol func.");
            Toast.makeText(mContext,"Android 8.1 不支持此功能",Toast.LENGTH_SHORT).show();
            //Android 8.1 can not support eapol func.
        }else if(mEAPOLFreameThread.getState() == Thread.State.NEW){
            //Android 6.0 support
            Log.d(TAG,"level < 27");
            mEAPOLFreameThread.start();
            Toast.makeText(mContext,"开启线程收集EAPOL帧",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"getEAPOL started");
        }
    }

    //checkAPListButtonListener　供fragment调用的方法
    public void checkAPList(){
        if(isAvailableCheckScanResult){
            isHasCheckedListDetails = true;
            Toast.makeText(mContext,"点击查看当前ap连接信息",Toast.LENGTH_SHORT).show();
            checkAPIsReadyToShowAndShowScanListDetails();
        }else{
            Log.d(TAG,"please wait for get the list of the available");
            Toast.makeText(mContext,"请等待系统获取scan list,请稍后再试",Toast.LENGTH_SHORT).show();
        }
    }

    public void checkAPIsReadyToShowAndShowScanListDetails(){
        if((null != mScanList) && (mScanList.size() != 0) && mWifiUtils.startScan() == true){
            showScanListDetils();
        }else{
            Log.d(TAG,"please wait for get the list of the available");
//            Toast.makeText(mContext,"请等待系统获取scan list,请稍后再试",Toast.LENGTH_SHORT).show();
        }
    }

    public void connectCustomNetwork(String ssid,String pwd) throws Exception{
        int toConnectSSID;
        boolean isAddSSIDSuccess;
        if(mWifiUtils.removeSSID(ssid)){
            //目前问题：当SSID为系统自己的也会删除成功
            //删除成功说明该ssid为APP创建
            WifiConfiguration wifiConfiguration = mWifiUtils.CreateWifiInfo(ssid,pwd,mWifiUtils.TYPE_WPA);
            isAddSSIDSuccess = addCustomNetwork(wifiConfiguration);
            if(isAddSSIDSuccess){
                //新增ssid连接成功后可以进行压力测试
                isHasAlreadyStopedStressAPOption = false;
                Log.d(TAG,"新增ssid连接成功后可以进行压力测试");
            }
            Log.d(TAG,"删除成功说明该ssid为APP创建, 然后再添加自定义ssid的结果:" + isAddSSIDSuccess);
            //isAddSSIDSuccess 返回true 并不能代表热点连接成功，但是返回false一定代表连接不成功,密码位数不对也无法连接成功
        }else if(mWifiUtils.getExitsWifiConfig(ssid) != null){
            //是系统创建
            //当前ssid系统创建的,连接过,切换到当前ssid上
            Log.d(TAG,"当前要连接的ssid是系统创建的，切换到当前ssid上");
            toConnectSSID = mWifiUtils.getExitsWifiConfig(ssid).networkId;
            mWifiUtils.enableNetwork(toConnectSSID,true);
        }else{
            WifiConfiguration wifiConfiguration = mWifiUtils.CreateWifiInfo(ssid,pwd,mWifiUtils.TYPE_WPA);
            isAddSSIDSuccess = addCustomNetwork(wifiConfiguration);
            if(isAddSSIDSuccess){
                //新增ssid连接成功后可以进行压力测试
                isHasAlreadyStopedStressAPOption = false;
                Log.d(TAG,"新增ssid连接成功后可以进行压力测试");
            }
            Log.d(TAG,"连接从未连接过ssid的结果:" + isAddSSIDSuccess);
            //isAddSSIDSuccess 返回true 并不能代表热点连接成功，但是返回false一定代表连接不成功,密码位数不对也无法连接成功
        }
    }

    //添加自定义WifiConfiguration并连接
    public boolean addCustomNetwork(WifiConfiguration configuration) {
        //step 1:addNetwork
        int newID = mWifiManager.addNetwork(configuration);
        //step 2:enableNetwork
        boolean enableNetworkRet =  mWifiUtils.enableNetwork(newID, true);
//        boolean isSavedNewSSIDConfig = mWifiManager.saveConfiguration();
        Log.d(TAG,"自定义newID: " + newID + " ; enableNetwork结果: " + enableNetworkRet /*+ "isSavedNewSSIDConfig: " + isSavedNewSSIDConfig*/);
        if(newID == 0){

        }
        if(newID == -1){
            //说明这个ssid不是自定义的，请删除系统连接或取消保存
            Log.d(TAG,"请取消当前SSID连接或取消保存");
            Toast.makeText(mContext,"请取消当前SSID连接或取消保存",Toast.LENGTH_SHORT).show();
            return false;
        }else if(!enableNetworkRet){
            Log.d(TAG,"连接自定义Config网络失败");
            Toast.makeText(mContext,"连接自定义Config网络失败",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(mContext,"连接网络成功",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"连接成功");
        }
        return enableNetworkRet;
    }

    //remove all ssid before exit APP，该函数存在是为了删除MiOffice
    public void removeAllSSID(){
        List<WifiConfiguration> configurations = mWifiUtils.getConfiguredNetworks();
        for(WifiConfiguration wc:configurations){
            mWifiManager.removeNetwork(wc.networkId);
            Log.d(TAG,"要删除的ssid:" + wc.SSID.toString());
        }
        mWifiManager.saveConfiguration();
    }

    private String beforeSSID = "";
    public boolean stressChangeSavedNetwork(){
        //返回false代表切换网络失败或状态不对，return true cause 网络连接数不对或压力测试成功
        //stress step 1:
        if(isHasAlreadyStopedStressAPOption){
            return false;
        }

        if(null == saveConfigured || null == mWifiUtils.getWifiInfo()){
            //case :在切换过程中手动断开wifi可用状态，抓取kernel log
            Log.d(TAG,"null == saveConfigured or null == getWifiInfo()");
            Log.d(TAG,"获取wifi info出错，触发抓log操作，稍后请查看log文件");
            captureKernelLog();
            captureWifiLog();
            mScanTimer.cancel();
            return false;
        }
        //获得切换前ssid
        beforeSSID = mWifiUtils.getWifiInfo().getSSID();
        if(stressChangeSavedNetwork(saveConfigured,mWifiUtils.getWifiInfo())){
            miWifiScanTestActivityParent.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //更新TextView 不及时,在receive 处理中更新
                    Toast.makeText(miWifiScanTestActivityParent.getBaseContext(),"切换网络成功!",Toast.LENGTH_SHORT).show();
                }
            });
            Log.d(TAG, "-------------stressChangeSavedNetwork() -> 切换ap后连接结果: true  " + "连接状态结果" + mWifiManager.isWifiEnabled());
            checkSwitchWifiResult();
        }else{
            miWifiScanTestActivityParent.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //切换后没网 failed
                    Toast.makeText(miWifiScanTestActivityParent.getBaseContext(),"切换网络失败!",Toast.LENGTH_SHORT).show();
                }
            });

            if(saveConfigured.size() < AVAILABLE_SWITCH_SSID_COUNT){
                Log.d(TAG,"当前Wifi列表中没有两个以上saved config");
                miWifiScanTestActivityParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(miWifiScanTestActivityParent.getBaseContext(), "请先确认已经连接上网络并保证有两个以上已保存的AP", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.d(TAG,"stressChangeSavedNetwork(saveConfigured,mWifiUtils.getWifiInfo() 返回false 因为saved count < 2");
                //返回false代表切换网络失败，return true cause 网络连接数不对
            }else{
                miWifiScanTestActivityParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(miWifiScanTestActivityParent.getBaseContext(),"因联网失败!触发抓log线程，请重新开始",Toast.LENGTH_SHORT).show();
                    }
                });
                Log.d(TAG,"stressChangeSavedNetwork(saveConfigured,mWifiUtils.getWifiInfo() 返回false 因为联网失败");
                captureKernelLog();
                captureWifiLog();
                mScanTimer.cancel();
                return false;
            }
        }
        return true;
    }

    public void showScanListDetils(){
        //返回list
        if(mWifiUtils.startScan() == true && (null != mScanList)){
//            Log.d(TAG,"扫描到wifi列表长度 = "+ mScanList.size());
            showScanList();
        }
    }

    public void openGPS(){
        if(mWifiUtils.isGPSAvailable()){
            Log.d(TAG,"openGPS-> GPS opened" + mWifiUtils.isGPSAvailable());
        }else{
            Log.d(TAG,"openGPS-> GPS未开启，需要打开GPS...");
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("打开GPS");
            dialog.setPositiveButton("确定",new android.content.DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //开启Location settings
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, RESULT_CODE_FOR_OPEN_GPS);
                }
            });

            dialog.setNegativeButton("取消",new android.content.DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //用户开启Location on同样需要等待SCAN_RESULTS_AVAILABLE_ACTION后才能查看AP列表
        //TODO 需要处理，还没想好怎么处理
//        registerScanResultAvailableBroadcast();
        if(requestCode == RESULT_CODE_FOR_OPEN_GPS && mWifiUtils.isGPSAvailable()){
            Toast.makeText(mContext,"开启Location成功！",Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(mContext,"没有开启Location！",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //这里进行授权被允许的处理,用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                Log.d(TAG,"onRequestPermissionsResult-> 允许用户修改权限");
                openGPS();
            } else {
                Log.d(TAG,"onRequestPermissionsResult-> 不允许用户修改权限");
                Log.d(TAG,"请确认允许该程序访问位置权限,没有Location权限无法获得ap list");
                Toast.makeText(mContext,"没有访问位置权限10秒后将关闭应用程序",Toast.LENGTH_SHORT).show();
                //TODO 没有访问位置权限10秒后将关闭
            }
        } else {
            Log.d(TAG,"onRequestPermissionsResult-> 返回requestCode不是REQUEST_CODE_ACCESS_COARSE_LOCATION权限");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void initLocation(){
        if(ContextCompat.checkSelfPermission(mContext,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_DENIED){
            //检查权限成功　ACCESS_FINE_LOCATION/ACCESS_COARSE_LOCATION
            Log.d(TAG,"into initLocation get permission success");
            openGPS();
        }else{
            //检查权限失败,申请权限,一组权限申请ACCESS_FINE_LOCATION或者ACCESS_COARSE_LOCATION
            ActivityCompat.requestPermissions(this,new String[]{ACCESS_FINE_LOCATION},REQUEST_CODE_ACCESS_COARSE_LOCATION);
            Log.d(TAG,"into initLocation get permission fail , to request permission ,mWifiUtils.startScan() = " + mWifiUtils.startScan());
        }
    }

    public String getChangedSSID(){
        return changedSSID;
    }

    public void setChangedSSID(String ssid){
        changedSSID = ssid;
    }

    private String changedSSID = "";
    public boolean stressChangeSavedNetwork(List<WifiConfiguration> config, WifiInfo savedInfo){
        //stress step 2 enableNetwork
        Log.d(TAG,"Utils::stressChangeSavedNetwork() in ");
        boolean isEnabledNextSSIDSuccess = false;

        for(int i = 0 ; i < config.size();i++){
            Log.d(TAG,"遍历已保存配置列表值 = "+config.get(i).SSID);
        }

        if(config.size() < AVAILABLE_SWITCH_SSID_COUNT){
            Log.d(TAG,"当前Wifi列表中没有两个以上saved config");
            miWifiScanTestActivityParent.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(miWifiScanTestActivityParent.getBaseContext(), "请先确认已经连接上网络并保证有两个以上已保存的AP", Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }

        for(int i = 0;i < config.size();i++){
            if(config.get(i).SSID.equals(savedInfo.getSSID())  && config.size() >= i + 1){
                if(config.size() == i + 1){
                    isEnabledNextSSIDSuccess = mWifiUtils.enableNetwork(config.get(0).networkId, true);
                    setChangedSSID(config.get(0).SSID);
                    break;
                }else{
                    isEnabledNextSSIDSuccess = mWifiUtils.enableNetwork(config.get(i + 1).networkId, true);
                    setChangedSSID((config.get(i + 1).SSID));
                    break;
                }
            }else{
//                Log.d(TAG,"savedInfo.getSSID() = " + savedInfo.getSSID() +  "i: " + i + "config.get(i).SSID = " + config.get(i).SSID + "config.size() = " + config.size());
            }
        }
        return isEnabledNextSSIDSuccess;
    }

    class WifiHandler extends Handler{
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case START:
                    Log.d(TAG,"发送 START 成功");

                    break;
                case STOP:
                    isStopCaptureLog = true;
                    isStopStressThread = true;
                    isStopEAPOLFreameThread = true;
                    isStopCaptureWifiKernelLogThread = true;
                    mScanTimer.cancel();
                    break;
                case CONNECT_WIFI:
//                    connectCustomNetwork(newSSID,newPassword);
                    break;
                case STRESS_WIFI_STATE:
                    startSwitchWifiState();
                    break;
                case UPDATE_FILE_PATH:
                    getCacheDirFile();
                    break;
            }
        }
    }

    ////////////////////////////////////enable disable start///////////////////////////////////////////////////////////////
    //供Fragment调用,仅用于启动线程
    public void startStressWifiState(){
        if(isHasAlreadyStartedStressAPOption){
            Toast.makeText(mContext,"切换ap操作正在进行,请先停止",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"stressConnectButtonListener::切换ap操作正在进行,请先停止!");
            return;
        }

        if(isAvailableStressWifiStateOption && !isHasAlreadyStartStressWifiStateOption){
            if(mStartStressWifiStateThread.getState() == Thread.State.NEW){
                mStartStressWifiStateThread.start();

                //标记切换wifi状态开始了
                isHasAlreadyStartStressWifiStateOption = true;
                Toast.makeText(mContext,"开启线程压力wifi 状态开关",Toast.LENGTH_SHORT).show();
                Log.d(TAG,"开启线程压力wifi 状态开关");
            }else{
                Toast.makeText(mContext,"线程已存在,请重新开始",Toast.LENGTH_SHORT).show();
                Log.d(TAG,"线程已存在，请重新开始");
            }
        }else{
            Toast.makeText(mContext,"当前不可进行wifi开关压力操作，稍等或重新开始",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"当前不可进行wifi开关压力操作，稍等或重新开始");
        }
    }

    //供Fragment调用,停止压力开关
    public void stopStressWifiState(){
        if(isHasAlreadyStartStressWifiStateOption){
            Toast.makeText(mContext,"停止压力wifi 状态开关",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"停止压力wifi 状态开关");
            isHasAlreadyStopStressWifiStateOption = true;
            isStopStressWIFIState = true;
            isHasAlreadyStartStressWifiStateOption = false;
        }else if(!isHasAlreadyStopStressWifiStateOption){
            Toast.makeText(mContext,"都没开始开关测试",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"都没开始开关测试");
        }else{
            Toast.makeText(mContext,"都没开始",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"都没开始");
        }
    }

    //压力切换wifi开关线程
    class StartStressWifiState extends Thread{
        public StartStressWifiState(MiWifiScanTestActivity activity){
            miWifiScanTestActivityParent = activity;
        }
        @Override
        public void run() {
            super.run();
            while (!isStopStressWIFIState){
                startSwitchWifiState();
            }
        }
    }
    //线程调用该函数切换WIFI 可用/不可用
    public void startSwitchWifiState(){
        if(!isStopStressWIFIState){
            setWifiStateDisabled();
        }
    }
    public void setWifiStateDisabled() {
        if (!isStopStressWIFIState && mWifiManager.isWifiEnabled()) {
            Log.d(TAG,"-----------------切换前-------------------");
            getWifiState();
            Log.d(TAG,"-----------------准备开始切换-------------------");
            miWifiScanTestActivityParent.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(miWifiScanTestActivityParent.getBaseContext(),"准备开始切换",Toast.LENGTH_SHORT).show();
                }
            });
            boolean result = mWifiManager.setWifiEnabled(false);
            SystemClock.sleep(7000);
            Log.d(TAG,"-----------------切换完毕-------------------");
            getWifiState();
            if (isAvailableStressWifiStateOption && result && !mWifiManager.isWifiEnabled() && !mWifiUtils.isWiFiAvailable()) {
                miWifiScanTestActivityParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(miWifiScanTestActivityParent.getBaseContext(),"切换wifi状态成功!",Toast.LENGTH_SHORT).show();
                    }
                });
                Log.d(TAG,"--------------切换wifi状态成功----------------------");
                getWifiState();
            } else {
                miWifiScanTestActivityParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(miWifiScanTestActivityParent.getBaseContext(),"切换wifi状态失败!",Toast.LENGTH_SHORT).show();
                    }
                });
                Log.d(TAG,"===============切换wifi状态失败!=====================");
                getWifiState();
                captureKernelLog();
                captureWifiLog();
                mScanTimer.cancel();
                isStopStressWIFIState = true;
            }
        }else{
            setWifiStateEnabled();
        }
    }
    public void setWifiStateEnabled(){
        if(!isStopStressWIFIState && !mWifiManager.isWifiEnabled()){
            Log.d(TAG,"-----------------切换前-------------------");
            getWifiState();
            miWifiScanTestActivityParent.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(miWifiScanTestActivityParent.getBaseContext(),"准备开始切换",Toast.LENGTH_SHORT).show();
                }
            });
            Log.d(TAG,"-----------------准备开始切换-------------------");
            boolean result = mWifiManager.setWifiEnabled(true);
            SystemClock.sleep(7000);
            Log.d(TAG,"-----------------切换完毕-------------------");
            if(isAvailableStressWifiStateOption && result && mWifiManager.isWifiEnabled() && mWifiUtils.isWiFiAvailable()){
                miWifiScanTestActivityParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(miWifiScanTestActivityParent.getBaseContext(),"切换wifi状态成功!",Toast.LENGTH_SHORT).show();
                    }
                });
                Log.d(TAG,"--------------切换wifi状态成功----------------------");
                getWifiState();
            }else{
                miWifiScanTestActivityParent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(miWifiScanTestActivityParent.getBaseContext(),"切换wifi状态失败!",Toast.LENGTH_SHORT).show();
                    }
                });
                Log.d(TAG,"===============切换wifi状态失败=====================");
                getWifiState();
                captureKernelLog();
                captureWifiLog();
                mScanTimer.cancel();

                isStopStressWIFIState = true;
            }
        }else{
            setWifiStateDisabled();
        }
    }
    //判断wifi状态
    public void getWifiState(){
        switch (mWifiManager.getWifiState()){
            case WIFI_STATE_DISABLING:
                Log.d(TAG, "当前wifi状态为: " + "------------------ WIFI_STATE_DISABLING ------------------");
                break;
            case WIFI_STATE_DISABLED:
                Log.d(TAG, "当前wifi状态为: " + "------------------ WIFI_STATE_DISABLED ------------------");
                break;
            case WIFI_STATE_ENABLED:
                Log.d(TAG, "当前wifi状态为: " + "------------------ WIFI_STATE_ENABLED ------------------");
                break;
            case WIFI_STATE_ENABLING:
                Log.d(TAG, "当前wifi状态为: " + "------------------ WIFI_STATE_ENABLING ------------------");
                break;
            case WIFI_STATE_UNKNOWN:
                Log.d(TAG, "当前wifi状态为: " + "------------------ WIFI_STATE_UNKNOWN ------------------");
                break;
                default:
                    Log.d(TAG, "当前wifi状态为: " + "------------------WIFI_STATE_UNKNOWN ------------------");
                    break;
        }
    }
    ////////////////////////////////////enable disable stop///////////////////////////////////////////////////////////////
    ////////////////////////////////////stress ap start///////////////////////////////////////////////////////////////////
    //stressConnectButtonListener 供fragment 调用的方法
    public void stressConnect(){
        //这里是主线程
        if(isHasAlreadyStopedStressAPOption){
            Toast.makeText(mContext,"请返回应用重新开始",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"stressConnectButtonListener::返回应用重新开始!");
            return;
        }

        //wifi 开关切换不允许切换ap操作
        if(isHasAlreadyStartStressWifiStateOption){
            Toast.makeText(mContext,"切换wifi开关操作正在进行,请先停止",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"stressConnectButtonListener::切换wifi开关操作正在进行,请先停止!");
            return;
        }

        if(isAvailableCheckScanResult && !isHasAlreadyStartedStressAPOption){
            Toast.makeText(mContext,"开始反复切换连接操作",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"stressConnectButtonListener::开始反复切换连接操作");
            Log.d(TAG,"----------------------------" + mStressThread.getState());
            if(mStressThread.getState() == Thread.State.NEW){
                mStressThread.start();
                //此标记证明切换ap操作开始
                isHasAlreadyStartedStressAPOption = true;
            }else{
                Log.d(TAG,"压力切换ap线程已启动");
            }
        }else{
            if(isHasAlreadyStartedStressAPOption){
                Log.d(TAG,"StressThread 已启动");
                Toast.makeText(mContext,"StressThread 已启动",Toast.LENGTH_SHORT).show();
            }else{
                Log.d(TAG,"please wait for get the list of the available");
                Toast.makeText(mContext,"请等待系统获取scan list,请稍后再试",Toast.LENGTH_SHORT).show();
            }
        }
    }

    //stop stress ap connect 供fragment调用
    public void stopStressConnect(){
        if(isHasAlreadyStartedStressAPOption){
            Toast.makeText(mContext,"停止反复切换连接操作",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"stressConnectButtonListener::停止反复切换连接!");
            mHandler.sendEmptyMessage(STOP);
            isHasAlreadyStopedStressAPOption = true;
            isHasAlreadyStartedStressAPOption = false;
            mScanTimer.cancel();

            //TODO:在8.1中是否是通过这个参数改变来停止抓log的
            isStopCaptureLog = true;
            isStopStressThread = true;
            isStopEAPOLFreameThread = true;
            isStopCaptureWifiKernelLogThread = true;
        }else{
            Toast.makeText(mContext,"请先开始压力切换操作再停止",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"stressConnectButtonListener::请先开始压力切换操作!");
        }
    }

    class StressThread extends Thread{
        public StressThread(MiWifiScanTestActivity activity){
            miWifiScanTestActivityParent = activity;
        }

        @Override
        public void run() {
            //这里是子线程
            Log.d(TAG,"Thread.currentThread().getName(): " + Thread.currentThread().getName());
            try{
                Thread.sleep(5000);
                Log.d(TAG,"isStopStressThread: " + isStopStressThread);
                while(!isStopStressThread){
                    //在子线程中进行压力测试
                    if(!stressChangeSavedNetwork()){
                        //返回false因为点击压力切换后没网或网络切换失败
                        isHasAlreadyStopedStressAPOption = true;
                    }
                    //发送消息给主线成进行切换压力测试
                    Message msg = new Message();
                    msg.what = START;
                    mHandler.sendEmptyMessageDelayed(msg.what,5000);

                    if(Thread.currentThread().isInterrupted()){
                        Log.d(TAG,"thread stop!");
                        return;
                    }
                    Log.d(TAG,"循环发送成功");
                    Thread.sleep(10000);
                }
            }catch (InterruptedException mayStopThread){
                mayStopThread.printStackTrace();
            }
        }
    }
    ////////////////////////////////////stress ap stop/////////////////////////////////////////////////////////////////////
    class EAPOLFreameThread extends Thread{
        @Override
        public void run() {
            super.run();
            Log.d(TAG,"Thread.currentThread().getName(): " + Thread.currentThread().getName());
            try{
                Log.d(TAG,"isStopEAPOLFreameThread: " + isStopEAPOLFreameThread);
                if(!isStopEAPOLFreameThread){
                    captureEAPOLframe();
                    Thread.sleep(5000);

                    if(Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "thread stop!");
                        return;
                    }
                    Log.d(TAG, "check pass");
                }
                Thread.sleep(10000);
            }catch (InterruptedException mayStopThread){
                mayStopThread.printStackTrace();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        public void captureEAPOLframe() throws ClassNotFoundException,NoSuchMethodException,InstantiationException,IllegalAccessException,InvocationTargetException{
            String path = "/data/log/";
            File eapolFile = new File(path);
            if(!eapolFile.exists()){
                boolean res = eapolFile.mkdir();
                Log.d(TAG,"创建的文件路径为: " + eapolFile + " ; 创建文件结果为 res: " + res);
            }else{
                Log.d(TAG,"文件路径已经创建,无需再次创建");
            }

            Class<?> propertiesClass = Class.forName("android.os.SystemProperties");
            Method setMethod = propertiesClass.getMethod("set",String.class,String.class);

            Log.d(TAG,"STEP one: grant permission");
            String cmd_chmod = "misysdiagnose:-s chmod,777,/data/log/run_eapol_frame.sh";
            setMethod.invoke(propertiesClass.newInstance(),"ctl.start",cmd_chmod);
            SystemClock.sleep(1000);
            Log.d(TAG,"STEP two: EXECUTE cmd shell file");
            String cmd = "misysdiagnose:-s /data/log/run_eapol_frame.sh";
            SystemClock.sleep(1000);
            Log.d(TAG,"cmd: " + cmd + "  cmd.lenth = " + cmd.length());
            setMethod.invoke(propertiesClass.newInstance(),"ctl.start",cmd);
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////
    //启动线程抓wifi tag log
    public void startScanThreadCaptureWifiLog(){
        if(isHasAlreadyStopedStressAPOption){
            Toast.makeText(mContext,"用户将切换操作停止或抓取log后,请返回应用重新开始",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"stressConnectButtonListener::用户将切换操作停止或抓取log后,请返回应用重新开始!");
            return;
        }

        //避免多次点击造成线程restart
        if(mCaptureWifiLogThread.getState() == Thread.State.NEW){
            mCaptureWifiLogThread.start();
            Toast.makeText(mContext,"开启线程抓取wifi tag log",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"开启线程抓取wifi tag log");
        }else{
            Toast.makeText(mContext,"wifi tag log线程已存在，请查看",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"抓取wifi tag log 线程已存在，请查看");
        }
    }

    //启动线程抓wifi tag log
    public void startWifiScanListCaptureKernelLog(){
        //如果压力操作停止,不进行抓取kernel log操作
        if(isHasAlreadyStopedStressAPOption){
            Toast.makeText(mContext,"用户将切换操作停止或抓取log后,请返回应用重新开始",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"stressConnectButtonListener::用户将切换操作停止或抓取log后,请返回应用重新开始!");
            return;
        }

        //启动线程
        if(mStartScanListCaptureKernelLogThread.getState() == Thread.State.NEW){
            mStartScanListCaptureKernelLogThread.start();
            Toast.makeText(mContext,"开启线程抓取Kernel log",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"开启线程抓取kernel log");
        }else{
            Toast.makeText(mContext,"kernel log线程已存在，请查看",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"kernel log线程已存在，请查看");
        }
    }
    class CaptureWifiLogThread implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(10000);
                captureWifiLog();
                while (true) {
                    if (mScanList == null) continue;
                    if (mScanList.isEmpty()) {
                        Log.e(TAG, "SCAN_ERROR! save kernel logs!");
                        captureWifiLog();
                    }
                    if(Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "thread stop!");
                        break;
                    }
                    Thread.sleep(10000);
                }
            } catch (InterruptedException mayStopThread) {
                    mayStopThread.printStackTrace();
            }
        }
    }

    private void captureWifiLog(){
        //step prepare for cmd
        String[] command = new String[]{"logcat","-s","grep",
                "*:S wifi",
                "*:S WIFI",
                "*:S wifi_gbk2utf",
                "*:S wpa_supplicant",
                "*:S WifiStateMachine",
                "*:S WifiAutoJoinController",
                "*:S WifiConfigStore",
                "*:S WifiService",
                "*:S WifiP2pService",
                "*:S NetworkStateManager",
                "*:S NetworkStateView",
                "*:S WifiNetworkAgent",
                "*:S WifiNative-HAL",
                "*:S WIFI_UT",
                "*:S WIFI",
                "*:S Ethernet",
                "*:S NetworkMonitor",
                "*:S NetworkAgentInfo",
                "*:S WifiDisplayController",
                "*:S WifiDisplayAdapter",
                "*:S DisplayManagerService",
                "*:S RemoteDisplay",
                "*:S WifiDisplaySink",
                "*:S wfd_client",
                "*:S NetworkSession",
                "*:S NuPlayer",
                "*:S WifiTracker",
                "*:S SettingsLib.AccessPoint",
                "*:S WifiConfigController",
                "*:S ConnectivityService",
                "*:S WifiSetting",
                "*:S WifiConnectivityManager",
                "*:S MitvAccessPoint",
                "*:S MitvWifiActivity",
                "*:S chromium"};
        Process process = null;
        try{
            process = Runtime.getRuntime().exec(command);
        }catch (Exception e){
            e.printStackTrace();
        }

        //step 1:获取输入流
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String errorTime = dateFormat.format(now);
        String fileName = "WifiTagLog_" + errorTime + ".log";

        //step 2:创建保存log的路径
        File saveLogFile = new File(mContext.getCacheDir() + "/wifi");
        if(saveLogFile.exists()){
            Log.d(TAG,"文件路径: " + saveLogFile.getAbsolutePath() + "已创建,无需再次创建");
        }else{
            boolean res = saveLogFile.mkdir();
            Log.d(TAG,"wifi tag文件路径为: " + saveLogFile.getAbsolutePath() + " ;创建结果为: " + res);
        }

        //step 3:将执行结果放在文件中
        File resultFile = new File(saveLogFile.getAbsolutePath(), fileName);
        if(null == resultFile){
            Log.d(TAG,"resultFile == null");
            return;
        }else{
            //step 4:执行结果保存在文件中
            copyInputStreamToFile(process.getInputStream(),resultFile);
        }
    }

    class ScanCaptureKernelLog extends Thread{
        @Override
        public void run() {
            try{
                Thread.sleep(10000);
                if(!isStopCaptureWifiKernelLogThread){
                    captureKernelLog();
                    while(!isStopCaptureWifiKernelLogThread){
                        final List<ScanResult> results = mWifiUtils.getScanResults();
                        if (results == null) continue;
                        if (results.isEmpty()) {
                            Log.e(TAG, "SCAN_ERROR! save kernel logs!");
                            captureKernelLog();
                        } else {
                            Log.d(TAG,"SCAN PASS!");
                        }
                        if(Thread.currentThread().isInterrupted()) {
                            Log.d(TAG, "thread stop!");
                            return;
                        }
                        Log.d(TAG, "check pass");
                        Thread.sleep(10000);
                    }
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            super.run();
        }
    }

    private void captureKernelLog(){
        //step prepare for cmd
        Log.d(TAG,"---------------bugreport or dmesg---------------");
        String cmd = "";
        Log.d(TAG, "Current API LEVEL:" + api_level);
        if(api_level >= 27){
            Log.d(TAG,"level >= 27");
            //Android 8.1 use bugreport to capture kernel log
            cmd = "/system/bin/bugreport";
        }else{
            //Android 6.0
            Log.d(TAG,"level < 27");
            cmd = "/system/bin/dmesg";
        }
        Process process = null;
        try{
            process = Runtime.getRuntime().exec(cmd);
        }catch (Exception e){
            e.printStackTrace();
        }
        //step 1:获取输入流
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String errorTime = dateFormat.format(now);
        String fileName = "WifiScanResultKernelLog_" + errorTime + ".log";

        //step 2:创建保存log的路径
        File saveLogFile = new File(mContext.getCacheDir() + "/wifi");
        if(saveLogFile.exists()){
            Log.d(TAG,"文件路径: " + saveLogFile.getAbsolutePath() + "已创建,无需再次创建");
        }else{
            boolean res = saveLogFile.mkdir();
            Log.d(TAG,"kernel log文件路径为: " + saveLogFile.getAbsolutePath() + " ;创建结果为: " + res);
        }

        //step 3:将执行结果放在文件中
        File resultFile = new File(saveLogFile.getAbsolutePath(), fileName);
        if(null == resultFile){
            Log.d(TAG,"resultFile == null");
            return;
        }else{
            //step 4:执行结果保存在文件中
            copyInputStreamToFile(process.getInputStream(), resultFile);
        }
        isStopCaptureWifiKernelLogThread = true;
    }

    private void copyInputStreamToFile(InputStream in, File file ) {
        try {
            OutputStream out = new FileOutputStream(file);
            String newline = System.getProperty("line.separator");
            byte[] buf = new byte[1024];
            int len;
            String line;
            BufferedReader tbuf = new BufferedReader(new InputStreamReader(in));
            //循环读取每一行
            while (!isStopCaptureLog && (line = tbuf.readLine()) != null) {
                out.write(line.getBytes());
                out.write(newline.getBytes());
            }
            while(!isStopCaptureLog && (len = in.read(buf)) > 0){
                out.write(buf,0,len);
                out.write(newline.getBytes());
            }
            out.close();
            Log.d(TAG,"capture log end........");
            //发送消息给主线成进行切换压力测试
            Message msg = new Message();
            msg.what = UPDATE_FILE_PATH;
            mHandler.sendEmptyMessageDelayed(msg.what,5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerScanResultAvailableBroadcast(){
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mContext.registerReceiver(mWifiReceiver, mIntentFilter);
    }

    public void registerWifiNetworkChangedBroadcase(){
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mWifiReceiver, mIntentFilter);
    }

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TAG, "监听系统intent is calleld with：" + intent);
            if((!isHasAlreadyStopedStressAPOption || !isHasAlreadyStopStressWifiStateOption) && action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
                synchronized (mWifiScanResultLock) {
                    Log.d(TAG,"监听到：WIFI_STATE_CHANGED_ACTION");
                    saveConfigured = mWifiUtils.getConfiguredNetworks();
                    checkAPIsReadyToShowAndShowScanListDetails();

                    isAvailableCheckScanResult = true;
                    isAvailableStressWifiStateOption = true;

                    mWifiScanResultLock.notifyAll();
                }
            }

            if ((!isHasAlreadyStopedStressAPOption || !isHasAlreadyStopStressWifiStateOption) && action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                synchronized (mWifiScanResultLock) {
                    mScanList = mWifiUtils.getScanResults();
                    //对收集到的ssid信号排序
                    mWifiUtils.sortByLevel(mScanList);
                    Log.d(TAG,"监听到：SCAN_RESULTS_AVAILABLE_ACTION");
                    saveConfigured = mWifiUtils.getConfiguredNetworks();
                    checkAPIsReadyToShowAndShowScanListDetails();

                    mWifiScanResultLock.notifyAll();
                }
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Toast.makeText(mContext,"欢迎又回到WifiScan工具",Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onRestart" + '\n' + "欢迎又回到WifiScan工具");
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "start");
    }

    @Override
    public void onResume() {
        super.onResume();
        mWifiUtils.startScan();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mScanTimer.cancel();

        //停止三个线程
        isStopStressThread = true;
        isStopCaptureLog = true;
        isStopEAPOLFreameThread = true;
        isStopCaptureWifiKernelLogThread = true;

        if(mWifiReceiver != null){
            mContext.unregisterReceiver(mWifiReceiver);
            mWifiReceiver = null;
        }
        super.onDestroy();

        if(null != mEnableDisableWifiFragment){
            mEnableDisableWifiFragment.showCachePathEmpty("");
        }else if(null != mSwitchApFragment){
            mSwitchApFragment.showCachePathEmpty("");
        }
    }
}