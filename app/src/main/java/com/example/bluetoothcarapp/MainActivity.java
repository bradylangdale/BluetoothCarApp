package com.example.bluetoothcarapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity {
    private ListView lstvw;
    private ArrayAdapter aAdapter;
    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();

    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };
    private static final String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    ByteBuffer readBuffer;
    byte[] startBytes = "VFM".getBytes();
    byte[] endBytes = "VED".getBytes();
    byte[] imageBytes;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button forward = (Button) findViewById(R.id.btnForward);
        Button reverse = (Button) findViewById(R.id.btnReverse);
        Button left = (Button) findViewById(R.id.btnLeft);
        Button right = (Button) findViewById(R.id.btnRight);

        forward.setVisibility(View.INVISIBLE);
        reverse.setVisibility(View.INVISIBLE);
        left.setVisibility(View.INVISIBLE);
        right.setVisibility(View.INVISIBLE);

        forward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        mmOutputStream.write("f".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        mmOutputStream.write("s".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }
                return false;
            }
        });

        reverse.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        mmOutputStream.write("b".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        mmOutputStream.write("s".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }
                return false;
            }
        });

        left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        mmOutputStream.write("l".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        mmOutputStream.write("s".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }
                return false;
            }
        });

        right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        mmOutputStream.write("r".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        mmOutputStream.write("s".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }
                return false;
            }
        });

        Button btn = (Button) findViewById(R.id.btnGet);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bAdapter == null) {
                    Toast.makeText(getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
                } else {
                    checkPermissions();
                    @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();
                    ArrayList list = new ArrayList();
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            @SuppressLint("MissingPermission") String devicename = device.getName();
                            list.add(devicename);
                        }
                        lstvw = (ListView) findViewById(R.id.deviceList);
                        aAdapter = new ArrayAdapter(getApplicationContext(), R.layout.list_textview, list);
                        lstvw.setAdapter(aAdapter);
                        lstvw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                String selectedItem = (String) parent.getItemAtPosition(position);
                                Log.d("DEBUG", "Selected Device: " + selectedItem);

                                try {
                                    lstvw.setVisibility(View.INVISIBLE);
                                    btn.setVisibility(View.INVISIBLE);
                                    forward.setVisibility(View.VISIBLE);
                                    reverse.setVisibility(View.VISIBLE);
                                    left.setVisibility(View.VISIBLE);
                                    right.setVisibility(View.VISIBLE);
                                    findBT(selectedItem);
                                } catch (IOException e) {
                                    Log.d("DEBUG", "findBT failed this shouldn't have happened.");
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkPermissions(){
        int permission1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN);
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    1
            );
        } else if (permission2 != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_LOCATION,
                    1
            );
        }
    }

    @SuppressLint("MissingPermission")
    void findBT(String selectedDevice) throws IOException {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Log.d("DEBUG", "Odd Bluetooth isn't available?");
        }

        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals(selectedDevice))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        Log.d("DEBUG", "Found chosen device.");

        openBT();
    }

    @SuppressLint("MissingPermission")
    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        Log.d("DEBUG", "Opened Bluetooth Device.");
    }

    private void beginListenForData()
    {
        final Handler handler = new Handler();

        stopWorker = false;

        readBuffer = ByteBuffer.allocate(32768*64);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageBytes = new byte[8192];

        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        int bytesSinceStartTag = 0;
                        if (bytesAvailable > 0) {
                            Log.d("IMAGING", "Available: " + bytesAvailable);
                            Log.d("IMAGING", "Length: " + readBuffer.position());
                            readBuffer.put(mmInputStream.readNBytes(bytesAvailable));
                            bytesSinceStartTag += bytesAvailable;

                            int[] startTags = allIndicesOf(readBuffer, startBytes);
                            int[] endTags = allIndicesOf(readBuffer, endBytes);

                            int startTag = -1;
                            int endTag = -1;

                            if (startTags.length != 0) startTag = startTags[0];
                            if (endTags.length != 0) endTag = endTags[0];

                            Log.d("IMAGING", "Start: " + startTag + ", End: " + endTag);
                            if (startTag != -1 && endTag != -1) {
                                if (startTag < endTag) {
                                    readBuffer.get(imageBytes, startTag + 3, endTag - (startTag + 3));
                                    Log.d("IMAGE", Arrays.toString(imageBytes));
                                    //readBuffer.clear();
                                    //readBuffer.position(0);
                                    bytesSinceStartTag = 0;

                                    Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                                    if (bmp != null) {
                                        handler.post(new Runnable() {
                                            public void run() {
                                                imageView.setImageBitmap(Bitmap.createScaledBitmap(bmp, imageView.getWidth(), imageView.getHeight(), false));
                                            }
                                        });
                                    }
                                } else
                                {
                                    //bytesSinceStartTag += bytesAvailable;
                                    //readBuffer.position(startTag);
                                    ///readBuffer.compact();
                                    //readBuffer.position(bytesSinceStartTag);
                                }
                            } else if (startTag != -1) {
                                //bytesSinceStartTag += bytesAvailable;
                                //readBuffer.position(startTag);
                                //readBuffer.compact();
                                //readBuffer.position(bytesSinceStartTag);
                            } else {
                                bytesSinceStartTag = 0;
                                //readBuffer.flip();
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    private void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Log.d("DEBUG", "Bluetooth Device Closed.");
    }

    public static int[] allIndicesOf(ByteBuffer buf, byte[] b) {
        if (b.length == 0) {
            return new int[0];
        }
        return IntStream.rangeClosed(0, buf.limit() - b.length)
                .filter(i -> IntStream.range(0, b.length).allMatch(j -> buf.get(i + j) == b[j]))
                .toArray();
    }
}
