package com.forkize.sdk.rest;

import android.database.Cursor;
import android.util.Base64;
import android.util.Log;

import com.forkize.sdk.Forkize;
import com.forkize.sdk.ForkizeConfig;
import com.forkize.sdk.ForkizeHelper;
import com.forkize.sdk.ForkizeInstance;
import com.forkize.sdk.messaging.ForkizeTemplateManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

public class Request {

    // FZ::TODO:: Check if commented rules were necessary
//  private final String BASE_URL = "https://androidsdk-davkhech.c9.io";    //"http://artakvg.cloudapp.net:7001/" + ForkizeConfig.getInstance().getSdkVersion();

    //    private final String BASE_URL = "http://fzgate.cloudapp.net:8080/" + ForkizeConfig.getInstance().getSdkVersion();
    private final String BASE_URL = "http://forkize-davkhech.c9users.io:8080/" + ForkizeConfig.getInstance().getSdkVersion();

    private final String URL_auth = "/people/identify";
    private final String URL_alias = "/people/alias";
    private final String URL_track = "/event/batch";
    private final String URL_update = "/profile/change";
    private final String URL_template = "/profile/templates";
    private final String URL_campaign = "/profile/getMatchingCampaigns";

    private String accessToken;

    protected String getToken() {
        return this.accessToken;
    }

    protected void dropAccessToken() {
        this.accessToken = null;
    }

    protected void updateCampaign() {
        JSONObject api_data = new JSONObject();
        try {
            api_data.put("cmp_hashes", ForkizeInstance.getInstance().getUserProfile().getCampaignHashArray());
            HttpURLConnection urlConnection = getConnection(URL_campaign, api_data);

            if (urlConnection == null) {
                return;
            }

            InputStream errorStream = urlConnection.getErrorStream();
            InputStream in;

            if (errorStream == null) {
                in = new BufferedInputStream(urlConnection.getInputStream());

                String responseBody = getResponseBody(in);
                in.close();

                JSONObject object = new JSONObject(responseBody);

                Iterator<String> keys = object.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject campaign = object.getJSONObject(key);

                    String content = campaign.getString("content");
                    String hash = campaign.getString("hash");
                    byte[] decoded = Base64.decode(content.getBytes("UTF-8"), Base64.DEFAULT);

                    String decodedString = new String(decoded);
                    Log.e("Forkize SDK", "CAMPAIGNS!!!");
                    Log.e("Forkize SDK", "ENCODED!!!\n" + content);
                    Log.e("Forkize SDK", "DECODED!!!\n" + decodedString);

                    if (ForkizeHelper.md5(decodedString).equals(hash)) {
                        //TODO
//                        Log.e("Forkize SDK", new String(decoded) + " hash: " + ForkizeHelper.md5(new String(decoded)) + " givenHash: " + hash);
                        ForkizeInstance.getInstance().getUserProfile().addMessage(decodedString);
                    }
                }

                // TODO :: check status, then get missing campaigns and add to notification pool
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO::TEST
    protected void getTemplates() {
        JSONObject api_data = new JSONObject();
        try {
            api_data.put("template_hash", ForkizeInstance.getInstance().getUserProfile().getTemplateHashArray());
            HttpURLConnection urlConnection = getConnection(URL_template, api_data);

            if (urlConnection == null) {
                return;
            }

            InputStream errorStream = urlConnection.getErrorStream();
            InputStream in;

            if (errorStream == null) {
                in = new BufferedInputStream(urlConnection.getInputStream());

                String responseBody = getResponseBody(in);
                in.close();

                JSONObject object = new JSONObject(responseBody);
                Log.e("Forkize SDK", object.toString());

                Iterator<String> keys = object.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject template = object.getJSONObject(key);

                    String content = template.getString("content");
                    String hash = template.getString("hash");
                    byte[] decoded = Base64.decode(content.getBytes("UTF-8"), Base64.DEFAULT);

                    String decodedString = new String(decoded);
                    Log.e("Forkize SDK", "TEMPLATES!!!");
                    Log.e("Forkize SDK", "ENCODED!!!\n" + content);
                    Log.e("Forkize SDK", "DECODED!!!\n" + decodedString);

                    if (ForkizeHelper.md5(decodedString).equals(hash)) {
                        //TODO
//                        Log.e("Forkize SDK", new String(decoded) + " hash: " + ForkizeHelper.md5(new String(decoded)) + " givenHash: " + hash);
                        // TODO::CHECK if always content arrives with "" and if so, write the substring of it otherwise keep above line the same
                        ForkizeTemplateManager.getInstance().writeFile(key, decodedString.substring(1, decodedString.length() - 1));
                        ForkizeTemplateManager.getInstance().saveHash(key, hash);
                    }
                }
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    protected void getAccessToken() {
        try {
            JSONObject api_data = new JSONObject();
            HttpURLConnection urlConnection = getConnection(URL_auth, api_data);

            if (urlConnection == null) {
                return;
            }

            InputStream errorStream = urlConnection.getErrorStream();
            InputStream in;

            if (errorStream == null) {
                in = new BufferedInputStream(urlConnection.getInputStream());

                String responseBody = getResponseBody(in);
                in.close();

                JSONObject object = new JSONObject(responseBody);
                this.accessToken = object.getString("access_token");

//                TODO::REMOVE
//                JSONObject message;
//
//                try {
//                    message = object.getJSONObject("message");
//                    ForkizeMessage.getInstance().showMessage(message);
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }

                urlConnection.disconnect();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    protected boolean postAlias(Cursor cursor) {
        if (cursor == null) {
            return false;
        }
        String aliasid = cursor.getString(1);

        if (ForkizeHelper.isNullOrEmpty(aliasid))
            return false;

        JSONObject api_data = new JSONObject();
        try {
            api_data.put("alias_id", aliasid);
            HttpURLConnection urlConnection = getConnection(URL_alias, api_data);

            if (urlConnection == null) {
                return false;
            }

            InputStream errorStream = urlConnection.getErrorStream();
            InputStream in;

            // FZ::TODO we do nothing in case of error
            if (errorStream == null) {
                in = new BufferedInputStream(urlConnection.getInputStream());

                String responseBody = getResponseBody(in);
                in.close();

                JSONObject object = new JSONObject(responseBody);
                this.accessToken = object.getString("access_token");

                if (object.getInt("status") == 1) {
                    urlConnection.disconnect();
                    return true;
                }
            }

            urlConnection.disconnect();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // TODO::REVIEW !!!
    protected boolean updateUserProfile() {
        try {
            JSONObject api_data = new JSONObject(ForkizeInstance.getInstance().getUserProfile().getChangeLog());
            api_data.put("upv", ForkizeInstance.getInstance().getUserProfile().getProfileVersion());
            HttpURLConnection urlConnection = getConnection(URL_update, api_data);

            if (urlConnection == null) {
                return false;
            }

            InputStream errorStream = urlConnection.getErrorStream();
            InputStream in;

            if (errorStream == null) {
                in = new BufferedInputStream(urlConnection.getInputStream());

                String responseBody = getResponseBody(in);
                in.close();

                JSONObject object = new JSONObject(responseBody);

                if (object.getInt("status") == 1) {
                    urlConnection.disconnect();

                    ForkizeInstance.getInstance().getUserProfile().setProfileVersion(object.optLong("upv", -1));
                    Log.w("Forkize SDK", String.valueOf(object.optLong("upv", -1)));
                    String up = object.optString("up", null);
                    if (up != null) {
                        ForkizeInstance.getInstance().getUserProfile().setProfile(up, false);
                    }
                    return true;
                }

                urlConnection.disconnect();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected int postEvents(JSONArray api_data) {
        try {
            // FZ::TODO::REMOVE
            if (this.accessToken != null) {
                Log.e("Forkize SDK", this.accessToken);
            } else {
                Log.e("Forkize SDK", "There is no access token");
            }
            HttpURLConnection urlConnection = getConnection(URL_track, api_data);
            if (urlConnection == null) {
                return 0;
            }

            InputStream errorStream = urlConnection.getErrorStream();
            InputStream in;

            // FZ::TODO we do nothing in case of error
            if (errorStream == null) {
                in = new BufferedInputStream(urlConnection.getInputStream());

                String responseBody = getResponseBody(in);
                in.close();
                JSONObject object = new JSONObject(responseBody);

                int status = object.optInt("status", 0);
                String error = object.optString("error");

                // FZ::TODO what is response body, how find out if batch has been received
                if (status == 1) {
                    Log.e("Forkize SDK", object.toString());
                    urlConnection.disconnect();
                    return 1;
                } else if (error.equalsIgnoreCase("TOKEN_EXPIRED")) {
                    Log.e("Forkize SDK", "Access token expired response");
                    urlConnection.disconnect();
                    return 2;
                }
            }

            urlConnection.disconnect();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private HttpURLConnection getConnection(String endPoint, Object api_data) {

        String str = generateRequestBody(endPoint, api_data);
        if (ForkizeHelper.isNullOrEmpty(str)) {
            return null;
        }

        HttpURLConnection urlConnection = null;
        try {
            byte[] body = str.getBytes("UTF-8");

            URL url = new URL(BASE_URL + endPoint);
            urlConnection = (HttpURLConnection) url.openConnection();

            Log.i("Forkize SDK", "Connection Opened");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Connection", "close");

            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setFixedLengthStreamingMode(body.length);

            urlConnection.connect();

            BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            out.write(body, 0, body.length);
            out.close();

            Log.i("Forkize SDK", new String(body, "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urlConnection;
    }

    private String generateRequestBody(String endPoint, Object api_data) {
        if (api_data == null) {
            return null;
        }

        String hashInfo = ForkizeConfig.getInstance().getAppId() + "="
                + ForkizeInstance.getInstance().getUserProfile().getUserId() + "="
                + "android" + "="
                + ForkizeConfig.getInstance().getSdkVersion() + "="
                + ForkizeConfig.getInstance().getAppKey();

        if (!endPoint.equals(URL_auth)) {
            hashInfo += "=" + api_data.toString();
        }

        String hashString = ForkizeHelper.md5(hashInfo);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("app_id", ForkizeConfig.getInstance().getAppId());
            jsonObject.put("user_id", ForkizeInstance.getInstance().getUserProfile().getUserId());
            jsonObject.put("sdk", "android");
            jsonObject.put("version", ForkizeConfig.getInstance().getSdkVersion());
            jsonObject.put("hash", hashString);

            if (!endPoint.equals(URL_auth)) {
                jsonObject.put("api_data", api_data);
            }

            if (endPoint.equals(URL_alias) || endPoint.equals(URL_update) || endPoint.equals(URL_track) || endPoint.equals(URL_campaign)) {
                jsonObject.put("access_token", this.accessToken);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject.toString();
    }

    private String getResponseBody(InputStream in) {
        StringBuilder str = new StringBuilder();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                str.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.w("Forkize SDK", "Response: " + str.toString());

        return str.toString();
    }
}