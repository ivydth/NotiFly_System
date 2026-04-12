package com.example.notifly_system;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SeeAllActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "extra_category";

    // ── Views — matched to activity_star.xml IDs ──────────────────────────────

    private AppCompatImageView btnMenu;      // @+id/btnMenu
    private AppCompatImageView btnProfile;   // @+id/btnProfile
    private AppCompatImageView ivHome;       // @+id/ivHome
    private AppCompatImageView ivSearch;     // @+id/ivSearch
    private AppCompatImageView ivBell;       // @+id/ivBell

    private TextView     tvSectionLabel;    // @+id/tvSectionLabel
    private RecyclerView rvStarred;         // @+id/rvStarred
    private TextView     tvEmptyState;      // @+id/tvEmptyState

    private NotifListAdapter adapter;
    private String category = "All";

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_star);

        category = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (category == null) category = "All";

        bindViews();
        setupSectionLabel();
        setupRecyclerView();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void bindViews() {
        btnMenu        = findViewById(R.id.btnMenu);
        btnProfile     = findViewById(R.id.btnProfile);
        ivHome         = findViewById(R.id.ivHome);
        ivSearch       = findViewById(R.id.ivSearch);
        ivBell         = findViewById(R.id.ivBell);
        tvSectionLabel = findViewById(R.id.tvSectionLabel);
        rvStarred      = findViewById(R.id.rvStarred);
        tvEmptyState   = findViewById(R.id.tvEmptyState);
    }

    private void setupSectionLabel() {
        if (tvSectionLabel != null) {
            tvSectionLabel.setText(category.toUpperCase());
        }
    }

    private void setupRecyclerView() {
        adapter = new NotifListAdapter(new ArrayList<>(), item -> {
            if (item.isStarred) {
                NotificationStore.getInstance().unstar(item.id);
                item.isStarred = false;
            } else {
                NotificationStore.getInstance().star(item.id);
                item.isStarred = true;
            }
            // If on the Starred list, unstarring removes the item
            if (category.equalsIgnoreCase("Starred")) {
                refreshList();
            } else {
                adapter.notifyDataSetChanged();
            }
        });

        rvStarred.setLayoutManager(new LinearLayoutManager(this));
        rvStarred.setNestedScrollingEnabled(false);
        rvStarred.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, UserMenu.class));
            overridePendingTransition(0, 0);
        });

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, UserActivity.class));
            finish();
        });

        ivSearch.setOnClickListener(v -> {
            // TODO: search screen
        });

        ivBell.setOnClickListener(v ->
                startActivity(new Intent(this, NotifActivity1.class)));
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void refreshList() {
        List<NotificationItem> items;

        if (category.equalsIgnoreCase("All")) {
            items = NotificationStore.getInstance().getAll();
        } else if (category.equalsIgnoreCase("Starred")) {
            items = NotificationStore.getInstance().getStarred();
        } else {
            items = NotificationStore.getInstance().getByCategory(category);
        }

        adapter.updateData(items);

        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private static class NotifListAdapter
            extends RecyclerView.Adapter<NotifListAdapter.VH> {

        interface OnStarClickListener {
            void onStarClick(NotificationItem item);
        }

        private final List<NotificationItem> data;
        private final OnStarClickListener    starListener;

        NotifListAdapter(List<NotificationItem> data, OnStarClickListener listener) {
            this.data         = data;
            this.starListener = listener;
        }

        void updateData(List<NotificationItem> newData) {
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            NotificationItem item = data.get(position);

            // Bind to item_notification_row.xml IDs:
            // ivAvatar, tvSenderName, tvMessage, tvDate, tvStar, vDivider
            h.avatar.setBackgroundResource(item.avatarResId);
            h.tvName.setText(item.senderName);
            h.tvMessage.setText(item.message);
            h.tvDate.setText(item.dateLabel);

            applyStarColor(h.tvStar, item.isStarred);

            h.tvStar.setOnClickListener(v -> {
                if (starListener != null) starListener.onStarClick(item);
                applyStarColor(h.tvStar, item.isStarred);
            });

            // Hide divider on the last row
            if (h.divider != null) {
                h.divider.setVisibility(
                        position < data.size() - 1 ? View.VISIBLE : View.GONE);
            }
        }

        private void applyStarColor(TextView tvStar, boolean starred) {
            tvStar.setTextColor(starred
                    ? Color.parseColor("#FFB347")
                    : Color.parseColor("#44AACCDD"));
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            View     avatar;    // R.id.ivAvatar
            TextView tvName;    // R.id.tvSenderName
            TextView tvMessage; // R.id.tvMessage
            TextView tvDate;    // R.id.tvDate
            TextView tvStar;    // R.id.tvStar
            View     divider;   // R.id.vDivider

            VH(@NonNull View itemView) {
                super(itemView);
                avatar    = itemView.findViewById(R.id.ivAvatar);
                tvName    = itemView.findViewById(R.id.tvSenderName);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvDate    = itemView.findViewById(R.id.tvDate);
                tvStar    = itemView.findViewById(R.id.tvStar);
                divider   = itemView.findViewById(R.id.vDivider);
            }
        }
    }
}
