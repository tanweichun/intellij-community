package com.intellij.localvcs.core.changes;

import com.intellij.localvcs.core.Reversed;
import com.intellij.localvcs.core.storage.Content;
import com.intellij.localvcs.core.storage.Stream;
import com.intellij.localvcs.core.tree.Entry;
import com.intellij.localvcs.core.tree.RootEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChangeList {
  // todo hold changes in reverse order
  private List<Change> myChanges = new ArrayList<Change>();

  public ChangeList() {
  }

  public ChangeList(Stream s) throws IOException {
    int count = s.readInteger();
    while (count-- > 0) {
      myChanges.add(s.readChange());
    }
  }

  public void write(Stream s) throws IOException {
    s.writeInteger(myChanges.size());
    for (Change c : myChanges) {
      s.writeChange(c);
    }
  }

  // todo test support
  public List<Change> getChanges() {
    List<Change> result = new ArrayList<Change>(myChanges);
    Collections.reverse(result);
    return result;
  }

  public List<Change> getChangesAfter(Change target) {
    List<Change> result = new ArrayList<Change>();

    for (Change changeSet : Reversed.list(myChanges)) {
      for (Change c : Reversed.list(changeSet.getChanges())) {
        if (c == target) return result;
        result.add(c);
      }
    }

    return result;
  }

  public List<Change> getChangesFor(RootEntry r, String path) {
    RootEntry rootCopy = r.copy();
    Entry e = rootCopy.getEntry(path);

    List<Change> result = new ArrayList<Change>();
    for (Change c : Reversed.list(myChanges)) {
      if (c.affects(e)) result.add(c);
      if (c.isCreationalFor(e)) break;
      c.revertOn(rootCopy);
    }

    return result;
  }

  public void addChange(Change c) {
    myChanges.add(c);
  }

  public void revertUpTo(RootEntry r, Change target, boolean revertTargetChange) {
    for (Change c : Reversed.list(myChanges)) {
      if (!revertTargetChange && c == target) return;
      c.revertOn(r);
      if (c == target) return;
    }
  }

  public List<Content> purgeObsolete(long period) {
    List<Change> newChanges = new ArrayList<Change>();
    List<Content> contentsToPurge = new ArrayList<Content>();

    int index = getIndexOfLastObsoleteChange(period);

    for (int i = index + 1; i < myChanges.size(); i++) {
      newChanges.add(myChanges.get(i));
    }

    for (int i = 0; i <= index; i++) {
      contentsToPurge.addAll(myChanges.get(i).getContentsToPurge());
    }

    myChanges = newChanges;
    return contentsToPurge;
  }

  private int getIndexOfLastObsoleteChange(long period) {
    long prevTimestamp = 0;
    long length = 0;

    for (int i = myChanges.size() - 1; i >= 0; i--) {
      Change c = myChanges.get(i);
      if (prevTimestamp == 0) prevTimestamp = c.getTimestamp();

      long delta = prevTimestamp - c.getTimestamp();
      prevTimestamp = c.getTimestamp();

      length += delta < getIntervalBetweenActivities() ? delta : 1;

      if (length >= period) return i;
    }

    return -1;
  }

  protected long getIntervalBetweenActivities() {
    return 12 * 60 * 60 * 1000;
  }
}
