package edu.umd.lib.services;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SysAidUsers {

  private int offsetValue = 0;
  private static SysAidUsers _instance;

  private Logger log = Logger.getLogger(SysAidUsers.class);
  private JSONArray all_Users = new JSONArray();

  private String sysaidUrl;
  private String sysaidSession;

  public static synchronized SysAidUsers getInstance(String url, String sessionid) {
    if (_instance == null) {
      _instance = new SysAidUsers(url, sessionid);
    }
    return _instance;
  }

  /***
   * Load all Users of type user,admin and Manager on initialization
   */
  private SysAidUsers(String url, String sessionid) {
    this.sysaidUrl = url;
    this.sysaidSession = sessionid;
    this.LoadAllUsers("user");
    this.offsetValue = 0;
    this.LoadAllUsers("admin");
    this.offsetValue = 0;
    this.LoadAllUsers("manager");
    log.info("Total " + all_Users.length() + " Users loaded into Cache. ");
  }

  /***
   * Get All users List
   *
   * @return
   */
  public JSONArray getAll_Users() {
    return all_Users;
  }

  public void setAll_Users(JSONArray all_Users) {
    this.all_Users = all_Users;
  }

  /***
   * reload All users from SysAid to cache
   */
  public void reloadAllUsers(String url, String sessionid) {
    this.sysaidUrl = url;
    this.sysaidSession = sessionid;
    all_Users = new JSONArray();
    this.offsetValue = 0;
    this.LoadAllUsers("user");
    this.offsetValue = 0;
    this.LoadAllUsers("admin");
    this.offsetValue = 0;
    this.LoadAllUsers("manager");
    log.info("Total " + all_Users.length() + " Users reloaded into Cache. ");
  }

  /***
   * Connect to SysAid to get all users * @return ServiceRequest_ID
   */
  private JSONArray LoadAllUsers(String userType) {

    try {
      SysAidConnector sysaid = new SysAidConnector(this.sysaidUrl, this.sysaidSession);
      HttpResponse response = sysaid
          .getRequest(sysaid.getSysaid_URL() + "users?view=mobile&offset=" + offsetValue + "&type=" + userType);
      HttpEntity entity = response.getEntity();
      String responseString = EntityUtils.toString(entity, "UTF-8");
      JSONArray list_response = new JSONArray(responseString);

      for (int i = 0; i < list_response.length(); i++) {

        JSONObject user = new JSONObject();
        JSONObject user_response = list_response.getJSONObject(i);
        user.put("id", user_response.get("id"));
        user.put("name", user_response.get("name"));

        JSONArray info = (JSONArray) user_response.get("info");
        for (int j = 0; j < info.length(); j++) {
          JSONObject each_info = info.getJSONObject(j);
          user.put(each_info.getString("key"), each_info.getString("value"));
        }
        all_Users.put(user);
      }
      if (list_response.length() == 500) {
        offsetValue = offsetValue + list_response.length();
        LoadAllUsers(userType);
      }
      return list_response;
    } catch (JSONException e) {
      log.error("JSONException occured while attempting to "
          + "get list of all users in Cache.", e);
    } catch (ParseException e) {
      log.error("ParseException occured while attempting to "
          + "get list of all users in Cache.", e);
    } catch (IOException e) {
      log.error("IOException occured while attempting to "
          + "get list of all users in Cache.", e);
    }
    return new JSONArray();

  }

  /***
   * This method is used to get a user by providing any property of the user and
   * its value
   *
   * @param key
   * @param Value
   */
  public JSONObject getUserbyKey(String key, String Value) {

    try {
      for (int i = 0; i < all_Users.length(); i++) {
        JSONObject user = all_Users.getJSONObject(i);
        if (user.has(key)) {
          if (user.getString(key).equalsIgnoreCase(Value)) {
            return user;
          }
        }
      }
    } catch (JSONException e) {
      log.error("JSONException occured while attempting to "
          + "execute GET request.", e);
    }
    return null;

  }

}
