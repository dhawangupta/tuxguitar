package org.herac.tuxguitar.io.tef;

import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.herac.tuxguitar.gui.editors.tab.TGBeatImpl;
import org.herac.tuxguitar.gui.editors.tab.TGChordImpl;
import org.herac.tuxguitar.gui.editors.tab.TGNoteImpl;
import org.herac.tuxguitar.io.base.TGFileFormat;
import org.herac.tuxguitar.io.base.TGFileFormatException;
import org.herac.tuxguitar.io.base.TGLocalFileImporter;
import org.herac.tuxguitar.io.tef.base.TEChord;
import org.herac.tuxguitar.io.tef.base.TEComponent;
import org.herac.tuxguitar.io.tef.base.TEComponentChord;
import org.herac.tuxguitar.io.tef.base.TEComponentNote;
import org.herac.tuxguitar.io.tef.base.TESong;
import org.herac.tuxguitar.io.tef.base.TETimeSignature;
import org.herac.tuxguitar.io.tef.base.TETrack;
import org.herac.tuxguitar.song.managers.TGSongManager;
import org.herac.tuxguitar.song.models.TGBeat;
import org.herac.tuxguitar.song.models.TGChannel;
import org.herac.tuxguitar.song.models.TGChord;
import org.herac.tuxguitar.song.models.TGDivisionType;
import org.herac.tuxguitar.song.models.TGDuration;
import org.herac.tuxguitar.song.models.TGMeasure;
import org.herac.tuxguitar.song.models.TGMeasureHeader;
import org.herac.tuxguitar.song.models.TGNote;
import org.herac.tuxguitar.song.models.TGSong;
import org.herac.tuxguitar.song.models.TGString;
import org.herac.tuxguitar.song.models.TGTimeSignature;
import org.herac.tuxguitar.song.models.TGTrack;

public class TESongImporter implements TGLocalFileImporter {

  /** The Logger for this class. */
  public static final transient Logger LOG = Logger
      .getLogger(TESongImporter.class);

  private static final int[][] PERCUSSION_TUNINGS = new int[][] {
      new int[] { 49, 41, 32 }, new int[] { 49, 51, 42, 50 },
      new int[] { 49, 42, 50, 37, 32 }, new int[] { 49, 51, 42, 50, 45, 37 },
      new int[] { 49, 51, 42, 50, 45, 37, 41 }, };

  protected TGSongManager manager;
  protected InputStream stream;

  public TESongImporter() {
    super();
  }

  private void addChord(TEChord[] chords, TEComponentChord component,
      TGTrack tgTrack, TGMeasure tgMeasure) {
    if (component.getChord() >= 0 && component.getChord() < chords.length) {
      TEChord chord = chords[component.getChord()];
      byte[] strings = chord.getStrings();

      TGChord tgChord = new TGChordImpl(tgTrack.stringCount());
      tgChord.setName(chord.getName());
      for (int i = 0; i < tgChord.countStrings(); i++) {
        int value = ((i < strings.length) ? strings[i] : -1);
        tgChord.addFretValue(i, value);
      }
      if (tgChord.countNotes() > 0) {
        TGBeat tgBeat = getBeat(tgMeasure, getStart(null, tgMeasure, component
            .getPosition()));
        tgBeat.setChord(tgChord);
      }
    }
  }

  private void addComponents(TESong song) {
    for (final TEComponent component : song.getComponents()) {

      if (component.getMeasure() >= 0
          && component.getMeasure() < this.manager.getSong()
              .countMeasureHeaders()) {
        int offset = 0;
        TETrack[] tracks = song.getTracks();
        for (int i = 0; i < tracks.length; i++) {
          int strings = tracks[i].getStrings().length;
          int string = (component.getString() - offset);
          if (string >= 0 && string < strings && string < 7) {
            TGTrack tgTrack = this.manager.getSong().getTrack(i);
            TGMeasure tgMeasure = tgTrack.getMeasure(component.getMeasure());
            if (component instanceof TEComponentNote) {
              addNote(tracks[i], (TEComponentNote) component, string, strings,
                  tgMeasure);
            } else if (component instanceof TEComponentChord) {
              addChord(song.getChords(), (TEComponentChord) component, tgTrack,
                  tgMeasure);
            }
          }
          offset += strings;
        }
      }
    }
  }

  private void addMeasureValues(TESong song) {
    TGTimeSignature timeSignature = new TGTimeSignature();
    for (int i = 0; i < this.manager.getSong().countMeasureHeaders(); i++) {
      TGMeasureHeader header = this.manager.getSong().getMeasureHeader(i);
      TETimeSignature ts = song.getTimeSignature(i);
      timeSignature.setNumerator(ts.getNumerator());
      timeSignature.getDenominator().setValue(ts.getDenominator());
      this.manager.changeTimeSignature(header, timeSignature, false);
    }
  }

  private void addNote(TETrack track, TEComponentNote note, int string,
      int strings, TGMeasure tgMeasure) {
    int value = note.getFret();
    if (track.isPercussion()) {
      int tuning = (Math.min((strings - 2), (PERCUSSION_TUNINGS.length)) - 1);
      if (string >= 0 && string < PERCUSSION_TUNINGS[tuning].length) {
        value += PERCUSSION_TUNINGS[tuning][string];
      }
    }

    TGNote tgNote = new TGNoteImpl();
    tgNote.setString(string + 1);
    tgNote.setValue(value);

    TGDuration tgDuration = getDuration(note.getDuration());
    TGBeat tgBeat = getBeat(tgMeasure, getStart(tgDuration, tgMeasure, note
        .getPosition()));
    tgBeat.getVoice(0).setDuration(tgDuration.clone());
    tgBeat.getVoice(0).addNote(tgNote);
  }

  private void addTrackValues(TETrack[] tracks) {
    for (int i = 0; i < tracks.length; i++) {
      TGTrack track = this.manager.getSong().getTrack(i);
      track.getChannel().setVolume(
          (short) (((15 - tracks[i].getVolume()) * 127) / 15));
      track.getChannel().setBalance((short) ((tracks[i].getPan() * 127) / 15));
      track.getChannel().setInstrument((short) tracks[i].getInstrument());
      if (tracks[i].isPercussion()) {
        TGChannel.setPercussionChannel(track.getChannel());
      }
      track.getStrings().clear();
      int strings[] = tracks[i].getStrings();

      for (int j = 0; j < strings.length; j++) {
        if (j >= 7) {
          break;
        }
        track.getStrings().add(
            new TGString(j + 1, tracks[i].isPercussion() ? 0
                : (96 - strings[j])));
      }
    }
  }

  public boolean configure(boolean setDefaults) {
    return true;
  }

  private TGBeat getBeat(TGMeasure measure, long start) {
    TGBeat beat = this.manager.getMeasureManager().getBeat(measure, start);
    if (beat == null) {
      beat = new TGBeatImpl();
      beat.setStart(start);
      measure.addBeat(beat);
    }
    return beat;
  }

  private TGDuration getDuration(int duration) {
    TGDuration tgDuration = new TGDuration();

    int value = TGDuration.WHOLE;
    for (int i = 0; i < (duration / 3); i++) {
      value = (value * 2);
    }
    if ((duration % 3) == 1) {
      value = (value * 2);
      tgDuration.setDotted(true);
    } else if ((duration % 3) == 2) {
      tgDuration.setDivision(TGDivisionType.DEFAULT);
    }
    tgDuration.setValue(value);

    return tgDuration;
  }

  public TGFileFormat getFileFormat() {
    return new TGFileFormat("Tef", "*.tef");
  }

  public String getImportName() {
    return "Tef";
  }

  private long getStart(TGDuration duration, TGMeasure measure, int position) {
    float fixedPosition = position;
    if (duration != null
        && !duration.getDivision().isEqual(TGDivisionType.NORMAL)) {
      fixedPosition = ((fixedPosition - (fixedPosition % 64)) + ((((fixedPosition % 64) * 2) * 2) / 3));
    }
    long start = ((long) (measure.getStart() + ((fixedPosition * TGDuration.QUARTER_TIME) / 64)));

    return start;
  }

  public TGSong importSong() throws TGFileFormatException {
    try {
      if (this.manager != null && this.stream != null) {
        return this.parseSong(new TEInputStream(this.stream).readSong());
      }
    } catch (Exception e) {
      LOG.error(e);
    }
    throw new TGFileFormatException();
  }

  public void init(InputStream stream) {
    this.manager = new TGSongManager();
    this.stream = stream;
  }

  private void newTGSong(int tracks, int measures, int tempo) {
    this.manager.setSong(this.manager.newSong());
    this.manager.getFirstMeasureHeader().getTempo().setValue(tempo);

    while (this.manager.getSong().countTracks() < tracks) {
      this.manager.createTrack();
    }
    while (this.manager.getSong().countMeasureHeaders() < measures) {
      this.manager.addNewMeasureBeforeEnd();
    }
  }

  private TGSong parseSong(TESong song) {
    this.sortComponents(song);
    this.newTGSong(song.getTracks().length, song.getMeasures(), song.getTempo()
        .getValue());
    this.addMeasureValues(song);
    this.addTrackValues(song.getTracks());
    this.addComponents(song);

    return new TGSongAdjuster(this.manager).process();
  }

  public void sortComponents(TESong song) {
    Collections.sort(song.getComponents(), new Comparator<TEComponent>() {
      public int compare(TEComponent c1, TEComponent c2) {

        if (c1.getMeasure() < c2.getMeasure()) {
          return -1;
        }
        if (c1.getMeasure() > c2.getMeasure()) {
          return 1;
        }
        if (c1.getPosition() < c2.getPosition()) {
          return -1;
        }
        if (c1.getPosition() > c2.getPosition()) {
          return 1;
        }
        if ((c1 instanceof TEComponentNote) && !(c2 instanceof TEComponentNote)) {
          return -1;
        }
        if ((c2 instanceof TEComponentNote) && !(c1 instanceof TEComponentNote)) {
          return 1;
        }
        return 0;
      }
    });
  }
}

class TGSongAdjuster {

  protected TGSongManager manager;

  public TGSongAdjuster(TGSongManager manager) {
    this.manager = manager;
  }

  public void adjustBeats(TGMeasure measure) {
    TGBeat previous = null;
    boolean finish = true;

    long measureStart = measure.getStart();
    long measureEnd = (measureStart + measure.getLength());
    for (int i = 0; i < measure.countBeats(); i++) {
      TGBeat beat = measure.getBeat(i);
      long beatStart = beat.getStart();
      long beatLength = beat.getVoice(0).getDuration().getTime();
      if (previous != null) {
        long previousStart = previous.getStart();
        long previousLength = previous.getVoice(0).getDuration().getTime();

        // check for a chord in a rest beat
        if (beat.getVoice(0).isRestVoice() && beat.isChordBeat()) {
          TGBeat candidate = null;
          TGBeat next = this.manager.getMeasureManager().getFirstBeat(
              measure.getBeats());
          while (next != null) {
            if (candidate != null && next.getStart() > beat.getStart()) {
              break;
            }
            if (!next.getVoice(0).isRestVoice() && !next.isChordBeat()) {
              candidate = next;
            }
            next = this.manager.getMeasureManager().getNextBeat(
                measure.getBeats(), next);
          }
          if (candidate != null) {
            candidate.setChord(beat.getChord());
          }
          measure.removeBeat(beat);
          finish = false;
          break;
        }

        // check the duration
        if (previousStart < beatStart
            && (previousStart + previousLength) > beatStart) {
          if (beat.getVoice(0).isRestVoice()) {
            measure.removeBeat(beat);
            finish = false;
            break;
          }
          TGDuration duration = TGDuration
              .fromTime((beatStart - previousStart));
          previous.getVoice(0).setDuration(duration.clone());
        }
      }
      if ((beatStart + beatLength) > measureEnd) {
        if (beat.getVoice(0).isRestVoice()) {
          measure.removeBeat(beat);
          finish = false;
          break;
        }
        TGDuration duration = TGDuration.fromTime((measureEnd - beatStart));
        beat.getVoice(0).setDuration(duration.clone());
      }
      previous = beat;
    }
    if (!finish) {
      adjustBeats(measure);
    }
  }

  public TGSong process() {
    for (final TGTrack track : this.manager.getSong().getTracks()) {
      for (final TGMeasure measure : track.getMeasures()) {
        this.process(measure);
      }
    }
    return this.manager.getSong();
  }

  public void process(TGMeasure measure) {
    this.manager.getMeasureManager().orderBeats(measure);
    this.adjustBeats(measure);
  }
}
