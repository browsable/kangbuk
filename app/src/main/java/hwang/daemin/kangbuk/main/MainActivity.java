package hwang.daemin.kangbuk.main;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import hwang.daemin.kangbuk.R;
import hwang.daemin.kangbuk.auth.SignInActivity;
import hwang.daemin.kangbuk.common.BackPressCloseHandler;
import hwang.daemin.kangbuk.common.DialDefault;
import hwang.daemin.kangbuk.common.My;
import hwang.daemin.kangbuk.firebase.fUtil;
import hwang.daemin.kangbuk.fragments.BibleFragment;
import hwang.daemin.kangbuk.fragments.CalendarFragment;
import hwang.daemin.kangbuk.fragments.ColumnFragment;
import hwang.daemin.kangbuk.fragments.MainFragment;
import hwang.daemin.kangbuk.fragments.PlaceFragment;
import hwang.daemin.kangbuk.fragments.ScheduleFragment;
import hwang.daemin.kangbuk.fragments.file.YoutubeFragment;
import hwang.daemin.kangbuk.fragments.picture.PictureFragment;
import hwang.daemin.kangbuk.fragments.week.WeekAfternoonFragment;
import hwang.daemin.kangbuk.fragments.week.WeekDailyFragment;
import hwang.daemin.kangbuk.fragments.week.WeekGroupFragment;
import hwang.daemin.kangbuk.fragments.week.WeekMidFragment;
import hwang.daemin.kangbuk.fragments.week.WeekNoticeFragment;
import hwang.daemin.kangbuk.fragments.week.WeekServiceFragment;
import hwang.daemin.kangbuk.fragments.week.WeekStudyFragment;
import hwang.daemin.kangbuk.fragments.week.WeekWedFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_INVITE = 1;
    private BackPressCloseHandler backPressCloseHandler;
    // Firebase instance variables

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences pref = getSharedPreferences("USERINFO", MODE_PRIVATE);
        My.INFO.loginType = pref.getInt("loginType",0);
        if(My.INFO.loginType==1) FacebookSdk.sdkInitialize(getApplicationContext());

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .addApi(AppInvite.API)
                .build();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fUtil.databaseReference.child("appversion").child("version").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String serverVersion = (String) dataSnapshot.getValue();
                if(serverVersion==null) return;
                if (!serverVersion.equals(My.INFO.appVer)){
                    DialDefault dd = new DialDefault(MainActivity.this,
                            getResources().getString(R.string.update_title),
                            getResources().getString(R.string.update_notice),
                            0);
                    dd.show();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.content_frame, new MainFragment()).commit();

        backPressCloseHandler = new BackPressCloseHandler(this);

        Map<String, Object> bibleRandom = new HashMap<>();
        Random r = new Random();
        bibleRandom.put("biblenum",String.valueOf(r.nextInt(239)));
        fUtil.databaseReference.child("user").child(fUtil.firebaseUser.getUid()).updateChildren(bibleRandom);
    }

    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            backPressCloseHandler.onBackPressed(My.INFO.backKeyName);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_profile:
                Intent i = new Intent(MainActivity.this, UserDetailActivity.class);
                i.putExtra("uId",fUtil.getCurrentUserId());
                startActivity(i);
                return true;
            case R.id.sign_out_menu:
                fUtil.firebaseAuth.signOut();
                if(My.INFO.loginType==0)
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                else if(My.INFO.loginType==1)
                    LoginManager.getInstance().logOut();
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            case R.id.invite_menu:
                sendInvitation();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        FragmentManager fm = getFragmentManager();
        // Handle navigation view item clicks here.
        switch (item.getItemId()){
            case R.id.nav_home:
                My.INFO.backKeyName ="MainFragment";
                fm.beginTransaction().replace(R.id.content_frame,new MainFragment()).commit();
                break;
            case R.id.nav_bible:
                fm.beginTransaction().replace(R.id.content_frame,new BibleFragment()).commit();
                break;
            case R.id.nav_column:
                fm.beginTransaction().replace(R.id.content_frame,new ColumnFragment()).commit();
                break;
            case R.id.nav_calendar:
                fm.beginTransaction().replace(R.id.content_frame,new CalendarFragment()).commit();
                break;
            case R.id.nav_picture:
                fm.beginTransaction().replace(R.id.content_frame,new PictureFragment()).commit();
                break;
            case R.id.nav_schedule:
                fm.beginTransaction().replace(R.id.content_frame,new ScheduleFragment()).commit();
                break;
            case R.id.nav_place:
                fm.beginTransaction().replace(R.id.content_frame,new PlaceFragment()).commit();
                break;
            case R.id.nav_youtube:
                fm.beginTransaction().replace(R.id.content_frame,new YoutubeFragment()).commit();
                break;
            case R.id.nav_week_mid:
                fm.beginTransaction().replace(R.id.content_frame,new WeekMidFragment()).commit();
                break;
            case R.id.nav_week_afternoon:
                fm.beginTransaction().replace(R.id.content_frame,new WeekAfternoonFragment()).commit();
                break;
            case R.id.nav_week_wed:
                fm.beginTransaction().replace(R.id.content_frame,new WeekWedFragment()).commit();
                break;
            case R.id.nav_week_service:
                fm.beginTransaction().replace(R.id.content_frame,new WeekServiceFragment()).commit();
                break;
            case R.id.nav_week_group:
                fm.beginTransaction().replace(R.id.content_frame,new WeekGroupFragment()).commit();
                break;
            case R.id.nav_week_study:
                fm.beginTransaction().replace(R.id.content_frame,new WeekStudyFragment()).commit();
                break;
            case R.id.nav_week_daily:
                fm.beginTransaction().replace(R.id.content_frame,new WeekDailyFragment()).commit();
                break;
            case R.id.nav_week_notice:
                fm.beginTransaction().replace(R.id.content_frame,new WeekNoticeFragment()).commit();
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }
    private void sendInvitation() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode +
                ", resultCode=" + resultCode);

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Check how many invitations were sent.
                String[] ids = AppInviteInvitation
                        .getInvitationIds(resultCode, data);
                Log.d(TAG, "Invitations sent: " + ids.length);
            } else {
                // Sending failed or it was canceled, show failure message to
                // the user
                Log.d(TAG, "Failed to send invitation.");
            }
        }
    }
}
