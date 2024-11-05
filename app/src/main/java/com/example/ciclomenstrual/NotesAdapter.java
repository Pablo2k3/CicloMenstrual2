package com.example.ciclomenstrual;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<String> notes;
    private final OnNoteDeletedListener onNoteDeletedListener; // Listener para la eliminación de notas
    private RecyclerView.ViewHolder holder;
    public NotesAdapter(List<String> notes, OnNoteDeletedListener listener) {
        this.notes = notes;
        this.onNoteDeletedListener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        this.holder = holder;
        String note = notes.get(position);
        holder.noteTextView.setText(note);

        // Configurar el listener para el botón de eliminar
        holder.deleteButton.setOnClickListener(v -> {
            if (onNoteDeletedListener != null) {
                onNoteDeletedListener.onNoteDeleted(position); // Notificar la eliminación
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void updateNotes(List<String> newNotes) {
        this.notes = new ArrayList<>(newNotes != null ? newNotes : new ArrayList<>());
        notifyDataSetChanged();
    }

    // Interfaz para la eliminación de notas
    public interface OnNoteDeletedListener {
        void onNoteDeleted(int position);
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTextView;
        ImageButton deleteButton;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTextView = itemView.findViewById(R.id.noteTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}