package com.example.kelimeoyunu.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kelimeoyunu.R;
import com.example.kelimeoyunu.models.JokerItem;

import java.util.List;

public class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.JokerViewHolder> {

    private List<JokerItem> jokerList;
    private OnJokerBuyClickListener buyClickListener;

    public interface OnJokerBuyClickListener {
        void onBuyClick(JokerItem joker);
    }

    public MarketAdapter(List<JokerItem> jokerList, OnJokerBuyClickListener listener) {
        this.jokerList = jokerList;
        this.buyClickListener = listener;
    }

    @NonNull
    @Override
    public JokerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_joker, parent, false);
        return new JokerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JokerViewHolder holder, int position) {
        JokerItem joker = jokerList.get(position);

        holder.tvName.setText(joker.name);
        holder.tvDesc.setText(joker.description);
        holder.tvIcon.setText(joker.icon);
        holder.btnBuy.setText(joker.price + " 💰");

        // YENİ ROZET MANTIĞI: Miktar 0'dan büyükse göster, değilse gizle
        if (joker.ownedQuantity > 0) {
            holder.cvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(String.valueOf(joker.ownedQuantity));
        } else {
            holder.cvBadge.setVisibility(View.GONE);
        }

        holder.btnBuy.setOnClickListener(v -> buyClickListener.onBuyClick(joker));
    }

    @Override
    public int getItemCount() {
        return jokerList.size();
    }

    static class JokerViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvIcon, tvBadge;
        Button btnBuy;
        CardView cvBadge;

        public JokerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvJokerName);
            tvDesc = itemView.findViewById(R.id.tvJokerDesc);
            tvIcon = itemView.findViewById(R.id.tvJokerIcon);
            btnBuy = itemView.findViewById(R.id.btnBuyJoker);

            // Rozet tanımlamaları
            tvBadge = itemView.findViewById(R.id.tvJokerBadge);
            cvBadge = itemView.findViewById(R.id.cvBadge);
        }
    }
}