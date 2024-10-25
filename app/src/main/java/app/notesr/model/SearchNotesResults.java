package app.notesr.model;

import java.io.Serializable;
import java.util.List;

public record SearchNotesResults(List<Long> results) implements Serializable {}
