package me.iwf.photopicker.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.iwf.photopicker.R;
import me.iwf.photopicker.entity.Photo;
import me.iwf.photopicker.entity.PhotoDirectory;
import me.iwf.photopicker.event.OnItemCheckListener;
import me.iwf.photopicker.event.OnPhotoClickListener;
import me.iwf.photopicker.utils.AndroidLifecycleUtils;
import me.iwf.photopicker.utils.MediaStoreHelper;

/**
 * Created by donglua on 15/5/31.
 */
public class PhotoGridAdapter extends SelectableAdapter<PhotoGridAdapter.PhotoViewHolder> {

  private LayoutInflater inflater;

  private OnItemCheckListener onItemCheckListener    = null;
  private OnPhotoClickListener onPhotoClickListener  = null;
  private View.OnClickListener onCameraClickListener = null;

  public final static int ITEM_TYPE_CAMERA = 100;
  public final static int ITEM_TYPE_PHOTO  = 101;
  private final static int COL_NUMBER_DEFAULT = 3;

  private boolean hasCamera = true;
  private boolean previewEnable = true;

  private int imageSize;
  private int columnNumber = COL_NUMBER_DEFAULT;


  public PhotoGridAdapter(Context context,List<PhotoDirectory> photoDirectories) {
    this.photoDirectories = photoDirectories;
    inflater = LayoutInflater.from(context);
    setColumnNumber(context, columnNumber);
  }

  public PhotoGridAdapter(Context context,List<PhotoDirectory> photoDirectories, ArrayList<String> orginalPhotos, int colNum) {
    this(context, photoDirectories);
    setColumnNumber(context, colNum);
    selectedPhotos = new ArrayList<>();
    if (orginalPhotos != null) selectedPhotos.addAll(orginalPhotos);
  }

  private void setColumnNumber(Context context, int columnNumber) {
    this.columnNumber = columnNumber;
    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    DisplayMetrics metrics = new DisplayMetrics();
    wm.getDefaultDisplay().getMetrics(metrics);
    int widthPixels = metrics.widthPixels;
    imageSize = widthPixels / columnNumber;
  }

  @Override public int getItemViewType(int position) {
    return (showCamera() && position == 0) ? ITEM_TYPE_CAMERA : ITEM_TYPE_PHOTO;
  }


  @Override public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = inflater.inflate(R.layout.__picker_item_photo, parent, false);
    PhotoViewHolder holder = new PhotoViewHolder(itemView);
    if (viewType == ITEM_TYPE_CAMERA) {
      holder.vSelected.setVisibility(View.GONE);
      holder.ivPhoto.setScaleType(ImageView.ScaleType.CENTER);

      holder.ivPhoto.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          if (onCameraClickListener != null) {
            onCameraClickListener.onClick(view);
          }
        }
      });
    }
    return holder;
  }


  @Override public void onBindViewHolder(final PhotoViewHolder holder, int position) {

    if (getItemViewType(position) == ITEM_TYPE_PHOTO) {

      List<Photo> photos = getCurrentPhotos();
      final Photo photo;

      if (showCamera()) {
        photo = photos.get(position - 1);
      } else {
        photo = photos.get(position);
      }

      boolean canLoadImage = AndroidLifecycleUtils.canLoadImage(holder.ivPhoto.getContext());
      if(canLoadImage) {
        Uri uri = Uri.fromFile(new File(photo.getPath()));
        ImageRequest request = ImageRequestBuilder
                .newBuilderWithSource(uri)
                .setResizeOptions(new ResizeOptions(imageSize, imageSize))
                .setAutoRotateEnabled(true)
                .build();
        PipelineDraweeController controller = (PipelineDraweeController) Fresco.newDraweeControllerBuilder()
                .setOldController(holder.ivPhoto.getController())
                .setImageRequest(request)
                .setAutoPlayAnimations(true)
                .build();
        holder.ivPhoto.setController(controller);
      }

      final boolean isChecked = isSelected(photo);

      holder.vSelected.setSelected(isChecked);
      holder.ivPhoto.setSelected(isChecked);

      holder.ivPhoto.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          if (onPhotoClickListener != null) {
            int pos = holder.getAdapterPosition();
            if (previewEnable) {
              onPhotoClickListener.onClick(view, pos, showCamera());
            } else {
              holder.vSelected.performClick();
            }
          }
        }
      });
      holder.vSelected.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          int pos = holder.getAdapterPosition();
          boolean isEnable = true;

          if (onItemCheckListener != null) {
            isEnable = onItemCheckListener.OnItemCheck(pos, photo, isChecked,
                getSelectedPhotos().size());
          }
          if (isEnable) {
            toggleSelection(photo);
            notifyItemChanged(pos);
          }
        }
      });

    } else {
      GenericDraweeHierarchyBuilder builder =
              new GenericDraweeHierarchyBuilder(holder.ivPhoto.getResources());
      GenericDraweeHierarchy hierarchy = builder
              .setPlaceholderImage(R.drawable.__picker_camera)
              .build();
      holder.ivPhoto.setHierarchy(hierarchy);
      holder.ivPhoto.setImageURI(Uri.EMPTY);
    }
  }


  @Override public int getItemCount() {
    int photosCount =
        photoDirectories.size() == 0 ? 0 : getCurrentPhotos().size();
    if (showCamera()) {
      return photosCount + 1;
    }
    return photosCount;
  }


  public static class PhotoViewHolder extends RecyclerView.ViewHolder {
    private SimpleDraweeView ivPhoto;
    private View vSelected;

    public PhotoViewHolder(View itemView) {
      super(itemView);
      ivPhoto   = (SimpleDraweeView) itemView.findViewById(R.id.iv_photo);
      vSelected = itemView.findViewById(R.id.v_selected);
    }
  }


  public void setOnItemCheckListener(OnItemCheckListener onItemCheckListener) {
    this.onItemCheckListener = onItemCheckListener;
  }


  public void setOnPhotoClickListener(OnPhotoClickListener onPhotoClickListener) {
    this.onPhotoClickListener = onPhotoClickListener;
  }


  public void setOnCameraClickListener(View.OnClickListener onCameraClickListener) {
    this.onCameraClickListener = onCameraClickListener;
  }


  public ArrayList<String> getSelectedPhotoPaths() {
    ArrayList<String> selectedPhotoPaths = new ArrayList<>(getSelectedItemCount());

    for (String photo : selectedPhotos) {
      selectedPhotoPaths.add(photo);
    }

    return selectedPhotoPaths;
  }


  public void setShowCamera(boolean hasCamera) {
    this.hasCamera = hasCamera;
  }

  public void setPreviewEnable(boolean previewEnable) {
    this.previewEnable = previewEnable;
  }

  public boolean showCamera() {
    return (hasCamera && currentDirectoryIndex == MediaStoreHelper.INDEX_ALL_PHOTOS);
  }

  @Override public void onViewRecycled(PhotoViewHolder holder) {
    super.onViewRecycled(holder);
  }
}
