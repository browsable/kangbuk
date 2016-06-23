package hwang.daemin.kangbuk.fragments.picture;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.yongchun.library.view.ImageSelectorActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import hwang.daemin.kangbuk.R;
import hwang.daemin.kangbuk.data.PictureData;
import hwang.daemin.kangbuk.data.User;
import hwang.daemin.kangbuk.firebase.fUtil;
import pub.devrel.easypermissions.EasyPermissions;

public class NewPictureActivity extends AppCompatActivity implements
        EasyPermissions.PermissionCallbacks,
        NewPicUploadTaskFragment.TaskCallbacks {

    private static final String TAG = "NewPostActivity";
    public static final String TAG_TASK_FRAGMENT = "NewPicUploadTaskFragment";

    private LinearLayout btPicture, llGridView;
    private EditText mTitleField;
    private EditText mBodyField;
    //public static final String EXTRA_IMAGES = "extraImages";
    private RecyclerView resultRecyclerView;
    private ImageView singleImageView;
    private Bitmap mResizedBitmap;
    private Bitmap mThumbnail;
    private ArrayList<String> images = new ArrayList<>();
    private ArrayList<String> fullURLList = new ArrayList<>();
    private static final int THUMBNAIL_MAX_DIMENSION = 480;
    private static final int FULL_SIZE_MAX_DIMENSION = 960;
    private static final int RC_CAMERA_PERMISSIONS = 102;
    private static final String[] cameraPerms = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private NewPicUploadTaskFragment mTaskFragment;
    private String title;
    private String body;
    private String userId;
    private User user;
    private String key;
    private int imgCnt;
    @Override
    public void onDestroy() {
        // store the data in the fragment
        if (mResizedBitmap != null) {
            mTaskFragment.setSelectedBitmap(mResizedBitmap);
        }
        if (mThumbnail != null) {
            mTaskFragment.setThumbnail(mThumbnail);
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        imgCnt=0;
        mTitleField = (EditText) findViewById(R.id.field_title);
        mBodyField = (EditText) findViewById(R.id.field_body);
        btPicture = (LinearLayout) findViewById(R.id.btPicture);
        llGridView = (LinearLayout) findViewById(R.id.llGridView);
        singleImageView = (ImageView) findViewById(R.id.single_image);
        resultRecyclerView = (RecyclerView) findViewById(R.id.result_recycler);
        resultRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        findViewById(R.id.fab_submit_post).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(images!=null&&images.size()!=0)
                    submitPost();
                else
                    Toast.makeText(NewPictureActivity.this, "사진을 선택하세요", Toast.LENGTH_SHORT).show();
            }
        });
        btPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !EasyPermissions.hasPermissions(NewPictureActivity.this, cameraPerms)) {
                    EasyPermissions.requestPermissions(this,
                            "사진 업로드를 위해 저장소에 접근합니다",
                            RC_CAMERA_PERMISSIONS, cameraPerms);
                    return;
                }
                ImageSelectorActivity.start(NewPictureActivity.this, 10, 1, true, true, false);
            }
        });
        if (images.size() == 1) {
            resultRecyclerView.setVisibility(View.GONE);
            Glide.with(NewPictureActivity.this)
                    .load(new File(images.get(0)))
                    .into(singleImageView);
        } else {
            singleImageView.setVisibility(View.GONE);
            resultRecyclerView.setAdapter(new GridAdapter());
        }
        FragmentManager fm = getSupportFragmentManager();
        mTaskFragment = (NewPicUploadTaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);
        // create the fragment and data the first time
        if (mTaskFragment == null) {
            // add the fragment
            mTaskFragment = new NewPicUploadTaskFragment();
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
        }
        Bitmap selectedBitmap = mTaskFragment.getSelectedBitmap();
        Bitmap thumbnail = mTaskFragment.getThumbnail();
        if (selectedBitmap != null) {
            mResizedBitmap = selectedBitmap;
        }
        if (thumbnail != null) {
            mThumbnail = thumbnail;
        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == ImageSelectorActivity.REQUEST_IMAGE) {
            images = (ArrayList<String>) data.getSerializableExtra(ImageSelectorActivity.REQUEST_OUTPUT);
            //startActivity(new Intent(this,SelectResultActivity.class).putExtra(SelectResultActivity.EXTRA_IMAGES,images));
            btPicture.setVisibility(View.GONE);
            llGridView.setVisibility(View.VISIBLE);
        }
    }

    private void submitPost() {
        title = mTitleField.getText().toString();
        body = mBodyField.getText().toString();

        if (TextUtils.isEmpty(title)) {
            mTitleField.setError("제목을 입력하세요");
            return;
        }

        userId = fUtil.getCurrentUserId();
        fUtil.databaseReference.child("user").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    user = dataSnapshot.getValue(User.class);
                    if (user == null) {
                        Log.e(TAG, "User " + userId + " is unexpectedly null");
                        Toast.makeText(NewPictureActivity.this,
                                "권한이 필요합니다",
                                Toast.LENGTH_SHORT).show();
                    } else {
                            String path = images.get(imgCnt);
                            mTaskFragment.resizeBitmapWithPath(path,THUMBNAIL_MAX_DIMENSION);
                            mTaskFragment.resizeBitmapWithPath(path,FULL_SIZE_MAX_DIMENSION);
                    }
                    finish();
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                }
        });
    }

    private class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {

        @Override
        public GridAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_result, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(GridAdapter.ViewHolder holder, final int position) {
            Glide.with(NewPictureActivity.this)
                    .load(new File(images.get(position)))
                    .centerCrop()
                    .into(holder.imageView);
            holder.btRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    images.remove(position);
                    notifyDataSetChanged();
                    if(images.size()==0){
                        btPicture.setVisibility(View.VISIBLE);
                        llGridView.setVisibility(View.GONE);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            Button btRemove;

            public ViewHolder(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView.findViewById(R.id.image);
                btRemove = (Button) itemView.findViewById(R.id.btRemove);
            }
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    @Override
    public void onBitmapResized(Bitmap resizedBitmap, int mMaxDimension) {
            if (resizedBitmap == null) {
                Log.e(TAG, "Couldn't resize bitmap in background task.");
                Toast.makeText(getApplicationContext(), "Couldn't resize bitmap.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (mMaxDimension == THUMBNAIL_MAX_DIMENSION) {
                mThumbnail = resizedBitmap;
            } else if (mMaxDimension == FULL_SIZE_MAX_DIMENSION) {
                mResizedBitmap = resizedBitmap;
            }

            if (mThumbnail != null && mResizedBitmap != null) {
                Long now = System.currentTimeMillis();
                StorageReference fullSizeRef = fUtil.getStorePictureRef().child(now.toString()).child("full");
                StorageReference thumbnailRef = fUtil.getStorePictureRef().child(now.toString()).child("thumb");
                mTaskFragment.uploadPost(mResizedBitmap, fullSizeRef, mThumbnail, thumbnailRef, now.toString());
            }
    }

    @Override
    public void onPictureUploaded(final String error, final String fullURL, final String thumbURL) {
        NewPictureActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (error != null) {
                    Toast.makeText(NewPictureActivity.this, error, Toast.LENGTH_SHORT).show();
                }else{
                    if(imgCnt==0){
                        key = fUtil.getPictureRef().push().getKey();
                        Long time = System.currentTimeMillis();
                        fUtil.getPictureRef().child(key).setValue(new PictureData(userId,user.getuName(),title,body,thumbURL,time));
                        fUtil.getPictureDetailRef().child(key).push().setValue(fullURL);
                        String path = images.get(++imgCnt);
                        mTaskFragment.resizeBitmapWithPath(path, THUMBNAIL_MAX_DIMENSION);
                        mTaskFragment.resizeBitmapWithPath(path, FULL_SIZE_MAX_DIMENSION);
                    }else if(imgCnt!=images.size()){
                        String path = images.get(imgCnt++);
                        mTaskFragment.resizeBitmapWithPath(path, THUMBNAIL_MAX_DIMENSION);
                        mTaskFragment.resizeBitmapWithPath(path, FULL_SIZE_MAX_DIMENSION);
                        fUtil.getPictureDetailRef().child(key).push().setValue(fullURL);
                    }
                    /*fullURLList.add(fullURL);
                    Log.i("test",fullURLList.size()+"");*/
                    /*f(fullURLList.size()==images.size()) {

                    }else{*/
                    //}
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    /*
    private void writeNewPost(String userId, String username, String title, String body) {
        Map<String, Object> postValues = post.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/posts/" + key, postValues);
        childUpdates.put("/user-posts/" + userId + "/" + key, postValues);
        FUtil.databaseReference.updateChildren(childUpdates);
    }*/
}