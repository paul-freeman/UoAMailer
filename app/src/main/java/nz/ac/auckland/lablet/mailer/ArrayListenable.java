/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.mailer;


public class ArrayListenable<Listener extends ArrayListenable.IListener>
        extends WeakListenable<ArrayListenable.IListener> {

    public interface IListener {
        public void onDataAdded(int index, int count);
        public void onDataRemoving(int index, int count);
        public void onDataChanged(int index, int count);
        public void onAllDataChanged();
    }

    public void notifyDataAdded(int index, int count) {
        for (IListener listener : getListeners())
            listener.onDataAdded(index, count);
    }

    public void notifyDataRemoving(int index, int count) {
        for (IListener listener : getListeners())
            listener.onDataRemoving(index, count);
    }

    public void notifyDataChanged(int index, int count) {
        for (IListener listener : getListeners())
            listener.onDataChanged(index, count);
    }

    public void notifyAllDataChanged() {
        for (IListener listener : getListeners())
            listener.onAllDataChanged();
    }
}
