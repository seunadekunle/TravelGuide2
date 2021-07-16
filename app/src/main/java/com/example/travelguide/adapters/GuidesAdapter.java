package com.example.travelguide.adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.travelguide.R;
import com.example.travelguide.activities.MapsActivity;
import com.example.travelguide.classes.Guide;
import com.example.travelguide.databinding.LocationGuideBinding;
import com.example.travelguide.helpers.HelperClass;
import com.parse.ParseFile;

import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Guide}.
 * TODO: Replace the implementation with code for your data type.
 */
public class GuidesAdapter extends RecyclerView.Adapter<GuidesAdapter.ViewHolder> {

    private static final String TAG = "GuidesAdapter";

    private List<Guide> guides;
    private Context context;
    private ImageView expandedImageView;
    private View expandedImageViewBG;
    private Activity activity;

    // Hold a reference to the current animator
    private Animator currentAnimator;
    // The system "short" animation time duration, in milliseconds.
    private int shortAnimationDuration;

    public GuidesAdapter(List<Guide> items, Context context, ImageView expandedImageView, View expandedImageViewBG, Activity activity) {
        this.context = context;
        this.expandedImageView = expandedImageView;
        guides = items;
        this.expandedImageViewBG = expandedImageViewBG;
        this.activity = activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(LocationGuideBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        // fills ui elements with information from the guide
        Guide guide = guides.get(position);

        holder.tvUsername.setText(guide.getAuthor().getUsername());
        holder.tvDetail.setText(guide.getText());

        if (guide.getPhoto() != null) {

            String photoUrl = guide.getPhoto().getUrl();
            Glide.with(context)
                    .load(photoUrl).centerCrop().override(HelperClass.detailImgDimen, HelperClass.detailImgDimen)
                    .transform(new RoundedCornersTransformation(HelperClass.picRadius, 0)).into(holder.ibThumb);

            holder.ibThumb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // hide add button and zoom image
                    ((MapsActivity) activity).hideAddBtn();
                    zoomImageFromThumb(holder.ibThumb, photoUrl);
                }
            });
        }

        // loads user profile image on timeline
        ParseFile profileImg = guide.getAuthor().getParseFile("avatar");
        // handles if image gotten from database
        if (profileImg != null)
            Glide.with(context)
                    .load(profileImg.getUrl()).fitCenter().transform(new CircleCrop())
                    .override(100, 40).into(holder.ivAvatar);
        else
            (holder.ivAvatar).setVisibility(View.GONE);
    }

    // clear all elements of the RecyclerView
    public void clear() {
        guides.clear();
        notifyDataSetChanged();
    }

    // Add a list of items to the list
    public void addAll(List<Guide> list) {
        guides.addAll(list);
        notifyDataSetChanged();
    }

    // return size of lists for the lcoation
    @Override
    public int getItemCount() {
        return guides.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvUsername;
        public TextView tvDetail;
        public ImageView ivAvatar;

        private ConstraintLayout mediaLayout;
        private ImageButton ibThumb;

        public ViewHolder(LocationGuideBinding binding) {
            super(binding.getRoot());

            // binds ui elements to variables
            tvUsername = binding.tvUsername;
            tvDetail = binding.tvDetail;
            ivAvatar = binding.ivAvatar;

            mediaLayout = binding.mediaContainer.mediaLayout;
            ibThumb = binding.mediaContainer.ibThumb;
        }
    }


    /* creates an expanded view after clicking on thumbnail
     * ref: https://developer.android.com/training/animation/zoom.html
     */
    private void zoomImageFromThumb(final View thumbView, String imgUrl) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        Glide.with(context).load(imgUrl).into(expandedImageView);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        expandedImageView.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView,
                        View.SCALE_Y, startScale, 1f));
        set.setDuration(shortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimator = null;

                // shows background
                expandedImageViewBG.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                currentAnimator = null;
            }
        });
        set.start();
        currentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentAnimator != null) {
                    currentAnimator.cancel();
                }

                // removes background
                expandedImageViewBG.setVisibility(View.GONE);

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y, startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(shortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        currentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        currentAnimator = null;
                    }
                });
                set.start();
                currentAnimator = set;

                // show add button
                ((MapsActivity) activity).showAddBtn();
            }
        });
    }
}