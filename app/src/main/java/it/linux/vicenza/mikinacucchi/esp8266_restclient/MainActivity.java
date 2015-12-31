package it.linux.vicenza.mikinacucchi.esp8266_restclient;

import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ESP8266_app";

    private static String ESP8266_IP    = "192.168.4.1";//"192.168.1.100";
    private static String ESP8266_PORT    = "80";//"8080";
    private static int ESP8266_POLLING  =   1200;//ms

    private View searchingBox,interfacesBox;
    private RadioButton pin;
    private Switch led;

    private Handler handler_found, handler_lost;

    private HttpGet httpget_LED, httpget_PIN;
    private HttpPost httppost_LED;
    private EditText txtIP, txtPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchingBox   =   findViewById(R.id.searchingLayout);
        interfacesBox  =   findViewById(R.id.interfacesLayout);
        led  = (Switch) findViewById(R.id.switch1);
        pin  = (RadioButton) findViewById(R.id.radioButton);
        txtIP   = (EditText) findViewById(R.id.txtIP);
        txtPort = (EditText) findViewById(R.id.txtPort);

        led.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLED(led.isChecked());
            }
        });

        txtIP.setText(ESP8266_IP);
        txtPort.setText(ESP8266_PORT);

        txtIP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(!validIP(s.toString())) return;

                ESP8266_IP = s.toString();
                buildURIs();
            }
        });
        txtPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (Integer.parseInt(s.toString()) < 0) return;
                }catch (NumberFormatException e){return;}

                ESP8266_PORT = s.toString();
                buildURIs();
            }
        });

        buildURIs();
        lost();
    }

    private void lost(){
        Log.d(TAG, "lost");

        searchingBox.setVisibility(View.VISIBLE);
        interfacesBox.setVisibility(View.INVISIBLE);

        if(handler_found != null)
            handler_found.removeCallbacks(runnable_found);

        if(handler_lost == null) handler_lost = new Handler();
        handler_lost.postDelayed(runnable_lost, 100);
    }
    private Runnable runnable_lost = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Trying connection");
            new RequestHTTP(
                    httpget_LED,
                    new RequestHTTP.Response() {
                        @Override
                        public void onSuccess(String result) {
                            found();
                        }
                        @Override
                        public void onError(int errorCode) {
                            handler_lost.postDelayed(runnable_lost, ESP8266_POLLING);
                        }
                    }
            );
        }
    };

    private void found(){
        Log.d(TAG, "found");
        searchingBox.setVisibility(View.INVISIBLE);
        interfacesBox.setVisibility(View.VISIBLE);

        if(handler_lost != null)
            handler_lost.removeCallbacks(runnable_lost);

        if(handler_found == null) handler_found = new Handler();
        handler_found.postDelayed(runnable_found, 100);
    }
    private Runnable runnable_found = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Updating UI components");

            getPin();
            getLED();

            handler_found.postDelayed(this, ESP8266_POLLING);
        }
    };





    private void setLED(boolean value){
        Log.d(TAG, "setLED");
        //HTTP POST /LED    value
        ArrayList<NameValuePair> postParameters;
        postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("value", (value ? "1":"0")));

        try {
            httppost_LED.setEntity(new UrlEncodedFormEntity(postParameters));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException");
        }

        new RequestHTTP(
                httppost_LED,
                new RequestHTTP.Response() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "onSuccess");
                    }

                    @Override
                    public void onError(int errorCode) {
                        Log.e(TAG, "onError " + errorCode);
                        String strError = null;
                        switch (errorCode) {
                            //All RequestHTTP Errors
                            case RequestHTTP.ERRORCODE_HTTP_RESPONSE_FAIL:
                                strError = "ERRORCODE_HTTP_RESPONSE_FAIL";
                                break;
                            case RequestHTTP.ERRORCODE_NETWORK:
                                strError = "ERRORCODE_NETWORK";
                                break;
                            case RequestHTTP.ERRORCODE_URL_MALFORMED:
                                strError = "ERRORCODE_URL_MALFORMED";
                                break;
                            default:
                                strError = "UNKNOW ERROR";
                                break;
                        }

                        Toast.makeText(getApplicationContext(), strError, Toast.LENGTH_LONG).show();
                        lost();
                    }
                }
        );
    }
    private void getLED(){
        //HTTP GET /LED
        new RequestHTTP(
                httpget_LED,
                new RequestHTTP.Response() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "onSuccess");

                        Log.d(TAG, result);
                        if(result.equals("true"))
                            led.setChecked(true);
                        else if(result.equals("false"))
                            led.setChecked(false);
                    }

                    @Override
                    public void onError(int errorCode) {
                        Log.e(TAG, "onError " + errorCode);
                        String strError = null;
                        switch (errorCode) {
                            //All RequestHTTP Errors
                            case RequestHTTP.ERRORCODE_HTTP_RESPONSE_FAIL:
                                strError = "ERRORCODE_HTTP_RESPONSE_FAIL";
                                break;
                            case RequestHTTP.ERRORCODE_NETWORK:
                                strError = "ERRORCODE_NETWORK";
                                break;
                            case RequestHTTP.ERRORCODE_URL_MALFORMED:
                                strError = "ERRORCODE_URL_MALFORMED";
                                break;
                            default:
                                strError = "UNKNOW ERROR";
                                break;
                        }

                        Toast.makeText(getApplicationContext(), strError, Toast.LENGTH_LONG).show();
                        lost();
                    }
                }
        );
    }
    private void getPin(){
        //HTTP GET /PIN
        new RequestHTTP(
                httpget_PIN,
                new RequestHTTP.Response() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "onSuccess");

                        Log.d(TAG, result);
                        if(result.equals("true"))
                            pin.setChecked(true);
                        else if(result.equals("false"))
                            pin.setChecked(false);
                    }

                    @Override
                    public void onError(int errorCode) {
                        Log.e(TAG, "onError " + errorCode);
                        String strError = null;
                        switch (errorCode) {
                            //All RequestHTTP Errors
                            case RequestHTTP.ERRORCODE_HTTP_RESPONSE_FAIL:
                                strError = "ERRORCODE_HTTP_RESPONSE_FAIL";
                                break;
                            case RequestHTTP.ERRORCODE_NETWORK:
                                strError = "ERRORCODE_NETWORK";
                                break;
                            case RequestHTTP.ERRORCODE_URL_MALFORMED:
                                strError = "ERRORCODE_URL_MALFORMED";
                                break;
                            default:
                                strError = "UNKNOW ERROR";
                                break;
                        }

                        Toast.makeText(getApplicationContext(), strError, Toast.LENGTH_LONG).show();
                        lost();
                    }
                }
        );
    }


    private void buildURIs() {
        Uri LED_GET_Uri = new Uri.Builder()
                .scheme("http")
                .encodedAuthority(ESP8266_IP + ":" + ESP8266_PORT)
                .appendPath("led")
                .build();
        Uri LED_POST_Uri = new Uri.Builder()
                .scheme("http")
                .encodedAuthority(ESP8266_IP + ":" + ESP8266_PORT)
                .appendPath("led")
                .build();
        Uri PIN_GET_Uri = new Uri.Builder()
                .scheme("http")
                .encodedAuthority(ESP8266_IP + ":" + ESP8266_PORT)
                .appendPath("button")
                .build();

        try {
            httppost_LED = new HttpPost(LED_POST_Uri.toString());
            Log.d(TAG, "buildHttpPost: " + LED_POST_Uri.toString());

            httpget_LED = new HttpGet(LED_GET_Uri.toString());
            Log.d(TAG, "buildHttpGet: " + LED_GET_Uri.toString());

            httpget_PIN = new HttpGet(PIN_GET_Uri.toString());
            Log.d(TAG, "buildHttpGet: " + PIN_GET_Uri.toString());

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "HttpPost: IllegalArgumentException");
        }
    }
    public static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
