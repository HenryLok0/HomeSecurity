package ict.mgame.homesecurity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HistoryFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Reuse existing layout or create new one. For now using a placeholder if specific layout not ready
        // Assuming activity_history.xml exists and can be adapted, but for safety using a simple view
        // In a real migration, we would inflate R.layout.activity_history here
        return inflater.inflate(R.layout.activity_history, container, false);
    }
}
