package app.notesr.activity.file;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import app.notesr.ElementListAdapter;
import app.notesr.R;
import app.notesr.data.model.FileInfo;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FilesListAdapter extends ElementListAdapter<FileInfo> {
    private static final Map<String, Integer> FILES_TYPES_ICONS = Map.of(
            "text", R.drawable.text_file,
            "image", R.drawable.image_file,
            "video", R.drawable.video_file,
            "audio", R.drawable.audio_file
    );

    private static String toReadableSize(long size) {
        String[] units = new String[] {"B", "KB", "MB", "GB", "TB", "PB", "EB"};

        if (size > 0) {
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            return new DecimalFormat("#,##0.#")
                    .format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
        }

        return "0 B";
    }

    public FilesListAdapter(@NonNull Context context, int resource, @NonNull List<FileInfo> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater inflater;
            inflater = LayoutInflater.from(context);
            view = inflater.inflate(resourceLayout, null);
        }

        FileInfo fileInfo = getItem(position);

        if (fileInfo != null) {
            TextView nameView = view.findViewById(R.id.fileNameTextView);
            TextView sizeView = view.findViewById(R.id.fileSizeTextView);
            TextView updateAtView = view.findViewById(R.id.fileUpdatedAtTextView);

            ImageView iconView = view.findViewById(R.id.fileIconImageView);

            nameView.setText(fileInfo.getName());
            sizeView.setText(toReadableSize(fileInfo.getSize()));
            updateAtView.setText(fileInfo.getUpdatedAt().format(TIMESTAMP_FORMATTER));

            setFileIcon(fileInfo, iconView);
        }

        return view;
    }

    private void setFileIcon(FileInfo fileInfo, ImageView view) {
        byte[] thumbnail = fileInfo.getThumbnail();

        if (thumbnail != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);

            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            view.setBackgroundColor(Color.BLACK);
            view.setImageBitmap(bitmap);

            return;
        }

        if (fileInfo.getType() != null) {
            String type = fileInfo.getType().split("/")[0];

            if (FILES_TYPES_ICONS.containsKey(type)) {
                view.setImageResource(FILES_TYPES_ICONS.get(type));
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return Objects.requireNonNull(getItem(position)).getDecimalId();
    }
}
