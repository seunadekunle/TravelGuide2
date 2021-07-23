package com.example.travelguide.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.travelguide.R;
import com.example.travelguide.classes.Guide;
import com.example.travelguide.helpers.HelperClass;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.snackbar.Snackbar;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.example.travelguide.R.string.empty_text;
import static com.google.android.gms.common.util.IOUtils.toByteArray;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ComposeFragment} factory method to
 * create an instance of this fragment.
 */
public class ComposeFragment extends Fragment {

    public static final String TAG = "ComposeFragment";

    private static final String ARG_LONG = "longitude";
    private static final String ARG_LAT = "latitude";

    public static final int AUTOCOMPLETE_REQUEST_CODE = 1;

    private String photoFileName = "photo.jpg";

    private File audioFile;
    private File photoFile;
    private File videoFile;

    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    private int playerPos;

    // Activity Result Launcher
    ActivityResultLauncher<Intent> galleryActivityLauncher;
    ActivityResultLauncher<Intent> mediaActivityLauncher;
    ActivityResultLauncher<Intent> searchActivityLauncher;

    // parameters for passing data
    private Double longParam;
    private Double latParam;

    // default location
    private LatLng location = new LatLng(0, 0);

    private EditText etText;
    private Button locationBtn;
    private Button addBtn;
    private ImageButton mediaBtn;
    private ImageButton galleryBtn;
    private ImageButton audioBtn;
    private ImageButton clearBtn;
    private ImageView ivPreview;
    private VideoView vvPreview;
    private MediaController controller;

    // ui elements for the audio recorder
    private LinearLayout recordingLayout;
    private LinearLayout playLayout;
    private ImageButton recordBtn;
    private ImageButton playBtn;


    public ComposeFragment() {
        // Required empty public constructor
    }

//    /**
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment ComposeFragment.
//     */
    // TODO: Rename and change types and number of parameters
//    public static ComposeFragment newInstance(Double param1, Double param2) {
//        ComposeFragment fragment = new ComposeFragment();
//        Bundle args = new Bundle();
//
//        args.putDouble(ARG_LONG, param1);
//        args.putDouble(ARG_LAT, param2);
//
//        fragment.setArguments(args);
//        return fragment;
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//
//            longParam = getArguments().getDouble(ARG_LONG);
//            latParam = getArguments().getDouble(ARG_LAT);
//
//            // sets default location for new Travel Guide
//            location = new LatLng(longParam, latParam);
//
//        }
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_compose, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        locationBtn = view.findViewById(R.id.locationBtn);
        addBtn = view.findViewById(R.id.addBtn);
        etText = view.findViewById(R.id.etText);
        mediaBtn = view.findViewById(R.id.photoBtn);
        galleryBtn = view.findViewById(R.id.galleryBtn);
        audioBtn = view.findViewById(R.id.audioBtn);
        clearBtn = view.findViewById(R.id.clearBtn);

        ivPreview = view.findViewById(R.id.ivPreview);
        vvPreview = view.findViewById(R.id.vvPreview);

        recordingLayout = view.findViewById(R.id.recordingLayout);
        playLayout = view.findViewById(R.id.playLayout);

        recordBtn = view.findViewById(R.id.recordBtn);
        playBtn = view.findViewById(R.id.playBtn);

        if (latParam != null && longParam != null)
            // gets location info from coordinates and sets button text
            setButtonText(HelperClass.getAddress(getContext(), latParam, longParam));

        setActivityLaunchers();
        setClickListeners();
    }


    private void setActivityLaunchers() {

        // assignment for google places search activity
        searchActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    // if result code is ok set the location for the new guide
                    if (result.getResultCode() == requireActivity().RESULT_OK) {

                        Place place = Autocomplete.getPlaceFromIntent(result.getData());
                        setLocation(place);
                    } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {

                        Status status = Autocomplete.getStatusFromIntent(result.getData());
                        Log.i(TAG, status.getStatusMessage());
                    } else if (result.getResultCode() == requireActivity().RESULT_CANCELED) {

                        if (getLocation() == null)

                            // tell the user they didn't select a location
                            Snackbar.make(addBtn, R.string.location_not_selected, Snackbar.LENGTH_SHORT).show();
                    }
                    return;

                });

        // set launcher for photo/video intent
        mediaActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    if (result.getResultCode() == getActivity().RESULT_OK) {

                        // if an image object was passed
                        assert result.getData() != null;
                        if (result.getData().getData() == null) {

                            // resize bitmap
                            Uri takenPhotoUri = Uri.fromFile(HelperClass.getMediaFileUri(photoFileName, Environment.DIRECTORY_PICTURES, requireContext()));

                            // updates value of photoFile
                            photoFile = HelperClass.getResizedImg(takenPhotoUri, getContext(), photoFileName, ivPreview, false);

                            // Bitmap takenImage = BitmapFactory.decodeFile(resizedFile.getAbsolutePath());
                            // sets other buttons to be not clickable
                            showImgView();

                        } else {
                            // adjust view states to be visible
                            vvPreview.setVisibility(View.VISIBLE);
                            ivPreview.setVisibility(View.GONE);

                            // sets other file to be null
                            photoFile = null;

                            // play recorded video
                            playbackRecordedVideo(result.getData().getData());
                        }
                    } else {
                        Snackbar noMedia = Snackbar.make(mediaBtn, "Media wasn't taken", Snackbar.LENGTH_SHORT);
                        HelperClass.displaySnackBarWithBottomMargin(noMedia, 50, getContext());

                        HelperClass.toggleButtonState(mediaBtn);
                        setBtnState(true);
                        clearMediaVariables();
                    }
                });

        // set launcher for gallery activity
        galleryActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    if (result.getData() != null) {

                        // gets image data from gallery
                        Uri photoUri = result.getData().getData();

                        photoFile = HelperClass.getResizedImg(photoUri, getContext(), photoFileName, ivPreview, false);

                        showImgView();
                    } else {
                        HelperClass.toggleButtonState(galleryBtn);
                        setBtnState(true);
                        clearMediaVariables();
                    }
                });
    }

    private void setClickListeners() {

        // button click listener to add new guide
        locationBtn.setOnClickListener(v -> {

            // Start the autocomplete intent.
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, HelperClass.placesFields)
                    .build(requireContext());

            searchActivityLauncher.launch(intent);
        });


        // add media button on click
        mediaBtn.setOnClickListener(v -> {

            HelperClass.toggleButtonState(mediaBtn);
            toggleMediaBtns(mediaBtn);

            // Create a File reference for future access
            photoFile = HelperClass.getMediaFileUri(photoFileName, Environment.DIRECTORY_PICTURES, requireContext());
            videoFile = getVideoFileUri(HelperClass.videoFileName);

            // wrap File object into a content provider
            // required for API >= 24
            // See https://guides.codepath.com/android/Sharing-Content-with-Intents#sharing-files-with-api-24-or-higher
            Uri photoUri = HelperClass.getUriForFile(requireContext(), photoFile);
            Uri videoUri = Uri.fromFile(videoFile);

            onPickMedia(photoUri, videoUri);
        });

        // gallery button on click
        galleryBtn.setOnClickListener(v -> {

            HelperClass.toggleButtonState(galleryBtn);
            toggleMediaBtns(galleryBtn);

            onPickPhoto();
        });

        // audio button on click
        audioBtn.setOnClickListener(v -> {
            toggleAudioView();
            toggleMediaBtns(audioBtn);

            if (audioBtn.isSelected())
                setupRecorder();
        });

        // audio button on click
        recordBtn.setOnClickListener(v -> {

            HelperClass.toggleButtonState(recordBtn);

            if (recordBtn.isSelected()) {
                // Start recording the audio
                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();

                } catch (IOException e) {
                    Log.e(TAG, "prepare() failed", e);
                }
            } else {

                Log.i(TAG, "recording stopped");

                if (mediaRecorder != null) {

                    // Stop recording the audio
                    mediaRecorder.stop();
                    mediaRecorder.reset();
                    mediaRecorder.release();

                    // can't record again
                    recordBtn.setClickable(false);

                    // variables to play audio
                    playerPos = 0;
                    mediaPlayer = new MediaPlayer();
                    // toggles button once audio is finished playing
                    mediaPlayer.setOnCompletionListener(mp -> HelperClass.toggleButtonState(playBtn));

                    playLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        playBtn.setOnClickListener(v -> {

            HelperClass.toggleButtonState(playBtn);
            // plays or pause audio based on button state
            if (mediaPlayer != null) {
                if (playBtn.isSelected()) {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    try {
                        mediaPlayer.reset();
                        Log.i(TAG, audioFile.getAbsolutePath());
                        mediaPlayer.setDataSource(audioFile.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mediaPlayer.seekTo(playerPos);
                    mediaPlayer.start();
                } else {
                    playerPos = mediaPlayer.getCurrentPosition();
                    mediaPlayer.pause();
                }
            }
        });

        // clears all the ui elements and associated variables
        clearBtn.setOnClickListener(v -> {
            mediaBtn.setSelected(false);
            galleryBtn.setSelected(false);

            if (audioBtn.isSelected()) {
                toggleAudioView();
            }

            // buttons are all clickable
            setBtnState(true);

            // media variables are null
            clearMediaVariables();

            ivPreview.setImageResource(0);
            ivPreview.setVisibility(View.GONE);
            vvPreview.setVisibility(View.GONE);
        });

        addBtn.setOnClickListener(v -> {
            String text = etText.getText().toString();

            // if the text field is empty
            if (text.isEmpty()) {
                Snackbar emptyText = Snackbar.make(etText, empty_text, Snackbar.LENGTH_SHORT);
                HelperClass.displaySnackBarWithBottomMargin(emptyText, 80, getActivity());
                return;
            }

            // if no location is selected
            if (location == null) {
                Snackbar emptyLocation = Snackbar.make(etText, R.string.no_location, Snackbar.LENGTH_SHORT);
                HelperClass.displaySnackBarWithBottomMargin(emptyLocation, 80, getActivity());
                return;
            }

            ParseUser user = ParseUser.getCurrentUser();
            saveGuide(text, user, photoFile, videoFile, audioFile);
        });

    }

    private void setupRecorder() {

        // Verify that the device has a mic first
        PackageManager packageManager = requireContext().getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {

            // no mic on device
            Snackbar emptyText = Snackbar.make(etText, R.string.no_recorder, Snackbar.LENGTH_SHORT);
            HelperClass.displaySnackBarWithBottomMargin(emptyText, 80, getActivity());

            // hide audio view
            toggleAudioView();
        } else {

            mediaRecorder = new MediaRecorder();

            // creates audio file in podcast directory
            audioFile = HelperClass.getMediaFileUri("audioguide.mp4", Environment.DIRECTORY_PODCASTS, requireContext());

            // Set the audio format and encoder
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
        }
    }

    private void toggleAudioView() {
        // shows recording view based on state of audio button
        if (!audioBtn.isSelected()) {
            audioBtn.setSelected(true);
            recordingLayout.setVisibility(View.VISIBLE);
        } else {
            audioBtn.setSelected(false);
            recordingLayout.setVisibility(View.GONE);
        }
    }

    // creates intent to create a new photo or video
    private void onPickMedia(Uri photoUri, Uri videoUri) {


        // intent to take a video
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
//                takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);

        Intent chooserIntent = HelperClass.getChooserIntent(HelperClass.getPhotoIntent(photoUri), takeVideoIntent, "Capture Image or Video");
        mediaActivityLauncher.launch(chooserIntent);
    }

    // Trigger gallery selection for a photo
    public void onPickPhoto() {

        // as long as intent isn't null
        // Bring up gallery to select a photo
        galleryActivityLauncher.launch(HelperClass.getGalleryIntent());
    }

    // creates new Travel guide and updates it to the database
    private void saveGuide(String text, ParseUser user, File photo, File video, File audio) {

        Guide guide = new Guide();
        guide.setAuthor(user);
        guide.setText(text);
        guide.setLocation(location);

        // sets the photo and video fields if they exist
        if (photo != null)
            guide.setPhoto(new ParseFile(photo));
        else if (video != null)
            guide.setVideo(new ParseFile(video));
        else if (audio != null) {

            // Save sound using input stream
            // ref: https://stackoverflow.com/questions/43350226/android-how-to-upload-an-audio-file-with-parse-sdk
            byte[] soundBytes;
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(Uri.fromFile(audio));
//                soundBytes = new byte[inputStream.available()];
                soundBytes = toByteArray(inputStream);
                guide.setAudio(new ParseFile("audio.mp4", soundBytes));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // uploads new guide in the background
        guide.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.i(TAG, "Error while saving tag", e);
                    return;
                }

                // clears guide and goes back to main fragment
                guide.setText("");
                ivPreview.setImageResource(0);
                controller = null;
                vvPreview.setVideoPath("");
                photoFile = new File("");
                videoFile = new File("");
                audioFile = new File("");

                requireActivity().onBackPressed();
            }
        });
    }

    private void showImgView() {

        // adjust view states to be visible
        ivPreview.setVisibility(View.VISIBLE);
        vvPreview.setVisibility(View.GONE);

        // sets other file to be null
        videoFile = null;
    }

//    // load image into preview ui element
//    private void loadImgIntoPreview(File photoFile) {
//
//        ivPreview.setImageResource(0);
//
//        // Load the taken image into a preview
//        Glide.with(requireContext())
//                .load(photoFile)
//
//                .into(ivPreview);
//    }

    // plays video that has been recorded
    public void playbackRecordedVideo(Uri videoUri) {

        vvPreview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

                        // sets the controller to be anchored to the video view
                        controller = new MediaController(getContext());
                        vvPreview.setMediaController(controller);
                        controller.setAnchorView(vvPreview);
                    }
                });
            }
        });

        vvPreview.setVideoURI(videoUri);
        vvPreview.requestFocus();
        vvPreview.start();
    }

    // sets location from Google place object and changes button text
    public void setLocation(Place newLocation) {
        location = newLocation.getLatLng();
        setButtonText(newLocation.getName());
    }

    // sets location from LatLNg object
    public void setLocation(LatLng newLocation) {
        longParam = newLocation.longitude;
        latParam = newLocation.latitude;
        location = newLocation;
    }

    // returns location of new guide
    public LatLng getLocation() {
        return location;
    }

    // sets the button text given String
    private void setButtonText(String newText) {
        locationBtn.setText(newText);
    }


    /*
     * Returns the File for a video stored on disk given the fileName
     * https://guides.codepath.com/android/Video-Playback-and-Recording
     */
    public File getVideoFileUri(String fileName) {
        return new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + fileName);
    }


    // makes selected button the only one clickable
    public void toggleMediaBtns(ImageButton selected) {

        setBtnState(false);
        selected.setClickable(true);
    }

    // changes the state of all buttons
    private void setBtnState(boolean state) {
        audioBtn.setClickable(state);
        mediaBtn.setClickable(state);
        galleryBtn.setClickable(state);
    }

    private void clearMediaVariables() {
        videoFile = null;
        audioFile = null;
        photoFile = null;

        mediaPlayer = null;
        mediaRecorder = null;
    }
}