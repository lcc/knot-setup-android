package br.org.cesar.knot_setup_app.views;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

import br.org.cesar.knot_setup_app.R;
import br.org.cesar.knot_setup_app.domain.callback.DeviceCallback;
import br.org.cesar.knot_setup_app.knotSetupApplication;
import br.org.cesar.knot_setup_app.model.BluetoothDevice;
import br.org.cesar.knot_setup_app.persistence.mysqlDatabase.DBHelper;
import br.org.cesar.knot_setup_app.wrapper.BluetoothWrapper;

public class configureGatewayActivity extends AppCompatActivity {

    private BluetoothWrapper bluetoothWrapper;
    private BluetoothDevice device;
    private Integer read_count = 0;
    private Integer write_count = 0;
    private boolean readDone = false;
    private boolean writeDone = false;
    private DBHelper mydb;
    private Gateway gateway;

    private final UUID otSettingsService = UUID.fromString("a8a9e49c-aa9a-d441-9bec-817bb4900d30");
    private final UUID ChannelCharacteristic = UUID.fromString("a8a9e49c-aa9a-d441-9bec-817bb4900d31");
    private final UUID NetNameCharacteristic = UUID.fromString("a8a9e49c-aa9a-d441-9bec-817bb4900d32");
    private final UUID PanIDCharacteristic = UUID.fromString("a8a9e49c-aa9a-d441-9bec-817bb4900d33"); //Int
    private final UUID XpanidCharacteristic = UUID.fromString("a8a9e49c-aa9a-d441-9bec-817bb4900d34");
    private final UUID MasterKeyCharacteristic = UUID.fromString("a8a9e49c-aa9a-d441-9bec-817bb4900d35");

    private final UUID IPV6Service = UUID.fromString("49601183-5db4-498b-b35a-e6ddbe1c1470");
    private final UUID IPV6Characteristic = UUID.fromString("49601183-5db4-498b-b35a-e6ddbe1c1471");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure_gateway);

        this.bluetoothWrapper = knotSetupApplication.getBluetoothWrapper();
        this.device = knotSetupApplication.getBluetoothDevice();

        Log.d("DEV-LOG", this.device.getDevice().getName());

        gateway = new Gateway();
        gateway.name = device.getDevice().getName();

        callbackFlux();
    }

    private void callbackFlux(){

        Log.d("DEV-LOG","CallbackFlux");
        bluetoothWrapper.waitForBonding(device, new DeviceCallback() {
            @Override
            public void onConnect() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Scan for device " +
                                "successful!", Toast.LENGTH_LONG).show();
                    }
                });
                Log.d("DEV-LOG","OnConnect");
                bluetoothWrapper.discoverServices();
            }

            @Override
            public void onCharacteristicChanged(){
                Log.d("DEV-LOG","Characteristic changed?");
            }

            @Override
            public void onDisconnect(){
                Log.d("DEV-LOG","Disconnected");
                bluetoothWrapper.closeGatt();
                finish();
            }

            @Override
            public void onServiceDiscoveryComplete(){
                Log.d("DEV-LOG","Services discovered");
                //TODO: Depending on the operation method, after discovery, a write or a read will be called
                if(false){
                    thingConfigWrite("");
                }
                else{
                    gatewayConfigRead();
                }
            }

            @Override
            public void onServiceDiscoveryFail(){
                Log.d("DEV-LOG","Service discovery failed");
            }

            @Override
            public void onCharacteristicWriteComplete(){
                Log.d("DEV-LOG","Characteristic writen");
                if(writeDone){
                    bluetoothWrapper.closeConn();
                }
                else {
                    write_count++;
                    thingConfigWrite("");
                }
            }

            @Override
            public void onCharacteristicWriteFail(){
                Log.d("DEV-LOG","Characteristic write failed");
            }

            @Override
            public void onReadRssiComplete(int rssi){
                Log.d("DEV-LOG","Rssi read: " + rssi);
            }

            @Override
            public void onReadRssiFail(){
                Log.d("DEV-LOG","Rssi read failed");
            }

            @Override
            public void onCharacteristicReadComplete(byte[] value){
                String valueRead;

                if(value[0] < 97){valueRead = bytesToHex(value);}

                else {valueRead = new String(value);}

                Log.d("DEV-LOG","Characteristic read: " + valueRead);

                // Add read characteristic to gateway object
                gatewayConfigPersist(new String(value));

                if(readDone){
                    gatewayDBWrapper();
                    bluetoothWrapper.closeConn();
                }

                else {
                    read_count++;
                    gatewayConfigRead();
                }
            }

            @Override
            public void onCharacteristicReadFail(){
                Log.d("DEV-LOG","Characteristic read failed");
            }

        });
    }

    private void writeWrapper(UUID service, UUID characteristic, String valtoWrite){
        this.bluetoothWrapper.write(service,characteristic,valtoWrite);
    }


    private void writeWrapper(UUID service, UUID characteristic, byte[] valtoWrite){
        this.bluetoothWrapper.write(service,characteristic,valtoWrite);
    }

    private void readWrapper(UUID service, UUID characteristic){
        this.bluetoothWrapper.readCharacteristic(service,characteristic);
    }

    private void thingConfigWrite(String valToWrite){
        byte[] value = new byte[1];
        value[0] = (byte) (0x12);

        switch (write_count){
            case 0:
                Log.d("DEV-LOG", "Write Wrapper: Channel" );
                writeWrapper(otSettingsService,ChannelCharacteristic,value);
                break;
            case 1:
                Log.d("DEV-LOG", "WriteWrapper: NetName");
                writeWrapper(otSettingsService,NetNameCharacteristic,"lololo lololo");
                break;
            case 2:
                Log.d("DEV-LOG", "WriteWrapper: PanID");
                writeWrapper(otSettingsService,PanIDCharacteristic,value);
                break;
            case 3:
                Log.d("DEV-LOG", "WriteWrapper: XpanID");
                writeWrapper(otSettingsService,XpanidCharacteristic,"ooosh");
                break;
            case 4:
                Log.d("DEV-LOG", "WriteWrapper: IPV6");
                writeWrapper(IPV6Service,IPV6Characteristic,"the end");
                writeDone = true;
        }
    }

    private void gatewayConfigRead(){
        switch (read_count){
            case 0:
                Log.d("DEV-LOG", "ReadWrapper: Channel" );
                readWrapper(otSettingsService,ChannelCharacteristic);
                break;
            case 1:
                Log.d("DEV-LOG", "ReadWrapper: NetName");
                readWrapper(otSettingsService,NetNameCharacteristic);
                break;
            case 2:
                Log.d("DEV-LOG", "ReadWrapper: PanID");
                readWrapper(otSettingsService,PanIDCharacteristic);
                break;
            case 3:
                Log.d("DEV-LOG", "ReadWrapper: XpanID");
                readWrapper(otSettingsService,XpanidCharacteristic);
                break;
            case 4:
                Log.d("DEV-LOG", "ReadWrapper: IPV6");
                readWrapper(IPV6Service,IPV6Characteristic);
                readDone = true;
        }
    }

    private void gatewayConfigPersist(String value){
        switch (read_count){
            case 0:
                gateway.channel = value;
                break;
            case 1:
                gateway.netName = value;
                break;
            case 2:
                gateway.panID = value;
                break;
            case 3:
                gateway.xpanID = value;
                break;
            case 4:
                gateway.ipv6 = value;
        }
    }

    private static String bytesToHex(byte[] hashInBytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void gatewayDBWrapper(){
        mydb = new DBHelper(this);
        Log.d("DEV-LOG","Writing to database");
        mydb.insertDevice(gateway.ID,gateway.name,gateway.channel,gateway.netName,gateway.panID,gateway.xpanID,gateway.masterkey,gateway.ipv6);
        Log.d("DEV-LOG","Writing to database over");
    }

}

class Gateway {
    public Integer ID = 12123;
    public String name = "";
    public String channel = "";
    public String netName = "";
    public String panID = "";
    public String xpanID = "";
    public String masterkey = "asdas";
    public String ipv6 = "";
}
