package mdev.vinsta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
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
    final Transformation transformation = new RoundedCornersTransformation(150, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hdPic = (ImageView) findViewById(R.id.hdPic);
        profilePic = (ImageView) findViewById(R.id.profilepic);
        searchBtn = (Button) findViewById(R.id.download);
        saveBtn = (Button) findViewById(R.id.savebtn);
        retryBtn = (Button) findViewById(R.id.retry);
        usr = (TextInputEditText) findViewById(R.id.username);
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
                resultLayout.setVisibility(View.GONE);
                searchLayout.setVisibility(View.VISIBLE);
                hdPic.setVisibility(View.GONE);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    BitmapDrawable draw = (BitmapDrawable) hdPic.getDrawable();
                    Bitmap bitmap = draw.getBitmap();

                    FileOutputStream outStream = null;
                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File(sdCard.getAbsolutePath() + "/Vinsta");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    String fileName = String.format("%d.jpg", System.currentTimeMillis());
                    File outFile = new File(dir, fileName);
                    outStream = new FileOutputStream(outFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    outStream.flush();
                    outStream.close();
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(outFile));
                    sendBroadcast(intent);
                    Toast.makeText(MainActivity.this,"Photo saved successfully",Toast. LENGTH_SHORT).show();
                }catch(IOException e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,"Error !",Toast. LENGTH_SHORT).show();
                }
            }
        });
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ExtractJson();
            }
        });

    }


    private void ExtractJson() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder builder = new StringBuilder();
                try {
                    String username = usr.getText().toString().replace(" ","");
                    String url="https://www.instagram.com/" +username + "/?__a=1";
                    Document doc = Jsoup.connect(url).ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .get();
                    Element body = doc.body();
                    builder.append(body.text());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            JSONObject jsonObj = new JSONObject(builder.toString());
                            JSONObject graphql = jsonObj.getJSONObject("graphql");
                            JSONObject user = graphql.getJSONObject("user");
                            JSONObject nbfollow = user.getJSONObject("edge_follow");
                            JSONObject nbfollowed = user.getJSONObject("edge_followed_by");
                            JSONObject nbposts = user.getJSONObject("edge_owner_to_timeline_media");
                            Picasso.get().load(user.getString("profile_pic_url_hd")).transform(transformation).into(profilePic);
                            Picasso.get().load(user.getString("profile_pic_url_hd")).into(hdPic);
                            fullName.setText(user.getString("full_name"));
                            bio.setText(user.getString("biography"));
                            nbFollow.setText(nbfollow.getString("count") + " Following");
                            nbFollowed.setText(nbfollowed.getString("count") + " Followers");
                            nbPosts.setText(nbposts.getString("count") + " Posts");
                            resultLayout.setVisibility(View.VISIBLE);
                            searchLayout.setVisibility(View.GONE);
                            hdPic.setVisibility(View.VISIBLE);
                        } catch (final JSONException e) {
                            Toast.makeText(MainActivity.this, "Profile not found", Toast.LENGTH_LONG).show();
                            //e.printStackTrace();
                        }

                    }
                });
            }
        }).start();
    }

}
