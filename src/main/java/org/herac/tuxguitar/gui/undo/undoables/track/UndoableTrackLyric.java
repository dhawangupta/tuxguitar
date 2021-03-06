package org.herac.tuxguitar.gui.undo.undoables.track;

import org.herac.tuxguitar.gui.TuxGuitar;
import org.herac.tuxguitar.gui.editors.tab.TGLyricImpl;
import org.herac.tuxguitar.gui.undo.CannotRedoException;
import org.herac.tuxguitar.gui.undo.CannotUndoException;
import org.herac.tuxguitar.gui.undo.UndoableEdit;
import org.herac.tuxguitar.gui.undo.undoables.UndoableCaretHelper;
import org.herac.tuxguitar.song.models.TGLyric;
import org.herac.tuxguitar.song.models.TGTrack;

public class UndoableTrackLyric implements UndoableEdit {
  public static UndoableTrackLyric startUndo(TGTrack track,
      int undoCaretPosition) {
    UndoableTrackLyric undoable = new UndoableTrackLyric();
    undoable.doAction = UNDO_ACTION;
    undoable.trackNumber = track.getNumber();
    undoable.undoCaret = new UndoableCaretHelper();
    undoable.undoCaretPosition = undoCaretPosition;
    undoable.undoLyric = track.getLyrics().clone();
    return undoable;
  }

  private int doAction;
  private UndoableCaretHelper redoCaret;
  private int redoCaretPosition;
  private TGLyric redoLyric;
  private int trackNumber;
  private UndoableCaretHelper undoCaret;
  private int undoCaretPosition;

  private TGLyric undoLyric;

  private UndoableTrackLyric() {
    this.undoLyric = new TGLyricImpl();
    this.redoLyric = new TGLyricImpl();
  }

  public boolean canRedo() {
    return (this.doAction == REDO_ACTION);
  }

  public boolean canUndo() {
    return (this.doAction == UNDO_ACTION);
  }

  public UndoableTrackLyric endUndo(TGTrack track, int redoCaretPosition) {
    this.redoCaret = new UndoableCaretHelper();
    this.redoCaretPosition = redoCaretPosition;
    this.redoLyric = track.getLyrics().clone();
    return this;
  }

  public void redo() throws CannotRedoException {
    if (!canRedo()) {
      throw new CannotRedoException();
    }
    TGTrack track = TuxGuitar.instance().getSongManager().getTrack(
        this.trackNumber);
    track.setLyrics(this.redoLyric.clone());
    TuxGuitar.instance().getLyricEditor().setCaretPosition(
        this.redoCaretPosition);
    TuxGuitar.instance().getLyricEditor().update();
    TuxGuitar.instance().updateCache(false);
    this.redoCaret.update();
    this.doAction = UNDO_ACTION;
  }

  public void undo() throws CannotUndoException {
    if (!canUndo()) {
      throw new CannotUndoException();
    }
    TGTrack track = TuxGuitar.instance().getSongManager().getTrack(
        this.trackNumber);
    track.setLyrics(this.undoLyric.clone());
    TuxGuitar.instance().getLyricEditor().setCaretPosition(
        this.undoCaretPosition);
    TuxGuitar.instance().getLyricEditor().update();
    TuxGuitar.instance().updateCache(false);
    this.undoCaret.update();
    this.doAction = REDO_ACTION;
  }

}
