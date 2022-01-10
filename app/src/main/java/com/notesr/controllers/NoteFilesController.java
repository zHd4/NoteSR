package com.notesr.controllers;

import android.util.Base64;
import com.notesr.models.FileAttachment;

public class NoteFilesController {
    private static final String[] lastIndexTag = new String[] {
            "[__ind]", "[/[__ind]"
    };

    private static final String[] fileTagFormat = new String[] {
            "[__file-%s]", "[/[__file-%s]"
    };

    private static final String[] nameTagFormat = new String[] {
            "[__name-%s]", "[/__name-%s]"
    };

    private static final String[] dataTagFormat = new String[] {
            "[__data-%s]", "[/__data-%s]"
    };

    public static int getLastIndex(final String noteText) {
        if(noteText.contains(lastIndexTag[0]) && noteText.contains(lastIndexTag[1])) {
            return Integer.parseInt(getTextBetwen(noteText, lastIndexTag[0], lastIndexTag[1]));
        }

        return -1;
    }

    public static String setLastIndex(final String noteText, final int lastIndex) {
        String result = noteText;
        
        if(noteText.contains(lastIndexTag[0]) && noteText.contains(lastIndexTag[1])) {
            String newValue = String.valueOf(lastIndex);
            String oldValue = getTextBetwen(noteText, lastIndexTag[0], lastIndexTag[1]);

            newValue = lastIndexTag[0] + newValue + lastIndexTag[1];
            oldValue = lastIndexTag[0] + oldValue + lastIndexTag[1];

            result = result.replace(oldValue, newValue);
        }
        else {
            result += lastIndexTag[0] + "0" + lastIndexTag[1];
        }

        return result;
    }

    public static String generateMarkup(final String filename, final byte[] filedata, int lastIndex) {
        lastIndex = lastIndex + 1;

        StringBuilder result = new StringBuilder();
        String dataBase64 = Base64.encodeToString(filedata, Base64.DEFAULT);

        String[][] tags = generateTags(lastIndex);

        String[] fileTag = tags[0];
        String[] nameTag = tags[1];
        String[] dataTag = tags[2];

        result.append(fileTag[0]);

        result.append(nameTag[0]).append(filename).append(nameTag[1]);
        result.append(dataTag[0]).append(dataBase64).append(dataTag[1]);

        result.append(fileTag[1]);

        return result.toString();
    }

    public static FileAttachment[] findMarkups(String noteText) {
        if(noteText.contains(lastIndexTag[0])) {
            int lastIndex = Integer.parseInt(
                    getTextBetwen(
                            noteText, lastIndexTag[0], lastIndexTag[1]
                    )
            );

            noteText = noteText.replace(lastIndexTag[0] + lastIndex + lastIndexTag[1], "");

            FileAttachment[] files = new FileAttachment[lastIndex + 1];

            for(int i = 0; i <= lastIndex; i++) {
                String[][] tags = generateTags(i);

                String[] fileTag = tags[0];
                String[] nameTag = tags[1];
                String[] dataTag = tags[2];

                String filename = getTextBetwen(noteText, nameTag[0], nameTag[1]);
                String dataBase64 = getTextBetwen(noteText, dataTag[0], dataTag[1]);

                String fileMarkup =
                        fileTag[0] +
                        nameTag[0] + filename + nameTag[1] +
                        dataTag[0] + dataBase64 + dataTag[1] + fileTag[1];

                noteText = noteText.replace(fileMarkup, "");

                byte[] filedata = Base64.decode(dataBase64, Base64.DEFAULT);

                files[i] = new FileAttachment(filename, filedata);
            }

            return files;
        }

        return null;
    }

    private static String[][] generateTags(int index) {
        String[] fileTag = new String[] {
                String.format(nameTagFormat[0], index), String.format(nameTagFormat[1], index)
        };

        String[] nameTag = new String[] {
                String.format(nameTagFormat[0], index), String.format(nameTagFormat[1], index)
        };

        String[] dataTag = new String[] {
                String.format(dataTagFormat[0], index), String.format(dataTagFormat[1], index)
        };

        return new String[][] { fileTag, nameTag, dataTag };
    }

    private static String getTextBetwen(final String raw, final String start, final String end) {
        String result = raw;

        result = raw.substring(raw.indexOf(start) + 1);
        result = raw.substring(0, raw.indexOf(end));

        return result;
    }
}
