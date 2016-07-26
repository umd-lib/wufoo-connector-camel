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

import edu.umd.lib.services.SysAidConnector.SysAidLoginException;

public class SysAidUsers {

  private int offsetValue = 0;
  private static SysAidUsers _instance;

  private Logger log = Logger.getLogger(SysAidUsers.class);
  private JSONArray all_Users = new JSONArray();

  public static synchronized SysAidUsers getInstance() {
    if (_instance == null) {
      _instance = new SysAidUsers();
    }
    return _instance;
  }

  private SysAidUsers() {
    this.LoadAllUsers("user");
    this.offsetValue = 0;
    this.LoadAllUsers("admin");
    this.offsetValue = 0;
    this.LoadAllUsers("manager");
    log.info("Total " + all_Users.length() + " Users loaded into Cache. ");
  }

  public JSONArray getAll_Users() {
    return all_Users;
  }

  public void setAll_Users(JSONArray all_Users) {
    this.all_Users = all_Users;
  }

  public void reloadAllUsers() {
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
   * * @return ServiceRequest_ID
   */
  private JSONArray LoadAllUsers(String userType) {

    try {
      SysAidConnector sysaid = new SysAidConnector();
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
    } catch (SysAidLoginException e) {
      log.error("SysAidLoginException occured while attempting to "
          + "execute GET request.", e);
    } catch (JSONException e) {
      log.error("JSONException occured while attempting to "
          + "execute GET request.", e);
    } catch (ParseException e) {
      log.error("ParseException occured while attempting to "
          + "execute GET request.", e);
    } catch (IOException e) {
      log.error("IOException occured while attempting to "
          + "execute GET request.", e);
    }
    return new JSONArray();

  }

  /***
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
