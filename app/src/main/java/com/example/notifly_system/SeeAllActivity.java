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

public class SeeAllActivity extends AppCompatActivity
        implements NotificationStore.StoreListener {

    public static final String EXTRA_CATEGORY = "extra_category";

    private AppCompatImageView btnMenu;
    private AppCompatImageView btnProfile;
    private AppCompatImageView ivHome;
    private AppCompatImageView ivSearch;
    private AppCompatImageView ivBell;

    private TextView     tvSectionLabel;
    private RecyclerView rvStarred;
    private TextView     tvEmptyState;

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
        NotificationStore.getInstance().addListener(this);
        refreshList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NotificationStore.getInstance().removeListener(this);
    }

    // ── StoreListener ─────────────────────────────────────────────────────────

    @Override
    public void onStoreChanged() {
        runOnUiThread(this::refreshList);
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
        if (tvSectionLabel != null) tvSectionLabel.setText(category.toUpperCase());
    }

    private void setupRecyclerView() {
        adapter = new NotifListAdapter(
                new ArrayList<>(),
                // Star toggle
                item -> {
                    if (item.isStarred) {
                        NotificationStore.getInstance().unstar(item.id);
                        item.isStarred = false;
                    } else {
                        NotificationStore.getInstance().star(item.id);
                        item.isStarred = true;
                    }
                },
                // Row tap → open NotifActivity
                item -> {
                    Intent intent = new Intent(this, NotifActivity.class);
                    intent.putExtra(NotifActivity.EXTRA_NOTIF_ID, item.id);
                    startActivity(intent);
                }
        );

        rvStarred.setLayoutManager(new LinearLayoutManager(this));
        rvStarred.setNestedScrollingEnabled(false);
        rvStarred.setAdapter(adapter);
    }

    private void setupClickListeners() {
        if (btnMenu    != null) btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, UserMenu.class));
            overridePendingTransition(0, 0);
        });

        if (btnProfile != null) btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        if (ivHome     != null) ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, UserActivity.class));
            finish();
        });

        if (ivSearch   != null) ivSearch.setOnClickListener(v -> { /* TODO */ });

        if (ivBell     != null) ivBell.setOnClickListener(v ->
                startActivity(new Intent(this, NotifActivity1.class)));
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void refreshList() {
        List<NotificationItem> items;
        NotificationStore store = NotificationStore.getInstance();

        if (category.equalsIgnoreCase("All")) {
            items = store.getAll();
        } else if (category.equalsIgnoreCase("Starred")) {
            items = store.getStarred();
        } else if (category.equalsIgnoreCase("Unread")) {
            items = store.getUnread();
        } else {
            items = store.getByCategory(category);
        }

        adapter.updateData(items);

        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private static class NotifListAdapter
            extends RecyclerView.Adapter<NotifListAdapter.VH> {

        interface OnStarClickListener { void onStarClick(NotificationItem item); }
        interface OnRowClickListener  { void onRowClick(NotificationItem item);  }

        private final List<NotificationItem> data;
        private final OnStarClickListener    starListener;
        private final OnRowClickListener     rowListener;

        NotifListAdapter(List<NotificationItem> data,
                         OnStarClickListener starListener,
                         OnRowClickListener  rowListener) {
            this.data         = data;
            this.starListener = starListener;
            this.rowListener  = rowListener;
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

            // ✅ Null-safe avatar — same fix as ArcActivity
            if (h.avatar != null && item.avatarResId != 0) {
                try {
                    h.avatar.setBackgroundResource(item.avatarResId);
                } catch (Exception e) {
                    h.avatar.setBackgroundResource(R.drawable.avatar_teal);
                }
            }

            if (h.tvName    != null) h.tvName.setText(item.senderName);
            if (h.tvMessage != null) h.tvMessage.setText(item.message);
            if (h.tvDate    != null) h.tvDate.setText(item.dateLabel);

            // Dim already-read items
            if (h.tvName != null && h.tvMessage != null) {
                if (item.isRead) {
                    h.tvName.setTextColor(Color.parseColor("#668899"));
                    h.tvMessage.setTextColor(Color.parseColor("#446677"));
                } else {
                    h.tvName.setTextColor(Color.WHITE);
                    h.tvMessage.setTextColor(Color.parseColor("#AACCDD"));
                }
            }

            if (h.tvStar != null) {
                applyStarColor(h.tvStar, item.isStarred);

                // Star tap — must not also trigger row tap
                h.tvStar.setOnClickListener(v -> {
                    if (starListener != null) starListener.onStarClick(item);
                    applyStarColor(h.tvStar, item.isStarred);
                });
            }

            // Row tap → open NotifActivity
            h.itemView.setOnClickListener(v -> {
                if (rowListener != null) rowListener.onRowClick(item);
            });

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
            View     avatar;
            TextView tvName;
            TextView tvMessage;
            TextView tvDate;
            TextView tvStar;
            View     divider;

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
