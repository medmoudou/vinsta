package mdev.vinsta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

public class MainActivity extends AppCompatActivity {

    Button searchBtn,saveBtn,retryBtn;
    ImageView profilePic,hdPic;
    TextInputEditText usr;
    TextView fullName,bio,nbFollow,nbFollowed,nbPosts;
    ConstraintLayout searchLayout,resultLayout;
    AdView mAdView;
    InterstitialAd mInterstitialAd;
    final Transformation transformation = new RoundedCornersTransformation(150, 0); // to make profile pic rounded

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation of Admob

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });

        // Initialize and load Admob banner
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Initialize and load Admob Interstitial
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.admob_interstitial_ad));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        // Admob events
        mInterstitialAd.setAdListener(new AdListener() {
            // Event when ad closed
            @Override
            public void onAdClosed() {
                getPic(); // Call getPic function to display picture when ad closed
            }
        });

        // Initialisation of controls
        hdPic = findViewById(R.id.hdPic);
        profilePic = findViewById(R.id.profilepic);
        searchBtn = findViewById(R.id.download);
        saveBtn = findViewById(R.id.savebtn);
        retryBtn = findViewById(R.id.retry);
        usr = findViewById(R.id.username);
        fullName = findViewById(R.id.txtname);
        bio = findViewById(R.id.txtbio);
        nbFollow = findViewById(R.id.txtfollow);
        nbFollowed = findViewById(R.id.txtfollowed);
        nbPosts = findViewById(R.id.txtposts);
        searchLayout = findViewById(R.id.SearchLayout);
        resultLayout = findViewById(R.id.ResultLayout);

        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Button to close the result layout and show the search layout again !
                resultLayout.setVisibility(View.GONE);
                searchLayout.setVisibility(View.VISIBLE);
                hdPic.setVisibility(View.GONE);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Detect if user has not grant permission to show him the request dialog
                if (!havePerm()) {
                    reqPerm(); //request permission
                }
                // this part for save picture in gallery
                try{
                    BitmapDrawable draw = (BitmapDrawable) hdPic.getDrawable();
                    Bitmap bitmap = draw.getBitmap();
                    FileOutputStream outStream = null;
                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File(sdCard.getAbsolutePath() + "/Vinsta"); // you can change Vinsta to the folder you want to save picture in
                    if (!dir.exists()) { // check if folder is exists or not to create one
                        dir.mkdirs();
                    }
                    String fileName = String.format("%d.jpg", System.currentTimeMillis()); //give the picture current time as a name
                    File outFile = new File(dir, fileName);
                    outStream = new FileOutputStream(outFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    outStream.flush();
                    outStream.close();
                    // this part for reload gallery to display the pic on gallery
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(outFile));
                    sendBroadcast(intent);
                    Toast.makeText(MainActivity.this,"Photo saved successfully",Toast. LENGTH_SHORT).show();
                }catch(IOException e){
                    Toast.makeText(MainActivity.this,"Error !",Toast. LENGTH_SHORT).show();
                }
            }
        });

        // This search button to display the profile and the picture of the username wroted on the text field
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check first if ad loaded to show it first then show the profile and the picture
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    getPic(); // this function to display the profile and full size picture
                }
            }
        });

        // Detect if user has not grant permission to show him the request dialog
        if (!havePerm()) {
            reqPerm(); //request permission
        }


    }

    //-------------------------------------------------------------------------------------------------------------------
    // THIS method to get the profile info from Instagram API to display them on labels and to get full size profile pic
    //-------------------------------------------------------------------------------------------------------------------

    private void getPic() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                // This part to get the JSON from the profile of Instagram

                final StringBuilder builder = new StringBuilder();
                try {
                    String username = usr.getText().toString().replace(" ","");
                    String url="https://www.instagram.com/" +username + "/?__a=1";
                    Document doc = Jsoup
                            .connect(url)
                            .ignoreContentType(true)
                            .get();
                    Element body = doc.body();
                    builder.append(body.text());
                } catch (Exception e) {
                    //e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // This is the important part we got the Json from previous part now we are going to extract
                        // informations from this Json ..

                        try {
                            JSONObject jsonObj = new JSONObject(builder.toString()); // Get the full Json object
                            JSONObject graphql = jsonObj.getJSONObject("graphql"); // Get the graphq1 object
                            JSONObject user = graphql.getJSONObject("user"); // Get the user object from graphq1 object
                            JSONObject nbfollow = user.getJSONObject("edge_follow"); // Get following number from user object
                            JSONObject nbfollowed = user.getJSONObject("edge_followed_by"); // Get followers number from user object
                            JSONObject nbposts = user.getJSONObject("edge_owner_to_timeline_media");// Get posts number from user object
                            // Get the full picture and display it on the both imageView the big and small one !
                            Picasso.get().load(user.getString("profile_pic_url_hd")).transform(transformation).into(profilePic);
                            Picasso.get().load(user.getString("profile_pic_url_hd")).into(hdPic);
                            // Display all of those informations !
                            fullName.setText(user.getString("full_name"));
                            bio.setText(user.getString("biography"));
                            nbFollow.setText(nbfollow.getString("count") + " Following");
                            nbFollowed.setText(nbfollowed.getString("count") + " Followers");
                            nbPosts.setText(nbposts.getString("count") + " Posts");

                            // Show the result layout and hide the search layout
                            resultLayout.setVisibility(View.VISIBLE);
                            searchLayout.setVisibility(View.GONE);
                            hdPic.setVisibility(View.VISIBLE);
                        } catch (final JSONException e) {
                            Toast.makeText(MainActivity.this, "Profile not found", Toast.LENGTH_LONG).show();
                        }

                    }
                });
            }
        }).start();
    }


    //-------------------------------------------------------------------------------------------------------------
    // THIS part to check if user already grant the permission to save picture on gallery and to ask for permission
    //-------------------------------------------------------------------------------------------------------------

    private boolean havePerm() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    private void reqPerm() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101); //Request storage permission
    }
}