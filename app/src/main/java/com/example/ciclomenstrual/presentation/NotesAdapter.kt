package com.example.ciclomenstrual.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ciclomenstrual.databinding.ItemNoteBinding
import com.example.ciclomenstrual.domain.model.Note

class NotesAdapter(
    private val onDelete: (Note) -> Unit,
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder =
        NoteViewHolder(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: Note) {
            binding.noteTextView.text = note.content
            binding.deleteButton.setOnClickListener { onDelete(note) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
            if (oldItem.id != 0L && newItem.id != 0L) oldItem.id == newItem.id else oldItem === newItem

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem == newItem
    }
}
