package hwang.daemin.kangbuk.fragments.week;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.LeadingMarginSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import hwang.daemin.kangbuk.R;
import hwang.daemin.kangbuk.common.CustomJSONObjectRequest;
import hwang.daemin.kangbuk.common.My;
import hwang.daemin.kangbuk.common.MyVolley;


/**
 * Created by user on 2016-06-14.
 */
public class WeekStudyFragment extends Fragment {
    private ProgressBar bar;
    private static String url_get_weekly0 = "http://browsable.cafe24.com/weekly/get_weekly0.php";
    private static final String TAG_SUCCESS= "success";
    private static final String TAG_WEEKLY0 = "weekly0";
    private static final String TAG_WEEKLY0_5 = "weekly0_5";
    private static final String TAG_WEEKLY0_6 = "weekly0_6";
    private static final String TAG_WEEKLY6_3 = "weekly6_3";
    TextView weekly6_1;
    TextView weekly6_2;
    TextView weekly6_3;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_week_study,container,false);
        My.INFO.backKeyName ="";
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.nav_week_study));
        bar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        weekly6_1 = (TextView) rootView.findViewById(R.id.weekly6_1);
        weekly6_2 = (TextView) rootView.findViewById(R.id.weekly6_2);
        weekly6_3 = (TextView) rootView.findViewById(R.id.weekly6_3);
        getWeekStudy(getActivity());
        return rootView;
    }
    public void getWeekStudy(final Context context) {
        CustomJSONObjectRequest rq = new CustomJSONObjectRequest(Request.Method.POST,
                url_get_weekly0, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int success = response.getInt(TAG_SUCCESS);
                            if (success==1) {
                                JSONArray weekly0 = response.getJSONArray(TAG_WEEKLY0);
                                for (int i = 0; i < weekly0.length(); i++) {
                                    JSONObject c = weekly0.getJSONObject(i);
                                    weekly6_1.setText(c.getString(TAG_WEEKLY0_5));
                                    weekly6_2.setText(c.getString(TAG_WEEKLY0_6));
                                    //weekly6_3.setText(c.getString(TAG_WEEKLY6_3));
                                    weekly6_3.setText(createIndentedText(c.getString(TAG_WEEKLY6_3), 0, 41));
                                }
                            }else if (success == 0) {
                                Toast.makeText(context, "Invalid email or password..", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(context, "Something went wrong.Please try again..", Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        bar.setVisibility(View.GONE);
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                bar.setVisibility(View.GONE);
                Toast.makeText(context, context.getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
        MyVolley.getRequestQueue().add(rq);
    }
    static SpannableString createIndentedText(String text, int marginFirstLine, int marginNextLines) {
        SpannableString result=new SpannableString(text);
        result.setSpan(new LeadingMarginSpan.Standard(marginFirstLine, marginNextLines),0,text.length(),0);
        return result;
    }
}
