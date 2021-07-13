package com.example.travelguide.fragments;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.travelguide.R;
import com.example.travelguide.activities.MapsActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ComposeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ComposeFragment extends Fragment {

    private static final String TAG = "ComposeFragment";
    private static final String ARG_LONG = "longitude";
    private static final String ARG_LAT = "latitude";

    // TODO: Rename and change types of parameters
    private Double longParam;
    private Double latParam;

    // default location
    private LatLng location = new LatLng(0, 0);

    private EditText etText;
    private Button locationBtn;

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
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        locationBtn = view.findViewById(R.id.locationBtn);
        etText = view.findViewById(R.id.etText);

        // gets location info from coordinates and sets button text
        List<Address> likelyNames = new ArrayList<>();
        Geocoder geocoder = new Geocoder(getContext());
        try {
            likelyNames = geocoder.getFromLocation(latParam, longParam, 1);
            setButtonText(likelyNames.get(0).getAddressLine(0));

        } catch (IOException e) {
            Log.i(TAG, e.getMessage());
        }

        // button click listener to add new guide
        locationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the fields to specify which types of place data to return
                List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.PHOTO_METADATAS);

                // Start the autocomplete intent.
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                        .build(getContext());
                // ensure that the request code is the one defined in MapsActivity
                getActivity().startActivityForResult(intent, MapsActivity.AUTOCOMPLETE_REQUEST_CODE);
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_compose, container, false);
    }


    // sets location from Google place objecct and changes button text
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

    private void setButtonText(String newText) {
        locationBtn.setText(newText);
    }


}