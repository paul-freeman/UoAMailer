/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.mailer;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;


class GroupMembers extends ArrayListenable<ArrayListenable.IListener> {
    private List<String> members = new ArrayList<>();

    public void addMember(String name) {
        members.add(name);
        notifyDataAdded(members.size() - 1, 1);
    }

    public void removeMember(int index) {
        notifyDataRemoving(index, 1);
        members.remove(index);
    }

    public void updateMember(int index, String member) {
        members.remove(index);
        members.add(index, member);
        notifyDataChanged(index, 1);
    }

    public String get(int index) {
        return members.get(index);
    }

    public int size() {
        return members.size();
    }
}


public class GroupMemberView extends TableLayout {
    private GroupMembers groupMembers;
    final private List<EditMemberRow> tableRows = new ArrayList<>();

    private ArrayListenable.IListener listener = new ArrayListenable.IListener() {
        @Override
        public void onDataAdded(int index, int count) {
            createRow(index);

            // cleanup empty rows
            for (int i = 0; i < groupMembers.size(); i++) {
                if (i == index)
                    continue;
                if (groupMembers.get(i).trim().equals("")) {
                    groupMembers.removeMember(i);
                    break;
                }
            }
        }

        @Override
        public void onDataRemoving(int index, int count) {
            removeRow(index);
        }

        @Override
        public void onDataChanged(int index, int count) {

        }

        @Override
        public void onAllDataChanged() {

        }
    };

    public GroupMemberView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public void setGroupMembers(GroupMembers groupMembers) {
        if (this.groupMembers != null)
            this.groupMembers.removeListener(listener);

        this.groupMembers = groupMembers;
        if (groupMembers == null)
            return;

        this.groupMembers.addListener(listener);

        for (int i = 0; i < groupMembers.size(); i++)
            createRow(i);
    }

    class EditMemberRow extends TableRow {
        final EditText memberEditText;
        final Button removeButton;

        boolean memberEditTextProgrammatically = false;

        int getIndex() {
            return tableRows.indexOf(this);
        }

        public EditMemberRow(Context context) {
            super(context);

            memberEditText = new EditText(context);
            memberEditText.setMinimumWidth(150);
            memberEditText.setSingleLine(true);
            removeButton = new Button(context);
            removeButton.setText("remove");

            addView(memberEditText);
            addView(removeButton);

            memberEditText.requestFocus();

            removeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    groupMembers.removeMember(getIndex());
                }
            });

            memberEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (!memberEditTextProgrammatically)
                        groupMembers.updateMember(getIndex(), editable.toString());
                }
            });
        }

        public void setMemberEditText(String text) {
            memberEditTextProgrammatically = true;
            memberEditText.setText(text);
            memberEditTextProgrammatically = false;
        }
    }

    private TableRow createRow(int index) {
        EditMemberRow row = new EditMemberRow(getContext());
        tableRows.add(index, row);
        addView(row, index);
        return row;
    }

    private void removeRow(int index) {
        tableRows.remove(index);
        removeViewAt(index);
    }

}


