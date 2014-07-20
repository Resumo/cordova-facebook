package com.ccsoft.plugin;

import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.Session;
import com.sromku.simple.fb.listeners.*;
import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebookConfiguration;
import com.sromku.simple.fb.entities.Feed;
import com.sromku.simple.fb.entities.Profile;
import com.sromku.simple.fb.entities.Profile.Properties;

import android.content.Intent;
import android.util.Log;

public class CordovaFacebook extends CordovaPlugin {

    private final String TAG = "CordovaFacebook";

    private SimpleFacebookConfiguration facebookConfiguration = null;

    @Override
    public boolean execute(
            String action, 
            JSONArray args,
            final CallbackContext callbackContext
            ) throws JSONException {
    
        Log.d(TAG, "action:" + action);
        cordova.setActivityResultCallback(this);

        if (action.equals("init")) {
            
//          JSONArray ps = args.getJSONArray(2);
//          ArrayList<Permissions> permsArr = new ArrayList<Permissions>();
//          for (int i = 0; i < ps.length(); i++) {
//              Permissions p = Permissions.findPermission(ps.getString(i));
//              if (p != null) {
//                  permsArr.add(p);
//              }
//          }
//          if (permsArr.isEmpty()) {
//              permsArr.add(Permissions.BASIC_INFO);
////                permsArr.add(Permissions.);
//          }
//          Permissions[] perms = permsArr.toArray(new Permissions[permsArr
//                  .size()]);
            
            // hardcoded for now
            Permission[] permissions = new Permission[] {
                    Permission.PUBLIC_PROFILE,
                    Permission.USER_FRIENDS,
                    Permission.EMAIL,
                    Permission.USER_BIRTHDAY
            };

            String appId = args.getString(0);
            String appNamespace = args.getString(1);
            
            initializeFacebook(appId, appNamespace, permissions, callbackContext);
            
            return true;
        }

        // check if init was called
        if (facebookConfiguration == null) {
            Log.e(TAG, "init was not called");
            callbackContext.error("init plugin first");
            return true;
        }
        
        final SimpleFacebook mSimpleFacebook = SimpleFacebook.getInstance(cordova.getActivity());

        if (action.equals("login")) {           
            handleLogin(mSimpleFacebook, callbackContext);
            return true;
        }

        if (action.equals("logout")) {
            handleLogout(mSimpleFacebook, callbackContext);
            return true;
        }
        
        if (action.equals("info")) {
            handleInfo(mSimpleFacebook, callbackContext);
            return true;
        }
                
        if (action.equals("feed") || action.equals("share")) {          
            handleFeedShare(mSimpleFacebook, callbackContext, args);
            return true;
        }

        if (action.equals("invite")) {
            handleInvite(mSimpleFacebook, callbackContext, args);
            return true;
        }

        if (action.equals("friends")) {
            handleFriends(mSimpleFacebook, callbackContext);
            return true;
        }

        return false;
    }

    private void handleLogin(final SimpleFacebook simpleFacebook, final CallbackContext callbackContext) {
        // login listener
        final OnLoginListener onLoginListener = new OnLoginListener() {

            @Override
            public void onLogin() {
                // change the state of the button or do whatever you want
                Log.i(TAG, "Logged in fb");
                
//              JSONObject resp = prepareAccessTokenInfo(SimpleFacebook.getOpenSession());
                Session session = simpleFacebook.getSession();
                JSONObject resp = prepareAccessTokenInfo(session);
                callbackContext.success(resp);
            }

            @Override
            public void onNotAcceptingPermissions(Permission.Type type) {
                Log.w(TAG, "User didn't accept read permissions");
                callbackContext.error("permission not accepted");
            }

            @Override
            public void onFail(String reason) {
                Log.w(TAG, reason);
                callbackContext.error(reason);
            }

            @Override
            public void onException(Throwable throwable) {
                Log.e(TAG, "Bad thing happened", throwable);
                callbackContext.error(throwable.getMessage());
            }

            @Override
            public void onThinking() {
                // show progress bar or something to the user while login is
                // happening
                Log.i(TAG, "In progress");
            }
        };

        Runnable runnable = new Runnable() {
            public void run() {
                simpleFacebook.login(onLoginListener);
            };
        };
        cordova.getActivity().runOnUiThread(runnable);

    }

    private void handleLogout(final SimpleFacebook simpleFacebook, final CallbackContext callbackContext) {
        // logout listener
        final OnLogoutListener onLogoutListener = new OnLogoutListener() {
            @Override
            public void onFail(String reason) {
                Log.w(TAG, reason);
                callbackContext.error(reason);
            }

            @Override
            public void onException(Throwable throwable) {
                Log.e(TAG, "Bad thing happened", throwable);
                callbackContext.error(throwable.getMessage());
            }

            @Override
            public void onThinking() {
                // show progress bar or something to the user while login is
                // happening
                Log.i(TAG, "In progress");
            }

            @Override
            public void onLogout() {
                Log.i(TAG, "You are logged out");
                callbackContext.success("User was logged out.");
            }
        };

        Runnable runnable = new Runnable() {
            public void run() {
                simpleFacebook.logout(onLogoutListener);
            };
        };
        cordova.getActivity().runOnUiThread(runnable);      
    }
        
    private void handleFriends(final SimpleFacebook simpleFacebook, final CallbackContext callbackContext) {
        
        OnFriendsListener onFriendsListener = new OnFriendsListener() {
            
            @Override
            public void onFail(String reason) {
                // insure that you are logged in before getting the profile
                Log.w(TAG, reason);
                callbackContext.error(reason);
            }

            @Override
            public void onException(Throwable throwable) {
                Log.e(TAG, "Bad thing happened", throwable);
                callbackContext.error(throwable.getMessage());
            }

            @Override
            public void onThinking() {
                // show progress bar or something to the user while fetching
                // profile
                Log.i(TAG, "Thinking...");
            }

            @Override
            public void onComplete(List<Profile> friends) {
                Log.i(TAG, "Number of friends = " + friends.size());
                
                JSONArray jsonFriends = new JSONArray();
                for (int i = 0; i<friends.size(); i++) {
                    Profile friend = friends.get(i);
                    JSONObject jsonFriend = friend.getGraphObject().getInnerJSONObject();
                    jsonFriends.put(jsonFriend);                    
                }
        
                JSONObject response = new JSONObject();
                try {
                    response.put("data", jsonFriends);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                                
                callbackContext.success(response);                          }
        };

        Profile.Properties friendsProperties = new Profile.Properties.Builder()
            .add(Properties.ID)
            .add(Properties.NAME)
            .add(Properties.PICTURE)
            .build();
        simpleFacebook.getFriends(friendsProperties, onFriendsListener);
                
//      OnActionListener<List<Profile>> onFriendsListener = new OnActionListener<List<Profile>>() {
//          
//          @Override
//          public void onFail(String reason) {
//              // insure that you are logged in before getting the profile
//              Log.w(TAG, reason);
//              callbackContext.error(reason);
//          }
//
//          @Override
//          public void onException(Throwable throwable) {
//              Log.e(TAG, "Bad thing happened", throwable);
//              callbackContext.error(throwable.getMessage());
//          }
//
//          @Override
//          public void onThinking() {
//              // show progress bar or something to the user while fetching
//              // profile
//              Log.i(TAG, "Thinking...");
//          }
//
//          @Override
//          public void onComplete(List<Profile> friends) {
//              Log.i(TAG, "Number of friends = " + friends.size());
//              
//              JSONArray jsonFriends = new JSONArray();
//              for (int i = 0; i<friends.size(); i++) {
//                  Profile friend = friends.get(i);
//                  JSONObject jsonFriend = friend.getGraphObject().getInnerJSONObject();
//                  jsonFriends.put(jsonFriend);                    
//              }
//      
//              JSONObject response = new JSONObject();
//              try {
//                  response.put("data", jsonFriends);
//              } catch (JSONException e) {
//                  e.printStackTrace();
//              }
//                              
//              callbackContext.success(response);                          
//          }
//      };
//      
//      simpleFacebook.get("me", "friends", null, onFriendsListener);
    }
    
    private void handleInfo(final SimpleFacebook simpleFacebook, final CallbackContext callbackContext) {
        if (simpleFacebook.isLogin() == true) {
            getUserInfo(callbackContext);
        } else {
            callbackContext.error("not logged in");
        }       
    }
    
    private void handleFeedShare(final SimpleFacebook simpleFacebook, final CallbackContext callbackContext, JSONArray args) throws JSONException {
        // create publish listener
        final OnPublishListener onPublishListener = new OnPublishListener() {
            @Override
            public void onFail(String reason) {
                // insure that you are logged in before publishing
                Log.w(TAG, reason);
                callbackContext.error(reason);
            }

            @Override
            public void onException(Throwable throwable) {
                Log.e(TAG, "Bad thing happened", throwable);
                callbackContext.error(throwable.getMessage());
            }

            @Override
            public void onThinking() {
                // show progress bar or something to the user while
                // publishing
                Log.i(TAG, "In progress");
            }

            @Override
            public void onComplete(String postId) {
                Log.i(TAG, "Published successfully. The new post id = "
                        + postId);
                JSONObject r = new JSONObject();
                try {
                    r.put("post_id", postId);
                } catch (JSONException e) {
                    Log.e(TAG, "Bad thing happened with profile json", e);
                    callbackContext.error("json exception");
                    return;
                }
                callbackContext.success(r);
            }
        };

        // build feed
        final Feed feed = new Feed.Builder().setName(args.getString(0))
                .setLink(args.getString(1)).setPicture(args.getString(2))
                .setCaption(args.getString(3))
                .setDescription(args.getString(4)).build();

        Runnable runnable = new Runnable() {
            public void run() {
                simpleFacebook.publish(feed, onPublishListener);
            };
        };
        cordova.getActivity().runOnUiThread(runnable);
    }
    
    private void handleInvite(final SimpleFacebook simpleFacebook, final CallbackContext callbackContext, JSONArray args) throws JSONException {
        final String message = args.getString(0);
        final OnInviteListener onInviteListener = new OnInviteListener() {

            @Override
            public void onFail(String reason) {
                // insure that you are logged in before inviting
                Log.w(TAG, reason);
                callbackContext.error(reason);
            }

            @Override
            public void onException(Throwable throwable) {
                // user may have canceled, we end up here in that case as
                // well!
                Log.e(TAG, "Bad thing happened", throwable);
                callbackContext.error(throwable.getMessage());
            }

            @Override
            public void onComplete(List<String> invitedFriends,
                    String requestId) {
                if (invitedFriends.isEmpty()) {
                    callbackContext.error("nobody invited");
                    return;
                }

                // Log.i(TAG, "Invitation was sent to " +
                // invitedFriends.size() + " users with request id " +
                // requestId);
                JSONArray to = new JSONArray();
                for (String item : invitedFriends) {
                    to.put(item);
                }

                JSONObject r = new JSONObject();
                try {
                    r.put("to", to);
                    r.put("request", requestId);
                } catch (JSONException e) {
                    Log.e(TAG, "Bad thing happened with invite json", e);
                    callbackContext.error("json exception");
                    return;
                }
                callbackContext.success(r);
            }

            @Override
            public void onCancel() {
                Log.i(TAG, "Canceled the dialog");
                callbackContext.error("cancelled");
            }
        };

        Runnable runnable = new Runnable() {
            public void run() {
                simpleFacebook.invite(message, onInviteListener, null);
            };
        };
        cordova.getActivity().runOnUiThread(runnable);

    }
    
    private void initializeFacebook(String appId, String appNamespace, Permission[] permissions, CallbackContext callbackContext) {
        facebookConfiguration = new SimpleFacebookConfiguration.Builder()
                                        .setAppId(appId)
                                        .setNamespace(appNamespace)
                                        .setPermissions(permissions)
                                        .build();

        SimpleFacebook.setConfiguration(facebookConfiguration);

        SimpleFacebook simpleFB = SimpleFacebook.getInstance(cordova.getActivity());
        
        if (simpleFB.isLogin()) {
//          JSONObject resp = prepareAccessTokenInfo(SimpleFacebook.getOpenSession());
            Session session = simpleFB.getSession();
            JSONObject resp = prepareAccessTokenInfo(session);
            callbackContext.success(resp);
        } else {
            callbackContext.success("");
        }       
    }

    private JSONObject prepareAccessTokenInfo(Session session) {
        JSONObject r = new JSONObject();
        try {
            r.put("accessToken", session.getAccessToken());
            r.put("expirationDate", session.getExpirationDate().getTime());
            JSONArray permissions = new JSONArray();
            List<String> parr = session.getPermissions();
            for (String item : parr) {
                permissions.put(item);
            }
            r.put("permissions", permissions);
        } catch (JSONException e) {
            Log.e(TAG, "Exception when preparing access token json", e);
            return null;
        }
        return r;
    }

    public void getUserInfo(final CallbackContext callbackContext) {
        final SimpleFacebook mSimpleFacebook = SimpleFacebook.getInstance(cordova.getActivity());
        
        OnProfileListener onProfileRequestListener = new OnProfileListener() {
            @Override
            public void onFail(String reason) {
                // insure that you are logged in before getting the profile
                Log.w(TAG, reason);
                callbackContext.error(reason);
            }

            @Override
            public void onException(Throwable throwable) {
                Log.e(TAG, "Bad thing happened", throwable);
                callbackContext.error(throwable.getMessage());
            }

            @Override
            public void onThinking() {
                // show progress bar or something to the user while fetching
                // profile
                Log.i(TAG, "Thinking...");
            }

            @Override
            public void onComplete(Profile profile) {
                callbackContext.success(profile.getGraphObject().getInnerJSONObject());
            }

        };

        // do the get profile action
        mSimpleFacebook.getProfile(onProfileRequestListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        SimpleFacebook mSimpleFacebook = SimpleFacebook.getInstance(cordova
                .getActivity());
        mSimpleFacebook.onActivityResult(cordova.getActivity(), requestCode,
                resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
