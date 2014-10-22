package edu.berkeley.cs160.ralpharroyo.drawforfreespeech;

// java imports

import org.json.JSONException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

// android imports

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.MediaStore;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View.OnClickListener;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.LinearLayout;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;

// Flickr imports;
import com.gmail.yuyang226.flickrj.sample.android.FlickrHelper;
import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.REST;
import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.PhotoList;
import com.googlecode.flickrjandroid.photos.PhotosInterface;
import com.googlecode.flickrjandroid.photos.SearchParameters;

import com.gmail.yuyang226.flickrj.sample.android.FlickrjActivity;
import com.gmail.yuyang226.flickrj.sample.android.FlickrHelper;

// Toq imports

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.ResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.Card;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.NotificationTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteToqNotification;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.DeckOfCardsLauncherIcon;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.util.ParcelableUtil;

import javax.xml.parsers.ParserConfigurationException;


// Other imports


public class Draw extends Activity implements OnClickListener {

    private float smallBrush, mediumBrush, largeBrush;
    private DrawingView drawView;
    private ImageButton currPaint, drawBtn, eraseBtn, newBtn, saveBtn, uploadBtn;
    private Bitmap recent_bitmap;

    // Toq

    private final static String PREFS_FILE= "prefs_file";
    private final static String DECK_OF_CARDS_KEY= "deck_of_cards_key";
    private final static String DECK_OF_CARDS_VERSION_KEY= "deck_of_cards_version_key";

    private DeckOfCardsManager mDeckOfCardsManager;
    private RemoteDeckOfCards mRemoteDeckOfCards;
    private RemoteResourceStore mRemoteResourceStore;
    private CardImage[] mCardImages;
    private ToqBroadcastReceiver toqReceiver;
    private DeckOfCardsEventListener deckOfCardsEventListener;
    private boolean notificationSent = false;
    private int time = 5000; // milliseconds to update location.
    private long prev_time_sent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        LinearLayout paintLayout = (LinearLayout)findViewById(R.id.paint_colors);
        drawView = (DrawingView)findViewById(R.id.drawing);
        currPaint = (ImageButton) paintLayout.getChildAt(0);
        currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));

        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);
        drawBtn = (ImageButton)findViewById(R.id.draw_btn);
        drawBtn.setOnClickListener(this);
        drawView.setBrushSize(smallBrush);

        eraseBtn = (ImageButton)findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);

        newBtn = (ImageButton)findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);

        saveBtn = (ImageButton)findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(this);

        uploadBtn = (ImageButton)findViewById(R.id.upload_btn);
        uploadBtn.setOnClickListener(this);

        // Toq

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                float[] results = new float[3];
                Location.distanceBetween(latitude, longitude, 37.86965, -122.25914, results);
                float result = results[0];
                if (result <= 50){
                    if (notificationSent){
                        if(System.currentTimeMillis() - prev_time_sent >= 600000){
                            prev_time_sent = System.currentTimeMillis();
                            sendNotification();
                        }
                    }
                    else {
                        prev_time_sent = System.currentTimeMillis();
                        notificationSent = true;
                        sendNotification();
                    }

                }
                else{
                    if(System.currentTimeMillis() - prev_time_sent >= 600000){
                        prev_time_sent = System.currentTimeMillis();
                        notificationSent = false;
                    }
                }

                // Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_SHORT).show();
                // Toast.makeText(getApplicationContext(), "you are " + Float.toString(result) + "meters away.", Toast.LENGTH_SHORT).show();
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Get update every 5 seconds
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, locationListener);
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Get update every 5 seconds
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
        }

        mDeckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
        toqReceiver = new ToqBroadcastReceiver();
        init();
        install();
        deckOfCardsEventListener = new DeckOfCardsEventListenerImpl();
        mDeckOfCardsManager.addDeckOfCardsEventListener(deckOfCardsEventListener);

    }

    /**
     * @see android.app.Activity#onStart()
     * This is called after onCreate(Bundle) or after onRestart() if the activity has been stopped
     */
    protected void onStart(){
        super.onStart();

        Log.d(Constants.TAG, "ToqApiDemo.onStart");
        // If not connected, try to connect
        if (!mDeckOfCardsManager.isConnected()){
            try{
                mDeckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.draw, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void paintClicked(View view){
        //use chosen color
        drawView.setErase(false);

        if(view!=currPaint){
            //update color

            ImageButton imgView = (ImageButton)view;
            String color = view.getTag().toString();
            drawView.setColor(color);

            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint = (ImageButton) view;
        }
    }

    @Override
    public void onClick(View view){
        //respond to clicks

        if(view.getId()==R.id.draw_btn){
            //draw button clicked
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Brush size:");

            brushDialog.setContentView(R.layout.brush_chooser);

            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);

            smallBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {

                    drawView.setBrushSize(smallBrush);
                    drawView.setLastBrushSize(smallBrush);
                    brushDialog.dismiss();
                    drawView.setErase(false);
                }
            });

            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setBrushSize(mediumBrush);
                    drawView.setLastBrushSize(mediumBrush);
                    brushDialog.dismiss();
                    drawView.setErase(false);
                }
            });

            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setBrushSize(largeBrush);
                    drawView.setLastBrushSize(largeBrush);
                    brushDialog.dismiss();
                    drawView.setErase(false);
                }
            });
            brushDialog.show();
        }
        else if (view.getId()==R.id.erase_btn){
            //switch to erase - choose size
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Eraser size:");
            brushDialog.setContentView(R.layout.brush_chooser);

            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(smallBrush);
                    brushDialog.dismiss();
                }
            });

            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(mediumBrush);
                    brushDialog.dismiss();
                }
            });

            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(largeBrush);
                    brushDialog.dismiss();
                }
            });

            brushDialog.show();

        }
        else if (view.getId() == R.id.new_btn){
            AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
            newDialog.setTitle("New drawing");
            newDialog.setMessage("Start new drawing (you will lose the current drawing)?");
            newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    drawView.startNew();
                    dialog.dismiss();
                }
            });
            newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            newDialog.show();
        }
        else if(view.getId()==R.id.save_btn){
            //save drawing
            AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
            saveDialog.setTitle("Save drawing");
            saveDialog.setMessage("Save drawing to device Gallery?");
            saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    //save drawing
                    drawView.setDrawingCacheEnabled(true);
                    String imgSaved = MediaStore.Images.Media.insertImage(
                            getContentResolver(), drawView.getDrawingCache(),
                            UUID.randomUUID().toString()+".png", "drawing");

                    if(imgSaved!=null){
                        Toast savedToast = Toast.makeText(getApplicationContext(),
                                "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
                        savedToast.show();
                    }
                    else{
                        Toast unsavedToast = Toast.makeText(getApplicationContext(),
                                "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
                        unsavedToast.show();
                    }
                    drawView.destroyDrawingCache();
                }
            });
            saveDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            saveDialog.show();
        }
        else if (view.getId()==R.id.upload_btn){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivityForResult(intent, 102);
        }
    }

    // Read an image from assets and return as a bitmap
    private Bitmap getBitmap(String fileName) throws Exception{

        try{
            InputStream is= getAssets().open(fileName);
            return BitmapFactory.decodeStream(is);
        }
        catch (Exception e){
            throw new Exception("An error occurred getting the bitmap: " + fileName, e);
        }
    }

    // TOQ methods

    private void install() {
        updateDeckOfCardsFromUI();
        try{
            mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
        }
        catch (RemoteDeckOfCardsException e){
            e.printStackTrace();
            // Toast.makeText(this, "Could not update Toq cards. Please install app on watch.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDeckOfCardsFromUI() {

        if (mRemoteDeckOfCards == null) {
            mRemoteDeckOfCards = createDeckOfCards();
        }
        ListCard listCard= mRemoteDeckOfCards.getListCard();

        mCardImages = new CardImage[6];
        String[] messageTexts = new String[6];
        String[] titleTexts = {"Art Goldberg", "Jack Weinburg", "Jackie Goldberg", "Joan Baez", "Mario Savio", "Michael Rossman"};
        try {
            mCardImages[0] = new CardImage("card.image.1", getBitmap("art_goldberg_toq.png"));
            mCardImages[1] = new CardImage("card.image.2", getBitmap("jack_weinberg_toq.png"));
            mCardImages[2] = new CardImage("card.image.3", getBitmap("jackie_goldberg_toq.png"));
            mCardImages[3] = new CardImage("card.image.4", getBitmap("joan_baez_toq.png"));
            mCardImages[4] = new CardImage("card.image.5", getBitmap("mario_savio_toq.png"));
            mCardImages[5] = new CardImage("card.image.6", getBitmap("michael_rossman_toq.png"));
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Can't get picture icon");
            return;
        }
        messageTexts[0] = "Draw \"Now\"";
        messageTexts[1] = "Draw \"FSM\"";
        messageTexts[2] = "Draw \"SLATE\"";
        messageTexts[3] = "Draw a Megaphone";
        messageTexts[4] = "Draw your idea of free speech";
        messageTexts[5] = "Draw \"Free Speech\"";

        for(int i = 0; i < 6; i++){
            SimpleTextCard simpleTextCard = (SimpleTextCard)listCard.childAtIndex(i);
            simpleTextCard.setTitleText(titleTexts[i]);
            simpleTextCard.setMessageText(new String[]{messageTexts[i]});
            mRemoteResourceStore.addResource(mCardImages[i]);
            simpleTextCard.setCardImage(mRemoteResourceStore, mCardImages[i]);
            simpleTextCard.setReceivingEvents(true);
        }

        /*
        SimpleTextCard simpleTextCard= (SimpleTextCard)listCard.childAtIndex(0);
        // simpleTextCard.setHeaderText("Hello World");
        simpleTextCard.setTitleText("Art Goldberg");
        String[] messages = {"Draw \"FSM\""};

        CardImage cardImage = null;
        try{
            cardImage = new CardImage("card.image.1", getBitmap("art_goldberg_toq.png"));
        }
        catch (Exception e){
            Toast.makeText(this, "ERROR WITH IMAGE", Toast.LENGTH_SHORT).show();
        }
        mRemoteResourceStore.addResource(cardImage);
        simpleTextCard.setCardImage(mRemoteResourceStore, cardImage);
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(false);
        */

        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to Create SimpleTextCard.  Please install app on Toq watch.", Toast.LENGTH_SHORT).show();
        }
        // simpleTextCard.setShowDivider(true);*/
    }
    //
    // Create some cards with example content
    private RemoteDeckOfCards createDeckOfCards(){

        ListCard listCard= new ListCard();
        for(int i = 0; i < 7; i++){
            SimpleTextCard simpleTextCard= new SimpleTextCard("card" + (i + 1));
            listCard.add(simpleTextCard);
        }
        return new RemoteDeckOfCards(this, listCard);
    }

    //    Initialise
    private void init(){

        // Create the resource store for icons and images
        mRemoteResourceStore= new RemoteResourceStore();
        // Try to retrieve a stored deck of cards
        try {
            mRemoteDeckOfCards = createDeckOfCards();
        }
        catch (Throwable th){
            th.printStackTrace();
        }
    }

    private void sendNotification(){
        String[] message = new String[1];
        int figure = (int) (Math.random() * 6);
        switch(figure){
            case 0: message[0] = "Art Goldberg"; break;
            case 1: message[0] = "Jack Weinberg"; break;
            case 2: message[0] = "Jackie Goldberg"; break;
            case 3: message[0] = "Joan Baez"; break;
            case 4: message[0] = "Mario Savio"; break;
            case 5: message[0] = "Michael Rossman"; break;
        }
        // message[1] = "what's up";
        // Create a NotificationTextCard
        NotificationTextCard notificationCard = new NotificationTextCard(System.currentTimeMillis(),
                "Draw Request", message);

        // Draw divider between lines of text
        notificationCard.setShowDivider(true);
        // Vibrate to alert user when showing the notification
        notificationCard.setVibeAlert(true);
        // Create a notification with the NotificationTextCard we made
        RemoteToqNotification notification = new RemoteToqNotification(this, notificationCard);

        try {
            // Send the notification
            mDeckOfCardsManager.sendNotification(notification);
            // Toast.makeText(this, "Sent Notification", Toast.LENGTH_SHORT).show();
            notificationSent = true;
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send Notification. Please install app on watch.", Toast.LENGTH_SHORT).show();
        }
    }

    private class DeckOfCardsEventListenerImpl implements DeckOfCardsEventListener{
        @Override
        public void onCardOpen(String cardId){
            runOnUiThread(new Runnable(){
                public void run(){
                    Intent intent = new Intent(Draw.this, Draw.class);
                    startActivity(intent);
                }
            });
        }

        @Override
        public void onCardVisible(String cardId){

        }

        @Override
        public void onCardInvisible(String cardId){

        }

        @Override
        public void onCardClosed(String cardId){

        }

        @Override
        public void onMenuOptionSelected(final String cardId, String menuOption){

        }

        @Override
        public void onMenuOptionSelected(String cardId, String menuOption, String quickReplyOption){

        }
    }

    File fileUri;

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        @SuppressWarnings("deprecation")
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 102) {

            if (resultCode == Activity.RESULT_OK) {
                Uri tmp_fileUri = data.getData();

                //((ImageView) findViewById(R.id.imview))
                //        .setImageURI(tmp_fileUri);

                String selectedImagePath = getPath(tmp_fileUri);
                fileUri = new File(selectedImagePath);
                // Toast.makeText(this, "GOT HERE", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(),
                        FlickrjActivity.class);
                intent.putExtra("flickImagePath", fileUri.getAbsolutePath());
                showImage();
                startActivity(intent);
            }

        }
    };

    private void showImage() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    String svr="www.flickr.com";

                    REST rest=new REST();
                    rest.setHost(svr);

                    //initialize Flickr object with key and rest
                    Flickr flickr=new Flickr(FlickrHelper.API_KEY,rest);

                    //initialize SearchParameter object, this object stores the search keyword
                    SearchParameters searchParams=new SearchParameters();
                    searchParams.setSort(SearchParameters.DATE_TAKEN_DESC);

                    //Create tag keyword array
                    String[] tags=new String[]{"cs160fsm"};
                    searchParams.setTags(tags);

                    //Initialize PhotosInterface object
                    PhotosInterface photosInterface=flickr.getPhotosInterface();
                    //Execute search with entered tags
                    PhotoList photoList=photosInterface.search(searchParams,20,1);

                    //get search result and fetch the photo object
                    // Toast.makeText(getApplicationContext(), "GOT HERE1", Toast.LENGTH_SHORT).show();
                    if(photoList!=null){
                        //get photo object
                        // Toast.makeText(getApplicationContext(), "GOT HERE2", Toast.LENGTH_SHORT).show();
                        Photo photo=(Photo)photoList.get(0);

                        //Get small square url photo
                        InputStream is = photo.getMediumAsStream();
                        Bitmap bm = BitmapFactory.decodeStream(is);
                        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, 250, 288, false);

                        ListCard listCard = mRemoteDeckOfCards.getListCard();
                        CardImage recent = new CardImage("card.image.7", resizedBitmap);
                        SimpleTextCard simpleTextCard = (SimpleTextCard) listCard.childAtIndex(6);
                        simpleTextCard.setTitleText("Recent FSM Drawing");
                        // simpleTextCard.setMessageText(new String[]{messageTexts[i]});
                        mRemoteResourceStore.addResource(recent);
                        simpleTextCard.setCardImage(mRemoteResourceStore, recent);
                        simpleTextCard.setReceivingEvents(false);
                        try {
                            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
                            // Toast.makeText(getApplicationContext(), "YOOOO", Toast.LENGTH_SHORT).show();
                        } catch (RemoteDeckOfCardsException e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Failed to Create SimpleTextCard.  Please install app on Toq watch.", Toast.LENGTH_SHORT).show();
                        }
                        try{
                            mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
                        }
                        catch (RemoteDeckOfCardsException e){
                            e.printStackTrace();
                            // Toast.makeText(this, "Could not update Toq cards. Please install app on watch.", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (FlickrException e) {
                    e.printStackTrace();
                } catch (IOException e ) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }
}
