package com.forkize.sdk.messaging;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.forkize.sdk.R;

import org.json.JSONException;
import org.json.JSONObject;

public class ForkizeMessageActivity extends Activity {
    JSONObject message;

    RelativeLayout layout1;

    TextView header, body;
    Button button1, button2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("Forkize SDK", "Message Activity !!!");
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        try {
            message = new JSONObject(intent.getStringExtra(ForkizeMessageKeys.MESSAGE));
            String raw = intent.getStringExtra("raw");

            // TODO write all cases of notification
            switch (message.getString("type")){
                case "aaaa":
                    break;
                default:

                    setContentView(R.layout.forkize_layout_modal);
                    WebView webView = (WebView)findViewById(R.id.FZModularNotification);
                    WebSettings settings = webView.getSettings();
                    settings.setJavaScriptEnabled(true);
                    settings.setDomStorageEnabled(true);

                    webView.setWebChromeClient(new WebChromeClient());
                    webView.addJavascriptInterface(new JSClient(), "fz");
                    webView.loadData(raw, "text/html", "UTF-8");

                    Log.e("Forkize SDK", "RAW::Drawed\n" + raw);
            }

//            switch (message.getInt(ForkizeMessageKeys.TYPE)) {
//                case 1:
//                    setContentView(R.layout.forkize_layout_type_1);
//                    layout1 = (RelativeLayout) findViewById(R.id.forkize_layout_number_1);
//                    header = (TextView) findViewById(R.id.forkize_text_view_header_1);
//                    body = (TextView) findViewById(R.id.forkize_text_view_body_1);
//                    button1 = (Button) findViewById(R.id.forkize_button_1_1);
//                    button2 = (Button) findViewById(R.id.forkize_button_2_1);
//
//                    resolveMessageType1();
//                    break;
//                case 2:
//                    // FZ::TODO add
//                    break;
//                case 3:
//                    // FZ::TODO add
//                    break;
//                default:
//                    Log.e("Forkize SDK", "Wrong value for message type");
//            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class JSClient{

        @JavascriptInterface
        public void buttonOk(){
            ForkizeMessageActivity.this.finish();
        }
    }

    private void resolveMessageType1() throws Exception {
        JSONObject template = message.getJSONObject(ForkizeMessageKeys.TEMPLATE);
        JSONObject content = template.getJSONObject(ForkizeMessageKeys.CONTENT);
        JSONObject body = content.getJSONObject(ForkizeMessageKeys.BODY);

        this.body.setGravity(Gravity.CENTER);
        this.body.setText(body.getString(ForkizeMessageKeys.TEXT));
        this.body.setTextSize(body.getInt(ForkizeMessageKeys.FONTSIZE));
        int[] col = parseColor(body.getString(ForkizeMessageKeys.COLOR));
        this.body.setTextColor(Color.argb(col[3], col[0], col[1], col[2]));

        JSONObject header = content.getJSONObject(ForkizeMessageKeys.HEADER);

        this.header.setGravity(Gravity.CENTER);
        this.header.setText(header.getString(ForkizeMessageKeys.TEXT));
        this.header.setTextSize(header.getInt(ForkizeMessageKeys.FONTSIZE));
        col = parseColor(header.getString(ForkizeMessageKeys.COLOR));
        this.header.setTextColor(Color.argb(col[3], col[0], col[1], col[2]));

        JSONObject button1 = content.getJSONObject(ForkizeMessageKeys.BUTTON1);
        String button1Text = button1.getString(ForkizeMessageKeys.TEXT);

        JSONObject button2 = content.getJSONObject(ForkizeMessageKeys.BUTTON2);
        String button2Text = button2.getString(ForkizeMessageKeys.TEXT);

        // FZ::TODO I tried to make buttons length the same
//        int l1 = button1Text.length();
//        int l2 = button2Text.length();
//        if (l1 > l2) {
//            int z = (l1 - l2) / 2;
//            for (int j = 0; j < z; ++j)
//                button2Text = " " + button2Text + " ";
//            if ((l1 & 1) != (l2 & 1)) {
//                button2Text += " ";
//            }
//        } else {
//            int z = (l2 - l1) / 2;
//            for (int j = 0; j < z; ++j)
//                button1Text = " " + button1Text + " ";
//            if ((l1 & 1) != (l2 & 1)) {
//                button1Text += " ";
//            }
//        }

        this.button1.setText(button1Text);
        this.button1.setTextSize(button1.getInt(ForkizeMessageKeys.FONTSIZE));
        col = parseColor(button1.getString(ForkizeMessageKeys.COLOR));
        this.button1.setTextColor(Color.argb(col[3], col[0], col[1], col[2]));
        col = parseColor(button1.getString(ForkizeMessageKeys.BGCOLOR));
        this.button1.setBackgroundColor(Color.argb(col[3], col[0], col[1], col[2]));

        this.button2.setText(button2Text);
        this.button2.setTextSize(button2.getInt(ForkizeMessageKeys.FONTSIZE));
        col = parseColor(button2.getString(ForkizeMessageKeys.COLOR));
        this.button2.setTextColor(Color.argb(col[3], col[0], col[1], col[2]));
        col = parseColor(button2.getString(ForkizeMessageKeys.BGCOLOR));
        this.button2.setBackgroundColor(Color.argb(col[3], col[0], col[1], col[2]));

        JSONObject noty = content.getJSONObject(ForkizeMessageKeys.NOTIFICATION);

        col = parseColor(noty.getString(ForkizeMessageKeys.BGCOLOR));
        this.layout1.setBackgroundColor(Color.argb(col[3], col[0], col[1], col[2]));
    }

    private int[] parseColor(String s) {
        int[] c = {0, 0, 0, 0};
        String t = s.substring(5, s.length() - 1);
        int i = 0;
        int j = 0;
        for (; j < t.length() && i < 3; ++j) {
            if ('0' <= t.charAt(j) && t.charAt(j) <= '9') {
                c[i] = 10 * c[i] + ((int) t.charAt(j) - (int) '0');
            } else {
                ++i;
            }
        }
        c[3] = 255;
        return c;
    }
}