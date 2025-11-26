package com.example.ciclomenstrual;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ciclomenstrual.database.CycleRepository;
import com.example.ciclomenstrual.database.Note;

import java.util.Calendar;
import java.util.List;

public class MainViewModel extends AndroidViewModel {

    private final CycleRepository repository;
    private final MutableLiveData<List<Cycle>> cycles = new MutableLiveData<>();
    private final MutableLiveData<List<Note>> notes = new MutableLiveData<>();
    private final MutableLiveData<Calendar> predictedNextCycle = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new CycleRepository(application);
        loadData();
    }

    public LiveData<List<Cycle>> getCycles() {
        return cycles;
    }

    public LiveData<List<Note>> getNotes() {
        return notes;
    }

    public LiveData<Calendar> getPredictedNextCycle() {
        return predictedNextCycle;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadData() {
        repository.getAllCycles(result -> {
            cycles.postValue(result);
            updatePrediction(result);
        });
        repository.getAllNotes(notes::postValue);
    }

    public void addCycle(Cycle cycle) {
        repository.insertCycle(cycle, this::loadData);
    }

    public void updateCycle(Cycle cycle) {
        repository.updateCycle(cycle, this::loadData);
    }

    public void deleteCycle(Cycle cycle) {
        repository.deleteCycle(cycle, this::loadData);
    }

    public void addNote(Calendar date, String content) {
        Note note = new Note(date.getTimeInMillis(), content);
        repository.insertNote(note, this::loadData);
    }

    public void deleteNote(Calendar date, String content) {
        repository.deleteNote(date.getTimeInMillis(), content, this::loadData);
    }

    private void updatePrediction(List<Cycle> cycleList) {
        if (cycleList == null || cycleList.isEmpty()) {
            predictedNextCycle.postValue(null);
            return;
        }

        // Find last complete cycle to base prediction on
        Cycle lastComplete = null;
        for (int i = cycleList.size() - 1; i >= 0; i--) {
            Cycle c = cycleList.get(i);
            if (c.getEndDate() != null) {
                lastComplete = c;
                break;
            }
        }

        if (lastComplete != null) {
            Calendar prediction = CycleCalculator.predictNextCycleStart(lastComplete, 28); // Default 28 days
            predictedNextCycle.postValue(prediction);
        } else {
            predictedNextCycle.postValue(null);
        }
    }
}
